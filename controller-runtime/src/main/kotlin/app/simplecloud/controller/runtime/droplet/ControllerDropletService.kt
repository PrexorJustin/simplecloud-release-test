package app.simplecloud.controller.runtime.droplet

import app.simplecloud.droplet.api.droplet.Droplet
import build.buf.gen.simplecloud.controller.v1.*
import io.grpc.Status
import io.grpc.StatusException

class ControllerDropletService(private val dropletRepository: DropletRepository) :
    ControllerDropletServiceGrpcKt.ControllerDropletServiceCoroutineImplBase() {
    override suspend fun getDroplet(request: GetDropletRequest): GetDropletResponse {
        val droplet = dropletRepository.find(request.type, request.id)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("This Droplet does not exist"))
        return getDropletResponse { this.definition = droplet.toDefinition() }
    }

    override suspend fun getAllDroplets(request: GetAllDropletsRequest): GetAllDropletsResponse {
        val allDroplets = dropletRepository.getAll()
        return getAllDropletsResponse {
            definition.addAll(allDroplets.map { it.toDefinition() })
        }
    }

    override suspend fun getDropletsByType(request: GetDropletsByTypeRequest): GetDropletsByTypeResponse {
        val type = request.type
        val typedDroplets = dropletRepository.getAll().filter { it.type == type }
        return getDropletsByTypeResponse {
            definition.addAll(typedDroplets.map { it.toDefinition() })
        }
    }

    override suspend fun registerDroplet(request: RegisterDropletRequest): RegisterDropletResponse {
        val droplet = Droplet.fromDefinition(request.definition)
        try {
            dropletRepository.delete(droplet)
            dropletRepository.save(droplet)
        } catch (e: Exception) {
            throw StatusException(Status.INTERNAL.withDescription("Error whilst registering Droplet").withCause(e))
        }
        return registerDropletResponse { this.definition = droplet.toDefinition() }
    }

    override suspend fun unregisterDroplet(request: UnregisterDropletRequest): UnregisterDropletResponse {
        val droplet = dropletRepository.find(request.id)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("This Droplet does not exist"))
        val deleted = dropletRepository.delete(droplet)
        if (!deleted) throw StatusException(Status.NOT_FOUND.withDescription("Could not delete this Droplet"))
        return unregisterDropletResponse { }
    }
}