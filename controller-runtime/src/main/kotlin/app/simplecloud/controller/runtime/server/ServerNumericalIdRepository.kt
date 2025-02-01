package app.simplecloud.controller.runtime.server

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class ServerNumericalIdRepository {

    private val numericalIds = ConcurrentHashMap<String, Set<Int>>()
    private val mutex = Mutex()

    suspend fun findNextNumericalId(group: String): Int {
        return mutex.withLock {
            val numericalIds = findNumericalIds(group)
            var nextId = 1
            while (numericalIds.contains(nextId)) {
                nextId++
            }
            saveNumericalId(group, nextId)
            nextId
        }
    }

    fun saveNumericalId(group: String, id: Int) {
        numericalIds.compute(group) { _, v -> v?.plus(id) ?: setOf(id) }
    }

    suspend fun removeNumericalId(group: String, id: Int): Boolean {
        return mutex.withLock {
            numericalIds.computeIfPresent(group) { _, v -> v.minus(id) } != null
        }
    }

    fun findNumericalIds(group: String): Set<Int> {
        return numericalIds[group] ?: emptySet()
    }
}
