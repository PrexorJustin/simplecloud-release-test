package app.simplecloud.controller.runtime.server

import app.simplecloud.controller.runtime.MetricsEventNames
import app.simplecloud.controller.runtime.group.GroupRepository
import app.simplecloud.controller.runtime.host.ServerHostRepository
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.group.GroupTimeout
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.api.auth.AuthCallCredentials
import app.simplecloud.droplet.api.time.ProtobufTimestamp
import app.simplecloud.pubsub.PubSubClient
import build.buf.gen.simplecloud.controller.v1.*
import build.buf.gen.simplecloud.metrics.v1.metric
import build.buf.gen.simplecloud.metrics.v1.metricMeta
import io.grpc.Status
import io.grpc.StatusException
import org.apache.logging.log4j.LogManager
import java.time.LocalDateTime
import java.util.*

class ServerService(
    private val numericalIdRepository: ServerNumericalIdRepository,
    private val serverRepository: ServerRepository,
    private val hostRepository: ServerHostRepository,
    private val groupRepository: GroupRepository,
    private val authCallCredentials: AuthCallCredentials,
    private val pubSubClient: PubSubClient,
    private val serverHostAttacher: ServerHostAttacher,
) : ControllerServerServiceGrpcKt.ControllerServerServiceCoroutineImplBase() {

    private val logger = LogManager.getLogger(ServerService::class.java)

    @Deprecated("This method will be removed soon. Please use DropletService#registerDroplet")
    override suspend fun attachServerHost(request: AttachServerHostRequest): ServerHostDefinition {
        val serverHost = ServerHost.fromDefinition(request.serverHost, authCallCredentials)
        try {
            serverHostAttacher.attach(serverHost)
        } catch (e: Exception) {
            throw StatusException(Status.INTERNAL.withDescription("Could not attach serverhost").withCause(e))
        }
        return serverHost.toDefinition()
    }

    override suspend fun getAllServers(request: GetAllServersRequest): GetAllServersResponse {
        val currentServers = serverRepository.getAll()
        return getAllServersResponse { servers.addAll(currentServers.map { it.toDefinition() }) }
    }

    override suspend fun getServerByNumerical(request: GetServerByNumericalRequest): ServerDefinition {
        val server = serverRepository.findServerByNumerical(request.groupName, request.numericalId.toInt())
            ?: throw StatusException(Status.NOT_FOUND.withDescription("No server was found matching this group and numerical id"))
        return server.toDefinition()
    }

    override suspend fun stopServerByNumerical(request: StopServerByNumericalRequest): ServerDefinition {
        val server = serverRepository.findServerByNumerical(request.groupName, request.numericalId.toInt())
            ?: throw StatusException(Status.NOT_FOUND.withDescription("No server was found matching this group and numerical id"))
        try {
            return stopServer(server.toDefinition(), request.stopCause)
        } catch (e: Exception) {
            throw StatusException(
                Status.INTERNAL.withDescription("Error occured whilest cleaning up stopped server: ").withCause(e)
            )
        }
    }

    override suspend fun updateServer(request: UpdateServerRequest): ServerDefinition {
        val deleted = request.deleted
        val server = Server.fromDefinition(request.server)
        if (!deleted) {
            try {
                val before = serverRepository.find(server.uniqueId)
                    ?: throw StatusException(Status.NOT_FOUND.withDescription("Server not found"))
                val wasUpdated = before != server

                if (wasUpdated) {
                    pubSubClient.publish(
                        "event",
                        ServerUpdateEvent.newBuilder()
                            .setUpdatedAt(ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                            .setServerBefore(before.toDefinition()).setServerAfter(request.server).build()
                    )

                    pubSubClient.publish(MetricsEventNames.RECORD_METRIC, metric {
                        metricType = "ACTIVITY_LOG"
                        metricValue = 1L
                        time = ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now())
                        meta.addAll(
                            listOf(
                                metricMeta {
                                    dataName = "displayName"
                                    dataValue = "${server.group} #${server.numericalId}"
                                },
                                metricMeta {
                                    dataName = "status"
                                    dataValue = "EDITED"
                                },
                                metricMeta {
                                    dataName = "resourceType"
                                    dataValue = "SERVER"
                                },
                                metricMeta {
                                    dataName = "groupName"
                                    dataValue = server.group
                                },
                                metricMeta {
                                    dataName = "numericalId"
                                    dataValue = server.numericalId.toString()
                                },
                                metricMeta {
                                    dataName = "by"
                                    dataValue = "API"
                                }
                            )
                        )
                    })
                }

                serverRepository.save(server)
                return server.toDefinition()
            } catch (e: Exception) {
                throw StatusException(
                    Status.INTERNAL
                        .withDescription("Could not update server")
                        .withCause(e)
                )
            }
        } else {
            logger.info("Deleting server ${server.uniqueId} of group ${request.server.groupName}...")
            val deleteSuccess = serverRepository.delete(server)
            if (!deleteSuccess) {
                throw StatusException(
                    Status.INTERNAL
                        .withDescription("Could not delete server")
                )
            }
            logger.info("Deleted server ${server.uniqueId} of group ${request.server.groupName}.")
            pubSubClient.publish(
                "event", ServerStopEvent.newBuilder()
                    .setServer(request.server)
                    .setStoppedAt(ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                    .setStopCause(ServerStopCause.NATURAL_STOP)
                    .setTerminationMode(ServerTerminationMode.UNKNOWN_MODE) //TODO: Add proto fields to make changing this possible
                    .build()
            )

            pubSubClient.publish(MetricsEventNames.RECORD_METRIC, metric {
                metricType = "ACTIVITY_LOG"
                metricValue = 1L
                time = ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now())
                meta.addAll(
                    listOf(
                        metricMeta {
                            dataName = "displayName"
                            dataValue = "${server.group} #${server.numericalId}"
                        },
                        metricMeta {
                            dataName = "status"
                            dataValue = "STOPPED"
                        },
                        metricMeta {
                            dataName = "resourceType"
                            dataValue = "SERVER"
                        },
                        metricMeta {
                            dataName = "groupName"
                            dataValue = server.group
                        },
                        metricMeta {
                            dataName = "numericalId"
                            dataValue = server.numericalId.toString()
                        },
                        metricMeta {
                            dataName = "by"
                            dataValue = ServerStopCause.NATURAL_STOP.toString()
                        }
                    )
                )
            })

            return server.toDefinition()
        }
    }

    override suspend fun getServerById(request: GetServerByIdRequest): ServerDefinition {
        val server = serverRepository.find(request.serverId) ?: throw StatusException(
            Status.NOT_FOUND
                .withDescription("No server was found matching this unique id")
        )
        return server.toDefinition()
    }

    override suspend fun getServersByGroup(request: GetServersByGroupRequest): GetServersByGroupResponse {
        val groupServers = serverRepository.findServersByGroup(request.groupName)
        return getServersByGroupResponse { servers.addAll(groupServers.map { it.toDefinition() }) }
    }

    override suspend fun getServersByType(request: ServerTypeRequest): GetServersByTypeResponse {
        val typeServers = serverRepository.findServersByType(request.serverType)
        return getServersByTypeResponse { servers.addAll(typeServers.map { it.toDefinition() }) }
    }

    override suspend fun startMultipleServers(request: ControllerStartMultipleServersRequest): StartMultipleServerResponse {
        val host = hostRepository.find(serverRepository)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("No server host found, could not start servers"))
        val group = groupRepository.find(request.groupName)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("No group was found matching this name"))

        val startedServers = mutableListOf<ServerDefinition>()

        try {
            for (i in 1..request.amount) {
                val server = startServer(host, group)
                publishServerStartEvents(server, request.startCause)
                startedServers.add(server)
            }
        } catch (e: Exception) {
            throw StatusException(Status.INTERNAL.withDescription("Error whilst starting multiple servers").withCause(e))
        }

        return StartMultipleServerResponse.newBuilder()
            .addAllServers(startedServers)
            .build()
    }

    override suspend fun startServer(request: ControllerStartServerRequest): ServerDefinition {
        val host = hostRepository.find(serverRepository)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("No server host found, could not start server"))
        val group = groupRepository.find(request.groupName)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("No group was found matching this name"))
        try {
            val server = startServer(host, group)

            publishServerStartEvents(server, request.startCause)

            return server
        } catch (e: Exception) {
            throw StatusException(Status.INTERNAL.withDescription("Error whilst starting server").withCause(e))
        }
    }

    private suspend fun startServer(host: ServerHost, group: Group): ServerDefinition {
        val numericalId = numericalIdRepository.findNextNumericalId(group.name)
        val server = buildServer(group, numericalId)
        serverRepository.save(server)
        val stub = host.stub ?: throw StatusException(Status.INTERNAL.withDescription("Server host has no stub"))
        serverRepository.save(server)
        try {
            val result = stub.startServer(
                ServerHostStartServerRequest.newBuilder()
                    .setGroup(group.toDefinition())
                    .setServer(server.toDefinition())
                    .build()
            )
            serverRepository.save(Server.fromDefinition(result))
            return result
        } catch (e: Exception) {
            serverRepository.delete(server)
            numericalIdRepository.removeNumericalId(group.name, server.numericalId)
            logger.error("Error whilst starting server:", e)
            throw e
        }
    }

    private suspend fun publishServerStartEvents(server: ServerDefinition, startCause: ServerStartCause) {
        pubSubClient.publish(
            "event", ServerStartEvent.newBuilder()
                .setServer(server)
                .setStartedAt(ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                .setStartCause(startCause)
                .build()
        )

        pubSubClient.publish(MetricsEventNames.RECORD_METRIC, metric {
            metricType = "ACTIVITY_LOG"
            metricValue = 1L
            time = ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now())
            meta.addAll(
                listOf(
                    metricMeta {
                        dataName = "displayName"
                        dataValue = "${server.groupName} #${server.numericalId}"
                    },
                    metricMeta {
                        dataName = "status"
                        dataValue = "STARTED"
                    },
                    metricMeta {
                        dataName = "resourceType"
                        dataValue = "SERVER"
                    },
                    metricMeta {
                        dataName = "groupName"
                        dataValue = server.groupName
                    },
                    metricMeta {
                        dataName = "numericalId"
                        dataValue = server.numericalId.toString()
                    },
                    metricMeta {
                        dataName = "by"
                        dataValue = startCause.toString()
                    }
                )
            )
        })
    }

    private fun buildServer(group: Group, numericalId: Int): Server {
        return Server.fromDefinition(
            ServerDefinition.newBuilder()
                .setNumericalId(numericalId)
                .setServerType(group.type)
                .setGroupName(group.name)
                .setMinimumMemory(group.minMemory)
                .setMaximumMemory(group.maxMemory)
                .setServerState(ServerState.PREPARING)
                .setMaxPlayers(group.maxPlayers)
                .setCreatedAt(ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                .setUpdatedAt(ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                .setPlayerCount(0)
                .setUniqueId(UUID.randomUUID().toString().replace("-", "")).putAllCloudProperties(
                    mapOf(
                        *group.properties.entries.map { it.key to it.value }.toTypedArray()
                    )
                ).build()
        )
    }

    override suspend fun stopServer(request: StopServerRequest): ServerDefinition {
        val server = serverRepository.find(request.serverId)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("No server was found matching this id."))

        request.since?.let { sinceTimestamp ->
            val sinceLocalDateTime = ProtobufTimestamp.toLocalDateTime(sinceTimestamp)
            if (server.createdAt.isBefore(sinceLocalDateTime)) {
                return server.toDefinition()
            }
        }

        try {
            val stopped = stopServer(server.toDefinition(), request.stopCause)
            return stopped
        } catch (e: Exception) {
            throw StatusException(Status.INTERNAL.withDescription("Error whilst stopping server").withCause(e))
        }
    }

    override suspend fun stopServersByGroupWithTimeout(request: StopServersByGroupWithTimeoutRequest): StopServersByGroupResponse {
        val sinceLocalDateTime = request.since?.let {
            ProtobufTimestamp.toLocalDateTime(it)
        }
        return stopServersByGroup(request.groupName, request.timeoutSeconds, request.stopCause, sinceLocalDateTime)
    }

    override suspend fun stopServersByGroup(request: StopServersByGroupRequest): StopServersByGroupResponse {
        val sinceLocalDateTime = request.since?.let {
            ProtobufTimestamp.toLocalDateTime(it)
        }
        return stopServersByGroup(request.groupName, null, request.stopCause, sinceLocalDateTime)
    }

    private suspend fun stopServersByGroup(
        groupName: String,
        timeout: Int?,
        cause: ServerStopCause = ServerStopCause.NATURAL_STOP,
        since: LocalDateTime? = null
    ): StopServersByGroupResponse {
        val group = groupRepository.find(groupName)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("No group was found matching this name. $groupName"))
        val groupServers = serverRepository.findServersByGroup(group.name)
            .filter { since == null || it.createdAt.isAfter(since) }

        if (groupServers.isEmpty()) {
            throw StatusException(Status.NOT_FOUND.withDescription("No server was found matching this group name. ${group.name}"))
        }

        val serverDefinitionList = mutableListOf<ServerDefinition>()

        try {
            timeout?.let {
                group.timeout = GroupTimeout(it);
            }

            groupServers.forEach { server ->
                serverDefinitionList.add(stopServer(server.toDefinition(), cause))
            }

            return stopServersByGroupResponse { servers.addAll(serverDefinitionList) }
        } catch (e: Exception) {
            throw StatusException(Status.INTERNAL.withDescription("Error whilst stopping server by group").withCause(e))
        }
    }

    private suspend fun stopServer(
        server: ServerDefinition,
        cause: ServerStopCause = ServerStopCause.NATURAL_STOP
    ): ServerDefinition {
        val host = hostRepository.findServerHostById(server.hostId)
            ?: throw Status.NOT_FOUND
                .withDescription("No server host was found matching this server.")
                .asRuntimeException()
        val stub = host.stub ?: throw StatusException(Status.INTERNAL.withDescription("Server host has no stub"))
        try {
            val stopped = stub.stopServer(server)
            pubSubClient.publish(
                "event", ServerStopEvent.newBuilder()
                    .setServer(stopped)
                    .setStoppedAt(ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                    .setStopCause(cause)
                    .setTerminationMode(ServerTerminationMode.UNKNOWN_MODE) //TODO: Add proto fields to make changing this possible
                    .build()
            )

            pubSubClient.publish(MetricsEventNames.RECORD_METRIC, metric {
                metricType = "ACTIVITY_LOG"
                metricValue = 1L
                time = ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now())
                meta.addAll(
                    listOf(
                        metricMeta {
                            dataName = "displayName"
                            dataValue = "${server.groupName} #${server.numericalId}"
                        },
                        metricMeta {
                            dataName = "status"
                            dataValue = "STOPPED"
                        },
                        metricMeta {
                            dataName = "resourceType"
                            dataValue = "SERVER"
                        },
                        metricMeta {
                            dataName = "groupName"
                            dataValue = server.groupName
                        },
                        metricMeta {
                            dataName = "numericalId"
                            dataValue = server.numericalId.toString()
                        },
                        metricMeta {
                            dataName = "by"
                            dataValue = cause.toString()
                        }
                    )
                )
            })

            serverRepository.delete(Server.fromDefinition(stopped))
            return stopped
        } catch (e: Exception) {
            logger.error("Server stop error occured:", e)
            throw e
        }
    }

    override suspend fun updateServerProperty(request: UpdateServerPropertyRequest): ServerDefinition {
        val server = serverRepository.find(request.serverId)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("Server with id ${request.serverId} does not exist."))
        val serverBefore = Server.fromDefinition(server.toDefinition())
        server.properties[request.propertyKey] = request.propertyValue
        serverRepository.save(server)

        if (serverBefore.properties[request.propertyKey] != server.properties[request.propertyKey]) {
            pubSubClient.publish(
                "event",
                ServerUpdateEvent.newBuilder()
                    .setUpdatedAt(ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                    .setServerBefore(serverBefore.toDefinition())
                    .setServerAfter(server.toDefinition())
                    .build()
            )

            pubSubClient.publish(MetricsEventNames.RECORD_METRIC, metric {
                metricType = "ACTIVITY_LOG"
                metricValue = 1L
                time = ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now())
                meta.addAll(
                    listOf(
                        metricMeta {
                            dataName = "displayName"
                            dataValue = "${server.group} #${server.numericalId}"
                        },
                        metricMeta {
                            dataName = "status"
                            dataValue = "EDITED"
                        },
                        metricMeta {
                            dataName = "resourceType"
                            dataValue = "SERVER"
                        },
                        metricMeta {
                            dataName = "groupName"
                            dataValue = server.group
                        },
                        metricMeta {
                            dataName = "numericalId"
                            dataValue = server.numericalId.toString()
                        },
                        metricMeta {
                            dataName = "by"
                            dataValue = "API"
                        }
                    )
                )
            })
        }

        return server.toDefinition()
    }

    override suspend fun updateServerState(request: UpdateServerStateRequest): ServerDefinition {
        val server = serverRepository.find(request.serverId)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("Server with id ${request.serverId} does not exist."))
        val serverBefore = server.copy()
        server.state = request.serverState
        serverRepository.save(server)
        pubSubClient.publish(
            "event",
            ServerUpdateEvent.newBuilder().setUpdatedAt(ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                .setServerBefore(serverBefore.toDefinition()).setServerAfter(server.toDefinition()).build()
        )

        pubSubClient.publish(MetricsEventNames.RECORD_METRIC, metric {
            metricType = "ACTIVITY_LOG"
            metricValue = 1L
            time = ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now())
            meta.addAll(
                listOf(
                    metricMeta {
                        dataName = "displayName"
                        dataValue = "${server.group} #${server.numericalId}"
                    },
                    metricMeta {
                        dataName = "status"
                        dataValue = "EDITED"
                    },
                    metricMeta {
                        dataName = "resourceType"
                        dataValue = "SERVER"
                    },
                    metricMeta {
                        dataName = "groupName"
                        dataValue = server.group
                    },
                    metricMeta {
                        dataName = "numericalId"
                        dataValue = server.numericalId.toString()
                    },
                    metricMeta {
                        dataName = "by"
                        dataValue = "API"
                    }
                )
            )
        })

        return server.toDefinition()
    }

}