package app.simplecloud.controller.api.dsl.builders

import app.simplecloud.controller.api.dsl.markers.GroupDsl
import app.simplecloud.controller.shared.group.Group
import build.buf.gen.simplecloud.controller.v1.ServerType

@GroupDsl
class GroupCreateBuilder {
    var name: String = ""
    var type: ServerType = ServerType.UNKNOWN_SERVER
    var minMemory: Long = 0
    var maxMemory: Long = 0
    var startPort: Long = 0
    var minOnlineCount: Long = 0
    var maxOnlineCount: Long = 0
    var maxPlayers: Long = 0
    var newServerPlayerRatio: Long = -1
    private val properties = mutableMapOf<String, String>()

    operator fun String.unaryPlus() = this

    operator fun Pair<String, String>.unaryPlus() {
        properties[first] = second
    }

    infix fun String.to(value: String) {
        properties[this] = value
    }

    internal fun build() = Group(
        name = name,
        type = type,
        minMemory = minMemory,
        maxMemory = maxMemory,
        startPort = startPort,
        minOnlineCount = minOnlineCount,
        maxOnlineCount = maxOnlineCount,
        maxPlayers = maxPlayers,
        newServerPlayerRatio = newServerPlayerRatio,
        properties = properties
    )
}

@GroupDsl
class GroupUpdateBuilder {
    private var group: Group? = null
    private val properties = mutableMapOf<String, String>()

    fun fromGroup(existingGroup: Group) {
        group = existingGroup
        properties.putAll(existingGroup.properties)
    }

    operator fun String.unaryPlus() = this

    operator fun Pair<String, String>.unaryPlus() {
        properties[first] = second
    }

    infix fun String.to(value: String) {
        properties[this] = value
    }

    internal fun build(): Group {
        requireNotNull(group) { "Group must be set using fromGroup()" }
        return group!!.copy(properties = properties)
    }
}
