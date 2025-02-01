package app.simplecloud.controller.runtime.group

import app.simplecloud.controller.shared.group.Group
import build.buf.gen.simplecloud.controller.v1.*
import io.grpc.Status
import io.grpc.StatusException

class GroupService(
    private val groupRepository: GroupRepository
) : ControllerGroupServiceGrpcKt.ControllerGroupServiceCoroutineImplBase() {

    override suspend fun getGroupByName(request: GetGroupByNameRequest): GetGroupByNameResponse {
        val group = groupRepository.find(request.groupName)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("This group does not exist"))
        return getGroupByNameResponse { this.group = group.toDefinition() }
    }

    override suspend fun getAllGroups(request: GetAllGroupsRequest): GetAllGroupsResponse {
        val allGroups = groupRepository.getAll()
        return getAllGroupsResponse {
            groups.addAll(allGroups.map { it.toDefinition() })
        }
    }

    override suspend fun getGroupsByType(request: GetGroupsByTypeRequest): GetGroupsByTypeResponse {
        val type = request.serverType
        val typedGroups = groupRepository.getAll().filter { it.type == type }
        return getGroupsByTypeResponse {
            groups.addAll(typedGroups.map { it.toDefinition() })
        }
    }

    override suspend fun updateGroup(request: UpdateGroupRequest): GroupDefinition {
        groupRepository.find(request.group.name)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("This group does not exist"))
        val group = Group.fromDefinition(request.group)

        if (group.minMemory > group.maxMemory) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Minimum memory must be smaller than maximum memory"))
        }

        try {
            groupRepository.save(group)
        } catch (e: Exception) {
            throw StatusException(Status.INTERNAL.withDescription("Error whilst updating group").withCause(e))
        }
        return group.toDefinition()
    }

    override suspend fun createGroup(request: CreateGroupRequest): GroupDefinition {
        if (groupRepository.find(request.group.name) != null) {
            throw StatusException(Status.NOT_FOUND.withDescription("This group already exists"))
        }
        val group = Group.fromDefinition(request.group)

        if (group.minMemory > group.maxMemory) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Minimum memory must be smaller than maximum memory"))
        }

        try {
            groupRepository.save(group)
        } catch (e: Exception) {
            throw StatusException(Status.INTERNAL.withDescription("Error whilst creating group").withCause(e))
        }
        return group.toDefinition()
    }

    override suspend fun deleteGroupByName(request: DeleteGroupByNameRequest): GroupDefinition {
        val group = groupRepository.find(request.groupName)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("This group does not exist"))
        val deleted = groupRepository.delete(group)
        if (!deleted) throw StatusException(Status.NOT_FOUND.withDescription("Could not delete this group"))
        return group.toDefinition()
    }

}