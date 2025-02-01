package app.simplecloud.controller.api.dsl.builders

import app.simplecloud.controller.api.dsl.markers.ServerDsl
import build.buf.gen.simplecloud.controller.v1.ServerStartCause
import build.buf.gen.simplecloud.controller.v1.ServerState
import build.buf.gen.simplecloud.controller.v1.ServerStopCause

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

    fun getProperties(): Map<String, String> = properties.toMap()
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
