package app.simplecloud.controller.runtime.server

import app.simplecloud.controller.runtime.host.ServerHostRepository
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.coroutineScope
import org.apache.logging.log4j.LogManager

class ServerHostAttacher(
    private val hostRepository: ServerHostRepository,
    private val serverRepository: ServerRepository
) {

    private val logger = LogManager.getLogger(ServerHostAttacher::class.java)

    suspend fun attach(serverHost: ServerHost) {
        hostRepository.delete(serverHost)
        hostRepository.save(serverHost)
        logger.info("Successfully registered ServerHost ${serverHost.id}.")

        coroutineScope {
            serverRepository.findServersByHostId(serverHost.id).forEach { server ->
                logger.info("Reattaching Server ${server.uniqueId} of group ${server.group}...")
                try {
                    val result = serverHost.stub?.reattachServer(server.toDefinition())
                        ?: throw StatusException(Status.INTERNAL.withDescription("Could not reattach server, is the host misconfigured?"))
                    serverRepository.save(Server.fromDefinition(result))
                    logger.info("Success!")
                } catch (e: Exception) {
                    logger.error("Server was found to be offline, unregistering...")
                    serverRepository.delete(server)
                }
            }
        }
    }
}