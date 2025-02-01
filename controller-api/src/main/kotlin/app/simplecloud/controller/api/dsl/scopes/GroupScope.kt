package app.simplecloud.controller.api.dsl.scopes

import app.simplecloud.controller.api.GroupApi
import app.simplecloud.controller.api.dsl.builders.PropertyBuilder
import app.simplecloud.controller.api.dsl.markers.GroupDsl
import app.simplecloud.controller.shared.group.Group
import build.buf.gen.simplecloud.controller.v1.ServerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@GroupDsl
class GroupScope(private val api: GroupApi.Coroutine) {
    suspend fun create(block: GroupCreateBuilder.() -> Unit): Group {
        val builder = GroupCreateBuilder().apply(block)
        return api.createGroup(builder.build())
    }

    suspend fun update(block: GroupUpdateBuilder.() -> Unit): Group {
        val builder = GroupUpdateBuilder().apply(block)
        return api.updateGroup(builder.build())
    }

    suspend fun delete(name: String) = api.deleteGroup(name)

    suspend fun getByName(name: String) = api.getGroupByName(name)

    suspend fun getAll() = api.getAllGroups()

    suspend fun getByType(type: ServerType) = api.getGroupsByType(type)

    suspend fun parallel(block: suspend ParallelGroupScope.() -> Unit) {
        coroutineScope {
            ParallelGroupScope(this, api).block()
        }
    }
}

class ParallelGroupScope(
    private val scope: CoroutineScope,
    private val api: GroupApi.Coroutine
) {
    fun getByName(name: String) = scope.async { api.getGroupByName(name) }
    fun getAll() = scope.async { api.getAllGroups() }
    fun getByType(type: ServerType) = scope.async { api.getGroupsByType(type) }
}

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
    private val propertyBuilder = PropertyBuilder()

    fun properties(block: PropertyBuilder.() -> Unit) {
        propertyBuilder.apply(block)
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
        properties = propertyBuilder.build()
    )
}

@GroupDsl
class GroupUpdateBuilder {
    private var existingGroup: Group? = null
    private var propertyBuilder = PropertyBuilder()

    fun fromGroup(group: Group) {
        existingGroup = group
        propertyBuilder.properties(*group.properties.toList().toTypedArray())
    }

    fun properties(block: PropertyBuilder.() -> Unit) {
        propertyBuilder.apply(block)
    }

    internal fun build(): Group {
        requireNotNull(existingGroup) { "Existing group must be set using fromGroup()" }
        return existingGroup!!.copy(properties = propertyBuilder.build())
    }
}
