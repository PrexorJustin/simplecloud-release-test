package app.simplecloud.controller.runtime.envoy

import app.simplecloud.controller.runtime.droplet.DropletRepository
import app.simplecloud.controller.runtime.launcher.ControllerStartCommand
import app.simplecloud.droplet.api.droplet.Droplet
import io.envoyproxy.controlplane.server.V3DiscoveryServer
import io.grpc.ServerBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.logging.log4j.LogManager

/**
 * @see <a href="https://www.envoyproxy.io/docs/envoy/latest/api-docs/xds_protocol#xds-protocol-ads">ADS Documentation</a>
 */
class ControlPlaneServer(private val args: ControllerStartCommand, private val dropletRepository: DropletRepository) {
    private val server = V3DiscoveryServer(dropletRepository.getAsDropletCache().getCache())
    private val logger = LogManager.getLogger(ControlPlaneServer::class.java)

    fun start() {
        val serverBuilder = ServerBuilder.forPort(args.envoyDiscoveryPort)
        register(serverBuilder)
        val server = serverBuilder.build()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                server.start()
                server.awaitTermination()
            } catch (e: Exception) {
                logger.warn("Error in envoy control server server", e)
                throw e
            }
        }
        registerSelf()
    }

    private fun registerSelf() {
        dropletRepository.save(
            Droplet(
                type = "controller",
                id = "internal-controller",
                host = args.grpcHost,
                port = args.grpcPort,
                envoyPort = 8080,
            )
        )
        dropletRepository.save(
            Droplet(
                type = "auth",
                id = "internal-auth",
                host = args.grpcHost,
                port = args.authorizationPort,
                envoyPort = 8080
            )
        )
    }

    private fun register(builder: ServerBuilder<*>) {
        logger.info("Registering envoy ADS...")
        builder.addService(server.aggregatedDiscoveryServiceImpl)
    }
}