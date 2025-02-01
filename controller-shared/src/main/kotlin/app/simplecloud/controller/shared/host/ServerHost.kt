package app.simplecloud.controller.shared.host

import app.simplecloud.droplet.api.auth.AuthCallCredentials
import build.buf.gen.simplecloud.controller.v1.ServerHostDefinition
import build.buf.gen.simplecloud.controller.v1.ServerHostServiceGrpcKt
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

data class ServerHost(
    val id: String,
    val host: String,
    val port: Int,
    val stub: ServerHostServiceGrpcKt.ServerHostServiceCoroutineStub? = null,
) {

    fun toDefinition(): ServerHostDefinition {
        return ServerHostDefinition.newBuilder()
            .setHostHost(host)
            .setHostPort(port)
            .setHostId(id)
            .build()
    }

    companion object {
        @JvmStatic
        fun fromDefinition(serverHostDefinition: ServerHostDefinition, credentials: AuthCallCredentials): ServerHost {
            return ServerHost(
                serverHostDefinition.hostId,
                serverHostDefinition.hostHost,
                serverHostDefinition.hostPort,
                ServerHostServiceGrpcKt.ServerHostServiceCoroutineStub(
                    createChannel(
                        serverHostDefinition.hostHost,
                        serverHostDefinition.hostPort
                    )
                ).withCallCredentials(credentials),
            )
        }

        @JvmStatic
        fun createChannel(host: String, port: Int): ManagedChannel {
            return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
        }
    }
}