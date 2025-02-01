package app.simplecloud.controller.api.dsl.scopes

import app.simplecloud.controller.api.ServerApi
import app.simplecloud.controller.api.dsl.builders.ServerStartBuilder
import app.simplecloud.controller.api.dsl.builders.ServerStopBuilder
import app.simplecloud.controller.api.dsl.markers.ServerDsl
import build.buf.gen.simplecloud.controller.v1.ServerState

@ServerDsl
class ServerScope(private val api: ServerApi.Coroutine) {

    suspend fun start(groupName: String, block: ServerStartBuilder.() -> Unit) {
        val builder = ServerStartBuilder().apply(block)
        api.startServer(groupName, builder.startCause)
    }

    suspend fun stop(id: String, block: ServerStopBuilder.() -> Unit) {
        val builder = ServerStopBuilder().apply(block)
        api.stopServer(id, builder.stopCause)
    }

    suspend fun updateState(id: String, state: ServerState) {
        api.updateServerState(id, state)
    }

}