package app.simplecloud.controller.api.impl.coroutines

import app.simplecloud.controller.api.GroupApi
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.droplet.api.auth.AuthCallCredentials
import build.buf.gen.simplecloud.controller.v1.*
import io.grpc.ManagedChannel

class GroupApiCoroutineImpl(
    managedChannel: ManagedChannel,
    authCallCredentials: AuthCallCredentials
) : GroupApi.Coroutine {

    private val groupServiceStub: ControllerGroupServiceGrpcKt.ControllerGroupServiceCoroutineStub =
        ControllerGroupServiceGrpcKt.ControllerGroupServiceCoroutineStub(managedChannel)
            .withCallCredentials(authCallCredentials)

    override suspend fun getGroupByName(name: String): Group {
        return Group.fromDefinition(
            groupServiceStub.getGroupByName(getGroupByNameRequest {
                groupName = name
            }).group
        )
    }

    override suspend fun deleteGroup(name: String): Group {
        return Group.fromDefinition(
            groupServiceStub.deleteGroupByName(deleteGroupByNameRequest {
                groupName = name
            })
        )
    }

    override suspend fun createGroup(group: Group): Group {
        return Group.fromDefinition(
            groupServiceStub.createGroup(createGroupRequest {
                this.group = group.toDefinition()
            })
        )
    }

    override suspend fun updateGroup(group: Group): Group {
        return Group.fromDefinition(
            groupServiceStub.updateGroup(updateGroupRequest {
                this.group = group.toDefinition()
            })
        )
    }

    override suspend fun getAllGroups(): List<Group> {
        return groupServiceStub.getAllGroups(getAllGroupsRequest {}).groupsList.map {
            Group.fromDefinition(it)
        }
    }

    override suspend fun getGroupsByType(type: ServerType): List<Group> {
        return groupServiceStub.getGroupsByType(getGroupsByTypeRequest {
            serverType = type
        }).groupsList.map {
            Group.fromDefinition(it)
        }
    }

}