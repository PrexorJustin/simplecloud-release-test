package app.simplecloud.controller.api.impl.coroutines

import app.simplecloud.controller.api.ServerApi
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.api.auth.AuthCallCredentials
import build.buf.gen.simplecloud.controller.v1.*
import io.grpc.ManagedChannel

class ServerApiCoroutineImpl(
    managedChannel: ManagedChannel,
    authCallCredentials: AuthCallCredentials
) : ServerApi.Coroutine {

    private val serverServiceStub: ControllerServerServiceGrpcKt.ControllerServerServiceCoroutineStub =
        ControllerServerServiceGrpcKt.ControllerServerServiceCoroutineStub(managedChannel)
            .withCallCredentials(authCallCredentials)

    override suspend fun getAllServers(): List<Server> {
        return serverServiceStub.getAllServers(getAllServersRequest {}).serversList.map {
            Server.fromDefinition(it)
        }
    }

    override suspend fun getServerById(id: String): Server {
        return Server.fromDefinition(
            serverServiceStub.getServerById(
                getServerByIdRequest {
                    this.serverId = id
                }
            )
        )
    }

    override suspend fun getServersByGroup(groupName: String): List<Server> {
        return serverServiceStub.getServersByGroup(
            getServersByGroupRequest {
                this.groupName = groupName
            }
        ).serversList.map {
            Server.fromDefinition(it)
        }
    }

    override suspend fun getServersByGroup(group: Group): List<Server> {
        return getServersByGroup(group.name)
    }

    override suspend fun getServerByNumerical(groupName: String, numericalId: Long): Server {
        return Server.fromDefinition(
            serverServiceStub.getServerByNumerical(
                getServerByNumericalRequest {
                    this.groupName = groupName
                    this.numericalId = numericalId
                }
            )
        )
    }

    override suspend fun getServersByType(type: ServerType): List<Server> {
        return serverServiceStub.getServersByType(
            ServerTypeRequest.newBuilder()
                .setServerType(type)
                .build()
        ).serversList.map {
            Server.fromDefinition(it)
        }
    }

    override suspend fun startServer(groupName: String, startCause: ServerStartCause): Server {
        return Server.fromDefinition(
            serverServiceStub.startServer(
                controllerStartServerRequest {
                    this.groupName = groupName
                    this.startCause = startCause
                }
            )
        )
    }

    override suspend fun stopServer(groupName: String, numericalId: Long, stopCause: ServerStopCause): Server {
        return Server.fromDefinition(
            serverServiceStub.stopServerByNumerical(
                stopServerByNumericalRequest {
                    this.groupName = groupName
                    this.numericalId = numericalId
                    this.stopCause = stopCause
                }
            )
        )
    }

    override suspend fun stopServer(id: String, stopCause: ServerStopCause): Server {
        return Server.fromDefinition(
            serverServiceStub.stopServer(
                stopServerRequest {
                    this.serverId = id
                    this.stopCause = stopCause
                }
            )
        )
    }

    override suspend fun stopServers(groupName: String, stopCause: ServerStopCause): List<Server> {
        return serverServiceStub.stopServersByGroup(stopServersByGroupRequest {
            this.groupName = groupName
            this.stopCause = stopCause
        }).serversList.map {
            Server.fromDefinition(it)
        }
    }

    override suspend fun stopServers(groupName: String, timeoutSeconds: Int, stopCause: ServerStopCause): List<Server> {
        return serverServiceStub.stopServersByGroupWithTimeout(stopServersByGroupWithTimeoutRequest {
            this.groupName = groupName
            this.stopCause = stopCause
            this.timeoutSeconds = timeoutSeconds
        }).serversList.map {
            Server.fromDefinition(it)
        }
    }

    override suspend fun updateServerState(id: String, state: ServerState): Server {
        return Server.fromDefinition(
            serverServiceStub.updateServerState(
                updateServerStateRequest {
                    this.serverState = state
                    this.serverId = id
                }
            )
        )
    }

    override suspend fun updateServerProperty(id: String, key: String, value: Any): Server {
        return Server.fromDefinition(
            serverServiceStub.updateServerProperty(
                updateServerPropertyRequest {
                    this.propertyKey = key
                    this.propertyValue = value.toString()
                    this.serverId = id
                }
            )
        )
    }

}