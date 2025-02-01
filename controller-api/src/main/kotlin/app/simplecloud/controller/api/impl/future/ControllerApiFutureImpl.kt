package app.simplecloud.controller.api.impl.future

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.api.GroupApi
import app.simplecloud.controller.api.ServerApi
import app.simplecloud.droplet.api.auth.AuthCallCredentials
import app.simplecloud.pubsub.PubSubClient
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

class ControllerApiFutureImpl(
    authSecret: String
): ControllerApi.Future {

    private val authCallCredentials = AuthCallCredentials(authSecret)

    private val managedChannel = createManagedChannelFromEnv()
    private val groups: GroupApi.Future = GroupApiFutureImpl(managedChannel, authCallCredentials)
    private val servers: ServerApi.Future = ServerApiFutureImpl(managedChannel, authCallCredentials)


    private val pubSubClient = PubSubClient(
        System.getenv("CONTROLLER_PUBSUB_HOST") ?: "localhost",
        System.getenv("CONTROLLER_PUBSUB_PORT")?.toInt() ?: 5817,
        authCallCredentials,
    )

    /**
     * @return The controllers [GroupApi.Future]
     */
    override fun getGroups(): GroupApi.Future {
        return groups
    }

    /**
     * @return The controllers [ServerApi.Future]
     */
    override fun getServers(): ServerApi.Future {
        return servers
    }

    /**
     * @return The [PubSubClient] to subscribe to Controller events and send messages
     */
    override fun getPubSubClient(): PubSubClient {
        return pubSubClient
    }

    private fun createManagedChannelFromEnv(): ManagedChannel {
        val host = System.getenv("CONTROLLER_HOST") ?: "127.0.0.1"
        val port = System.getenv("CONTROLLER_PORT")?.toInt() ?: 5816
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    }

}