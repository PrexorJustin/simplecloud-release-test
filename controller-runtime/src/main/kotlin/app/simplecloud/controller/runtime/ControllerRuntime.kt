package app.simplecloud.controller.runtime

import app.simplecloud.controller.runtime.database.DatabaseFactory
import app.simplecloud.controller.runtime.droplet.ControllerDropletService
import app.simplecloud.controller.runtime.droplet.DropletRepository
import app.simplecloud.controller.runtime.envoy.ControlPlaneServer
import app.simplecloud.controller.runtime.group.GroupRepository
import app.simplecloud.controller.runtime.group.GroupService
import app.simplecloud.controller.runtime.host.ServerHostRepository
import app.simplecloud.controller.runtime.launcher.ControllerStartCommand
import app.simplecloud.controller.runtime.oauth.OAuthServer
import app.simplecloud.controller.runtime.reconciler.Reconciler
import app.simplecloud.controller.runtime.server.ServerHostAttacher
import app.simplecloud.controller.runtime.server.ServerNumericalIdRepository
import app.simplecloud.controller.runtime.server.ServerRepository
import app.simplecloud.controller.runtime.server.ServerService
import app.simplecloud.droplet.api.auth.AuthCallCredentials
import app.simplecloud.droplet.api.auth.AuthSecretInterceptor
import app.simplecloud.pubsub.PubSubClient
import app.simplecloud.pubsub.PubSubService
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager

class ControllerRuntime(
    private val controllerStartCommand: ControllerStartCommand
) {

    private val logger = LogManager.getLogger(ControllerRuntime::class.java)
    private val database = DatabaseFactory.createDatabase(controllerStartCommand.databaseUrl)
    private val authCallCredentials = AuthCallCredentials(controllerStartCommand.authSecret)

    private val pubSubClient = PubSubClient(
        controllerStartCommand.grpcHost,
        controllerStartCommand.pubSubGrpcPort,
        authCallCredentials
    )
    private val groupRepository = GroupRepository(controllerStartCommand.groupPath, pubSubClient)
    private val numericalIdRepository = ServerNumericalIdRepository()
    private val serverRepository = ServerRepository(database, numericalIdRepository)
    private val hostRepository = ServerHostRepository()
    private val serverHostAttacher = ServerHostAttacher(hostRepository, serverRepository)
    private val dropletRepository = DropletRepository(authCallCredentials, serverHostAttacher, controllerStartCommand.envoyStartPort, hostRepository)
    private val pubSubService = PubSubService()
    private val controlPlaneServer = ControlPlaneServer(controllerStartCommand, dropletRepository)
    private val authServer = OAuthServer(controllerStartCommand, database)
    private val reconciler = Reconciler(
        groupRepository,
        serverRepository,
        hostRepository,
        numericalIdRepository,
        createManagedChannel(),
        authCallCredentials
    )
    private val server = createGrpcServer()
    private val pubSubServer = createPubSubGrpcServer()

    suspend fun start() {
        logger.info("Starting controller")
        setupDatabase()
        startAuthServer()
        startControlPlaneServer()
        startPubSubGrpcServer()
        startGrpcServer()
        startReconciler()
        loadGroups()
        loadServers()

        suspendCancellableCoroutine<Unit> { continuation ->
            Runtime.getRuntime().addShutdownHook(Thread {
                server.shutdown()
                continuation.resume(Unit) { cause, _, _ ->
                    logger.info("Server shutdown due to: $cause")
                }
            })
        }
    }

    private fun startAuthServer() {
        logger.info("Starting auth server...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                authServer.start()
                logger.info("Auth server stopped.")
            } catch (e: Exception) {
                logger.error("Error in gRPC server", e)
                throw e
            }
        }

    }

    private fun startControlPlaneServer() {
        logger.info("Starting envoy control plane...")
        controlPlaneServer.start()
    }

    private fun setupDatabase() {
        logger.info("Setting up database...")
        database.setup()
    }

    private fun loadServers() {
        logger.info("Loading servers...")
        val loadedServers = serverRepository.load()
        if (loadedServers.isEmpty()) {
            return
        }

        logger.info("Loaded servers from cache: ${loadedServers.joinToString { "${it.group}-${it.numericalId}" }}")
    }

    private fun startGrpcServer() {
        logger.info("Starting gRPC server...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                server.start()
                server.awaitTermination()
            } catch (e: Exception) {
                logger.error("Error in gRPC server", e)
                throw e
            }
        }
    }

    private fun startPubSubGrpcServer() {
        logger.info("Starting pubsub gRPC server...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                pubSubServer.start()
                pubSubServer.awaitTermination()
            } catch (e: Exception) {
                logger.error("Error in gRPC server", e)
                throw e
            }
        }
    }

    private fun startReconciler() {
        logger.info("Starting Reconciler...")
        startReconcilerJob()
    }

    private fun loadGroups() {
        logger.info("Loading groups...")
        val loadedGroups = groupRepository.load()
        if (loadedGroups.isEmpty()) {
            logger.warn("No groups found.")
            return
        }

        logger.info("Loaded groups: ${loadedGroups.joinToString { it.name }}")
    }

    private fun createGrpcServer(): Server {
        return ServerBuilder.forPort(controllerStartCommand.grpcPort)
            .addService(GroupService(groupRepository))
            .addService(
                ServerService(
                    numericalIdRepository,
                    serverRepository,
                    hostRepository,
                    groupRepository,
                    authCallCredentials,
                    pubSubClient,
                    serverHostAttacher
                )
            )
            .addService(ControllerDropletService(dropletRepository))
            .intercept(AuthSecretInterceptor(controllerStartCommand.grpcHost, controllerStartCommand.authorizationPort))
            .build()
    }

    private fun createPubSubGrpcServer(): Server {
        return ServerBuilder.forPort(controllerStartCommand.pubSubGrpcPort)
            .addService(pubSubService)
            .intercept(AuthSecretInterceptor(controllerStartCommand.grpcHost, controllerStartCommand.authorizationPort))
            .build()
    }

    private fun createManagedChannel(): ManagedChannel {
        return ManagedChannelBuilder.forAddress(controllerStartCommand.grpcHost, controllerStartCommand.grpcPort)
            .usePlaintext()
            .build()
    }

    private fun startReconcilerJob(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                reconciler.reconcile()
                delay(2000L)
            }
        }
    }

}