package app.simplecloud.controller.api.dsl.extensions

import app.simplecloud.controller.api.ServerApi
import app.simplecloud.controller.shared.server.Server

suspend fun ServerApi.Coroutine.startServer(
    groupName: String,
    block: ServerStartBuilder.() -> Unit = {}
): Server {
    val builder = ServerStartBuilder().apply(block)
    val server = startServer(groupName, builder.startCause)

    builder.getProperties().forEach { (key, value) ->
        updateServerProperty(server.uniqueId, key, value)
    }

    return getServerById(server.uniqueId)
}

suspend fun ServerApi.Coroutine.stopServer(
    id: String,
    block: ServerStopBuilder.() -> Unit = {}
): Server {
    val builder = ServerStopBuilder().apply(block)
    return stopServer(id, builder.stopCause)
}

suspend fun ServerApi.Coroutine.updateServerState(
    id: String,
    block: ServerStateBuilder.() -> Unit
): Server {
    val builder = ServerStateBuilder().apply(block)
    return updateServerState(id, builder.state)
}

suspend fun ServerApi.Coroutine.updateProperty(
    id: String,
    key: String,
    value: Any
): Server {
    return updateServerProperty(id, key, value)
}

suspend fun ServerApi.Coroutine.updateProperties(
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
