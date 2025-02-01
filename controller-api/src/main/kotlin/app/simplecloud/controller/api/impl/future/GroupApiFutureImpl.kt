package app.simplecloud.controller.api.impl.future

import app.simplecloud.controller.api.GroupApi
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.droplet.api.auth.AuthCallCredentials
import app.simplecloud.droplet.api.future.toCompletable
import build.buf.gen.simplecloud.controller.v1.ControllerGroupServiceGrpc
import build.buf.gen.simplecloud.controller.v1.CreateGroupRequest
import build.buf.gen.simplecloud.controller.v1.DeleteGroupByNameRequest
import build.buf.gen.simplecloud.controller.v1.GetGroupByNameRequest
import build.buf.gen.simplecloud.controller.v1.GetAllGroupsRequest
import build.buf.gen.simplecloud.controller.v1.GetGroupsByTypeRequest
import build.buf.gen.simplecloud.controller.v1.ServerType
import build.buf.gen.simplecloud.controller.v1.UpdateGroupRequest
import io.grpc.ManagedChannel
import java.util.concurrent.CompletableFuture

class GroupApiFutureImpl(
    managedChannel: ManagedChannel,
    authCallCredentials: AuthCallCredentials
) : GroupApi.Future {

    private val groupServiceStub: ControllerGroupServiceGrpc.ControllerGroupServiceFutureStub =
        ControllerGroupServiceGrpc.newFutureStub(managedChannel)
            .withCallCredentials(authCallCredentials)

    override fun getGroupByName(name: String): CompletableFuture<Group> {
        return groupServiceStub.getGroupByName(
            GetGroupByNameRequest.newBuilder()
                .setGroupName(name)
                .build()
        ).toCompletable()
            .thenApply {
                Group.fromDefinition(it.group)
            }
    }

    override fun deleteGroup(name: String): CompletableFuture<Group> {
        return groupServiceStub.deleteGroupByName(
            DeleteGroupByNameRequest.newBuilder()
                .setGroupName(name)
                .build()
        ).toCompletable()
            .thenApply {
                Group.fromDefinition(it)
            }
    }

    override fun createGroup(group: Group): CompletableFuture<Group> {
        return groupServiceStub.createGroup(
            CreateGroupRequest.newBuilder()
                .setGroup(group.toDefinition())
                .build()
        ).toCompletable()
            .thenApply {
                Group.fromDefinition(it)
            }
    }

    override fun updateGroup(group: Group): CompletableFuture<Group> {
        return groupServiceStub.updateGroup(
            UpdateGroupRequest.newBuilder()
                .setGroup(group.toDefinition())
                .build()
        ).toCompletable().thenApply {
            return@thenApply Group.fromDefinition(it)
        }
    }

    override fun getAllGroups(): CompletableFuture<List<Group>> {
        return groupServiceStub.getAllGroups(GetAllGroupsRequest.newBuilder().build()).toCompletable().thenApply {
            return@thenApply it.groupsList.map { group -> Group.fromDefinition(group) }
        }
    }

    override fun getGroupsByType(type: ServerType): CompletableFuture<List<Group>> {
        return groupServiceStub.getGroupsByType(
            GetGroupsByTypeRequest.newBuilder()
                .setServerType(type)
                .build()
        ).toCompletable().thenApply {
            return@thenApply it.groupsList.map { group -> Group.fromDefinition(group) }
        }
    }

}