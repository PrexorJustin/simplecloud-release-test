package app.simplecloud.controller.api.dsl.extensions

import app.simplecloud.controller.api.GroupApi
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

    fun properties(block: ServerPropertyBuilder.() -> Unit) {
        val builder = ServerPropertyBuilder().apply(block)
        properties.putAll(builder.build())
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

    fun properties(block: ServerPropertyBuilder.() -> Unit) {
        val builder = ServerPropertyBuilder().apply(block)
        properties.putAll(builder.build())
    }

    internal fun build(): Group {
        requireNotNull(group) { "Group must be set using fromGroup()" }
        return group!!.copy(properties = properties)
    }
}

suspend fun GroupApi.Coroutine.create(block: GroupCreateBuilder.() -> Unit): Group {
    val builder = GroupCreateBuilder().apply(block)
    return createGroup(builder.build())
}

suspend fun GroupApi.Coroutine.update(block: GroupUpdateBuilder.() -> Unit): Group {
    val builder = GroupUpdateBuilder().apply(block)
    return updateGroup(builder.build())
}

suspend fun GroupApi.Coroutine.delete(
    name: String,
    block: (Group) -> Unit = {}
) {
    block(deleteGroup(name))
}

suspend fun GroupApi.Coroutine.getGroup(
    name: String,
    block: (Group) -> Unit = {}
) {
    block(getGroupByName(name))
}

suspend fun GroupApi.Coroutine.getAllGroups(
    block: (List<Group>) -> Unit = {}
) {
    block(getAllGroups())
}

suspend fun GroupApi.Coroutine.getGroupsByType(
    type: ServerType,
    block: (List<Group>) -> Unit = {}
) {
    block(getGroupsByType(type))
}