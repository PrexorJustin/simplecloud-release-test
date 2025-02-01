package app.simplecloud.controller.api

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.server.Server
import build.buf.gen.simplecloud.controller.v1.ServerStartCause
import build.buf.gen.simplecloud.controller.v1.ServerState
import build.buf.gen.simplecloud.controller.v1.ServerStopCause
import build.buf.gen.simplecloud.controller.v1.ServerType
import java.util.concurrent.CompletableFuture

interface ServerApi {

    interface Future {

        /**
         * @return a [CompletableFuture] with a [List] of all [Server]s
         */
        fun getAllServers(): CompletableFuture<List<Server>>

        /**
         * @return a [CompletableFuture] with the [Server] from the SIMPLECLOUD_UNIQUE_ID environment
         */
        fun getCurrentServer(): CompletableFuture<Server> {
            return getServerById(System.getenv("SIMPLECLOUD_UNIQUE_ID"))
        }

        /**
         * @param id the id of the server.
         * @return a [CompletableFuture] with the [Server].
         */
        fun getServerById(id: String): CompletableFuture<Server>

        /**
         * @param groupName the name of the server group.
         * @return a [CompletableFuture] with a [List] of [Server]s of that group.
         */
        fun getServersByGroup(groupName: String): CompletableFuture<List<Server>>

        /**
         * @param groupName the name of the server group.
         * @param numericalId the numerical id of the server.
         * @return a [CompletableFuture] with the [Server].
         */
        fun getServerByNumerical(groupName: String, numericalId: Long): CompletableFuture<Server>

        /**
         * @param group The server group.
         * @return a [CompletableFuture] with a [List] of [Server]s of that group.
         */
        fun getServersByGroup(group: Group): CompletableFuture<List<Server>>

        /**
         * @param type The servers type
         * @return a [CompletableFuture] with a [List] of all [Server]s with this type
         */
        fun getServersByType(type: ServerType): CompletableFuture<List<Server>>

        /**
         * @param groupName the group name of the group the new server should be of.
         * @return a [CompletableFuture] with a [Server] or null.
         */
        fun startServer(
            groupName: String,
            startCause: ServerStartCause = ServerStartCause.API_START
        ): CompletableFuture<Server?>

        /**
         * @param groupName the group name of the servers group.
         * @param numericalId the numerical id of the server.
         * @return a [CompletableFuture] with the stopped [Server].
         */
        fun stopServer(
            groupName: String,
            numericalId: Long,
            stopCause: ServerStopCause = ServerStopCause.API_STOP
        ): CompletableFuture<Server>

        /**
         * @param id the id of the server.
         * @return a [CompletableFuture] with the stopped [Server].
         */
        fun stopServer(id: String, stopCause: ServerStopCause = ServerStopCause.API_STOP): CompletableFuture<Server>

        /**
         * Stops all servers within a specified group.
         *
         * @param groupName The name of the server group to stop.
         * @param stopCause The reason for stopping the servers. Defaults to [ServerStopCause.API_STOP].
         * @return A [CompletableFuture] containing a list of stopped [Server] instances.
         */
        fun stopServers(
            groupName: String,
            stopCause: ServerStopCause = ServerStopCause.API_STOP
        ): CompletableFuture<List<Server>>

        /**
         * Stops all servers within a specified group and sets a timeout to prevent new server starts for the group.
         *
         * @param groupName The name of the server group to stop.
         * @param timeoutSeconds The duration (in seconds) for which new server starts will be prevented.
         * @param stopCause The reason for stopping the servers. Defaults to [ServerStopCause.API_STOP].
         * @return A [CompletableFuture] containing a list of stopped [Server] instances.
         */
        fun stopServers(
            groupName: String,
            timeoutSeconds: Int,
            stopCause: ServerStopCause = ServerStopCause.API_STOP
        ): CompletableFuture<List<Server>>

        /**
         * @param id the id of the server.
         * @param state the new state of the server.
         * @return a [CompletableFuture] with the updated [Server].
         */
        fun updateServerState(id: String, state: ServerState): CompletableFuture<Server>

        /**
         * @param id the id of the server.
         * @param key the server property key
         * @param value the new property value
         * @return a [CompletableFuture] with the updated [Server].
         */
        fun updateServerProperty(id: String, key: String, value: Any): CompletableFuture<Server>

    }

    interface Coroutine {

        /**
         * @return a [List] of all [Server]s
         */
        suspend fun getAllServers(): List<Server>

        /**
         * @return the [Server] from the SIMPLECLOUD_UNIQUE_ID environment
         */
        suspend fun getCurrentServer(): Server {
            return getServerById(System.getenv("SIMPLECLOUD_UNIQUE_ID"))
        }

        /**
         * @param id the id of the server.
         * @return the [Server].
         */
        suspend fun getServerById(id: String): Server

        /**
         * @param groupName the name of the server group.
         * @return a [List] of [Server]s of that group.
         */
        suspend fun getServersByGroup(groupName: String): List<Server>

        /**
         * @param groupName the name of the server group.
         * @param numericalId the numerical id of the server.
         * @return the [Server].
         */
        suspend fun getServerByNumerical(groupName: String, numericalId: Long): Server

        /**
         * @param group The server group.
         * @return a [List] of [Server]s of that group.
         */
        suspend fun getServersByGroup(group: Group): List<Server>

        /**
         * @param type The servers type
         * @return a [List] of all [Server]s with this type
         */
        suspend fun getServersByType(type: ServerType): List<Server>

        /**
         * @param groupName the group name of the group the new server should be of.
         * @return a [Server] or null.
         */
        suspend fun startServer(groupName: String, startCause: ServerStartCause = ServerStartCause.API_START): Server

        /**
         * @param groupName the group name of the servers group.
         * @param numericalId the numerical id of the server.
         * @return the stopped [Server].
         */
        suspend fun stopServer(
            groupName: String,
            numericalId: Long,
            stopCause: ServerStopCause = ServerStopCause.API_STOP
        ): Server

        /**
         * @param id the id of the server.
         * @return the stopped [Server].
         */
        suspend fun stopServer(id: String, stopCause: ServerStopCause = ServerStopCause.API_STOP): Server

        /**
         * Stops all servers within a specified group.
         *
         * @param groupName The name of the server group to stop.
         * @param stopCause The reason for stopping the servers. Defaults to [ServerStopCause.API_STOP].
         * @return A list of stopped [Server] instances.
         */
        suspend fun stopServers(
            groupName: String,
            stopCause: ServerStopCause = ServerStopCause.API_STOP
        ): List<Server>

        /**
         * Stops all servers within a specified group and sets a timeout to prevent new server starts for the group.
         *
         * @param groupName The name of the server group to stop.
         * @param timeoutSeconds The duration (in seconds) for which new server starts will be prevented.
         * @param stopCause The reason for stopping the servers. Defaults to [ServerStopCause.API_STOP].
         * @return A list of stopped [Server] instances.
         */
        suspend fun stopServers(
            groupName: String,
            timeoutSeconds: Int,
            stopCause: ServerStopCause = ServerStopCause.API_STOP
        ): List<Server>

        /**
         * @param id the id of the server.
         * @param state the new state of the server.
         * @return the updated [Server].
         */
        suspend fun updateServerState(id: String, state: ServerState): Server

        /**
         * @param id the id of the server.
         * @param key the server property key
         * @param value the new property value
         * @return the updated [Server].
         */
        suspend fun updateServerProperty(id: String, key: String, value: Any): Server

    }

}