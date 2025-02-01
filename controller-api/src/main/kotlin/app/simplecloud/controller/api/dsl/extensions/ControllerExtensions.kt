package app.simplecloud.controller.api.dsl.extensions

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.api.dsl.markers.ControllerDsl
import app.simplecloud.controller.api.dsl.scopes.GroupScope
import app.simplecloud.controller.api.dsl.scopes.ServerScope
import kotlinx.coroutines.coroutineScope

@ControllerDsl
class ControllerApiScope(private val api: ControllerApi.Coroutine) {
    suspend fun groups(block: suspend GroupScope.() -> Unit) {
        GroupScope(api.getGroups()).block()
    }

    suspend fun servers(block: suspend ServerScope.() -> Unit) {
        ServerScope(api.getServers()).block()
    }
}

suspend fun controllerApiScope(block: suspend ControllerApiScope.() -> Unit) {
    val api = ControllerApi.createCoroutineApi()
    controllerApiScope(api, block)
}

suspend fun controllerApiScope(
    api: ControllerApi.Coroutine,
    block: suspend ControllerApiScope.() -> Unit
) {
    coroutineScope {
        ControllerApiScope(api).block()
    }
}
