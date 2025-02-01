package app.simplecloud.controller.runtime.host

import app.simplecloud.controller.runtime.Repository
import app.simplecloud.controller.runtime.server.ServerRepository
import app.simplecloud.controller.shared.host.ServerHost
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ServerHostRepository : Repository<ServerHost, ServerRepository> {

    private val hosts: ConcurrentHashMap<String, ServerHost> = ConcurrentHashMap()

    fun findServerHostById(id: String): ServerHost? {
        return hosts.getOrDefault(hosts.keys.firstOrNull { it == id }, null)
    }

    override fun save(element: ServerHost) {
        hosts[element.id] = element
    }

    override suspend fun find(identifier: ServerRepository): ServerHost? {
        return mapHostsToServerHostWithServerCount(identifier).minByOrNull { it.serverCount }?.serverHost
    }

    suspend fun areServerHostsAvailable(): Boolean {
        return coroutineScope {
            return@coroutineScope hosts.any {
                val channel = it.value.stub?.channel as ManagedChannel
                val state = channel.getState(true)
                state == ConnectivityState.IDLE || state == ConnectivityState.READY
            }
        }
    }

    override suspend fun delete(element: ServerHost): Boolean {
        val host = hosts[element.id] ?: return false
        (host.stub?.channel as ManagedChannel).shutdown().awaitTermination(5L, TimeUnit.SECONDS)
        return hosts.remove(element.id, element)
    }

    override suspend fun getAll(): List<ServerHost> {
        return hosts.values.toList()
    }

    private suspend fun mapHostsToServerHostWithServerCount(identifier: ServerRepository): List<ServerHostWithServerCount> {
        return hosts.values.map { serverHost ->
            ServerHostWithServerCount(serverHost, identifier.findServersByHostId(serverHost.id).size)
        }
    }

}

