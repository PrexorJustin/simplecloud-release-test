package app.simplecloud.controller.api.dsl.extensions

import app.simplecloud.controller.api.ServerApi
import app.simplecloud.controller.api.dsl.markers.ServerDsl
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.server.Server
import build.buf.gen.simplecloud.controller.v1.ServerStartCause
import build.buf.gen.simplecloud.controller.v1.ServerState
import build.buf.gen.simplecloud.controller.v1.ServerStopCause
import build.buf.gen.simplecloud.controller.v1.ServerType

@ServerDsl
class ServerStartBuilder {
    var startCause: ServerStartCause = ServerStartCause.API_START
    private val properties = mutableMapOf<String, String>()

    operator fun String.unaryPlus() = this

    operator fun Pair<String, String>.unaryPlus() {
        properties[first] = second
    }

    infix fun String.to(value: String) {
        properties[this] = value
    }

    fun getProperties(): Map<String, String> = properties
}

@ServerDsl
class ServerStopBuilder {
    var stopCause: ServerStopCause = ServerStopCause.API_STOP
}

@ServerDsl
class ServerStateBuilder {
    var state: ServerState = ServerState.UNKNOWN_STATE
}

@ServerDsl
class ServerPropertyBuilder {
    private val properties = mutableMapOf<String, String>()

    operator fun String.unaryPlus() = this

    operator fun Pair<String, String>.unaryPlus() {
        properties[first] = second
    }

    infix fun String.to(value: String) {
        properties[this] = value
    }

    internal fun build(): Map<String, String> = properties.toMap()
}

suspend fun ServerApi.Coroutine.start(
    groupName: String,
    block: ServerStartBuilder.() -> Unit
): Server {
    val builder = ServerStartBuilder().apply(block)
    return startServer(groupName, builder.startCause)
}

suspend fun ServerApi.Coroutine.stop(
    id: String,
    block: ServerStopBuilder.() -> Unit
): Server {
    val builder = ServerStopBuilder().apply(block)
    return stopServer(id, builder.stopCause)
}

suspend fun ServerApi.Coroutine.stopByGroup(
    groupName: String,
    numericalId: Long,
    block: ServerStopBuilder.() -> Unit
): Server {
    val builder = ServerStopBuilder().apply(block)
    return stopServer(groupName, numericalId, builder.stopCause)
}

suspend fun ServerApi.Coroutine.updateState(
    id: String,
    block: ServerStateBuilder.() -> Unit
): Server {
    val builder = ServerStateBuilder().apply(block)
    return updateServerState(id, builder.state)
}

suspend fun ServerApi.Coroutine.updateProperty(
    id: String,
    block: ServerPropertyBuilder.() -> Unit
): Server {
    val builder = ServerPropertyBuilder().apply(block)
    var updatedServer: Server? = null
    builder.build().forEach { (key, value) ->
        updatedServer = updateServerProperty(id, key, value)
    }
    return updatedServer ?: throw IllegalStateException("Failed to update server properties")
}

suspend fun ServerApi.Coroutine.getAllServers(block: (List<Server>) -> Unit = {}) {
    block(getAllServers())
}

suspend fun ServerApi.Coroutine.getServer(
    id: String,
    block: (Server) -> Unit = {}
) {
    block(getServerById(id))
}

suspend fun ServerApi.Coroutine.getServersByGroup(
    groupName: String,
    block: (List<Server>) -> Unit = {}
) {
    block(getServersByGroup(groupName))
}

suspend fun ServerApi.Coroutine.getServersByGroup(
    group: Group,
    block: (List<Server>) -> Unit = {}
) {
    block(getServersByGroup(group))
}

suspend fun ServerApi.Coroutine.getServerByNumerical(
    groupName: String,
    numericalId: Long,
    block: (Server) -> Unit = {}
) {
    block(getServerByNumerical(groupName, numericalId))
}

suspend fun ServerApi.Coroutine.getServersByType(
    type: ServerType,
    block: (List<Server>) -> Unit = {}
) {
    block(getServersByType(type))
}