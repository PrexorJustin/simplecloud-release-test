package app.simplecloud.controller.api.impl.coroutines

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.api.GroupApi
import app.simplecloud.controller.api.ServerApi
import app.simplecloud.droplet.api.auth.AuthCallCredentials
import app.simplecloud.pubsub.PubSubClient
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

class ControllerApiCoroutineImpl(
    authSecret: String
): ControllerApi.Coroutine {

    private val authCallCredentials = AuthCallCredentials(authSecret)

    private val managedChannel = createManagedChannelFromEnv()
    private val groups: GroupApi.Coroutine = GroupApiCoroutineImpl(managedChannel, authCallCredentials)
    private val servers: ServerApi.Coroutine = ServerApiCoroutineImpl(managedChannel, authCallCredentials)


    private val pubSubClient = PubSubClient(
        System.getenv("CONTROLLER_PUBSUB_HOST") ?: "localhost",
        System.getenv("CONTROLLER_PUBSUB_PORT")?.toInt() ?: 5817,
        authCallCredentials,
    )

    /**
     * @return The controllers [GroupApi.Coroutine]
     */
    override fun getGroups(): GroupApi.Coroutine {
        return groups
    }

    /**
     * @return The controllers [ServerApi.Coroutine]
     */
    override fun getServers(): ServerApi.Coroutine {
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