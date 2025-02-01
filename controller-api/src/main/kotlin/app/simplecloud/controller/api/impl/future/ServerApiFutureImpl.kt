package app.simplecloud.controller.api.impl.future

import app.simplecloud.controller.api.ServerApi
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.api.auth.AuthCallCredentials
import app.simplecloud.droplet.api.future.toCompletable
import build.buf.gen.simplecloud.controller.v1.*
import io.grpc.ManagedChannel
import java.util.concurrent.CompletableFuture

class ServerApiFutureImpl(
    managedChannel: ManagedChannel,
    authCallCredentials: AuthCallCredentials
) : ServerApi.Future {

    private val serverServiceStub: ControllerServerServiceGrpc.ControllerServerServiceFutureStub =
        ControllerServerServiceGrpc.newFutureStub(managedChannel).withCallCredentials(authCallCredentials)

    override fun getAllServers(): CompletableFuture<List<Server>> {
        return serverServiceStub.getAllServers(GetAllServersRequest.newBuilder().build()).toCompletable().thenApply {
            Server.fromDefinition(it.serversList)
        }
    }

    override fun getServerById(id: String): CompletableFuture<Server> {
        return serverServiceStub.getServerById(
            GetServerByIdRequest.newBuilder()
                .setServerId(id)
                .build()
        ).toCompletable().thenApply {
            Server.fromDefinition(it)
        }
    }

    override fun getServerByNumerical(groupName: String, numericalId: Long): CompletableFuture<Server> {
        return serverServiceStub.getServerByNumerical(
            GetServerByNumericalRequest.newBuilder()
                .setGroupName(groupName)
                .setNumericalId(numericalId)
                .build()
        ).toCompletable().thenApply {
            Server.fromDefinition(it)
        }
    }

    override fun getServersByGroup(groupName: String): CompletableFuture<List<Server>> {
        return serverServiceStub.getServersByGroup(
            GetServersByGroupRequest.newBuilder()
                .setGroupName(groupName)
                .build()
        ).toCompletable().thenApply {
            Server.fromDefinition(it.serversList)
        }
    }

    override fun getServersByGroup(group: Group): CompletableFuture<List<Server>> {
        return getServersByGroup(group.name)
    }

    override fun getServersByType(type: ServerType): CompletableFuture<List<Server>> {
        return serverServiceStub.getServersByType(
            ServerTypeRequest.newBuilder()
                .setServerType(type)
                .build()
        ).toCompletable().thenApply {
            Server.fromDefinition(it.serversList)
        }
    }

    override fun startServer(groupName: String, startCause: ServerStartCause): CompletableFuture<Server?> {
        return serverServiceStub.startServer(
            ControllerStartServerRequest.newBuilder()
                .setGroupName(groupName)
                .setStartCause(startCause)
                .build()
        ).toCompletable().thenApply {
            Server.fromDefinition(it)
        }
    }

    override fun stopServer(
        groupName: String,
        numericalId: Long,
        stopCause: ServerStopCause
    ): CompletableFuture<Server> {
        return serverServiceStub.stopServerByNumerical(
            StopServerByNumericalRequest.newBuilder()
                .setGroupName(groupName)
                .setNumericalId(numericalId)
                .setStopCause(stopCause)
                .build()
        ).toCompletable().thenApply {
            Server.fromDefinition(it)
        }
    }

    override fun stopServer(id: String, stopCause: ServerStopCause): CompletableFuture<Server> {
        return serverServiceStub.stopServer(
            StopServerRequest.newBuilder()
                .setServerId(id)
                .setStopCause(stopCause)
                .build()
        ).toCompletable().thenApply {
            Server.fromDefinition(it)
        }
    }

    override fun stopServers(groupName: String, stopCause: ServerStopCause): CompletableFuture<List<Server>> {
        return serverServiceStub.stopServersByGroup(
            StopServersByGroupRequest.newBuilder()
                .setGroupName(groupName)
                .setStopCause(stopCause)
                .build()
        ).toCompletable().thenApply {
            Server.fromDefinition(it.serversList)
        }
    }

    override fun stopServers(
        groupName: String,
        timeoutSeconds: Int,
        stopCause: ServerStopCause
    ): CompletableFuture<List<Server>> {
        return serverServiceStub.stopServersByGroupWithTimeout(
            StopServersByGroupWithTimeoutRequest.newBuilder()
                .setGroupName(groupName)
                .setStopCause(stopCause)
                .setTimeoutSeconds(timeoutSeconds)
                .build()
        ).toCompletable().thenApply {
            Server.fromDefinition(it.serversList)
        }
    }

    override fun updateServerState(id: String, state: ServerState): CompletableFuture<Server> {
        return serverServiceStub.updateServerState(
            UpdateServerStateRequest.newBuilder()
                .setServerState(state)
                .setServerId(id)
                .build()
        ).toCompletable().thenApply {
            return@thenApply Server.fromDefinition(it)
        }
    }

    override fun updateServerProperty(id: String, key: String, value: Any): CompletableFuture<Server> {
        return serverServiceStub.updateServerProperty(
            UpdateServerPropertyRequest.newBuilder()
                .setPropertyKey(key)
                .setPropertyValue(value.toString())
                .setServerId(id)
                .build()
        ).toCompletable().thenApply {
            return@thenApply Server.fromDefinition(it)
        }
    }
}