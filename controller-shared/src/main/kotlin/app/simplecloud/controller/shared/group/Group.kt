package app.simplecloud.controller.shared.group

import build.buf.gen.simplecloud.controller.v1.GroupDefinition
import build.buf.gen.simplecloud.controller.v1.ServerType
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class Group(
    val name: String = "",
    val type: ServerType = ServerType.UNKNOWN_SERVER,
    val minMemory: Long = 0,
    val maxMemory: Long = 0,
    val startPort: Long = 0,
    val minOnlineCount: Long = 0,
    val maxOnlineCount: Long = 0,
    val maxPlayers: Long = 0,
    val newServerPlayerRatio: Long = -1,
    val properties: Map<String, String> = mutableMapOf()
) {

    @Transient
    var timeout: GroupTimeout? = null

    fun toDefinition(): GroupDefinition {
        return GroupDefinition.newBuilder()
            .setName(name)
            .setType(type)
            .setMinimumMemory(minMemory)
            .setMaximumMemory(maxMemory)
            .setStartPort(startPort)
            .setMinimumOnlineCount(minOnlineCount)
            .setMaximumOnlineCount(maxOnlineCount)
            .setMaxPlayers(maxPlayers)
            .setNewServerPlayerRatio(newServerPlayerRatio)
            .putAllCloudProperties(properties)
            .build()
    }

    companion object {
        @JvmStatic
        fun fromDefinition(groupDefinition: GroupDefinition): Group {
            return Group(
                groupDefinition.name,
                groupDefinition.type,
                groupDefinition.minimumMemory,
                groupDefinition.maximumMemory,
                groupDefinition.startPort,
                groupDefinition.minimumOnlineCount,
                groupDefinition.maximumOnlineCount,
                groupDefinition.maxPlayers,
                groupDefinition.newServerPlayerRatio,
                groupDefinition.cloudPropertiesMap
            )
        }
    }

}
