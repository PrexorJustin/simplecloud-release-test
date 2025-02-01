package app.simplecloud.controller.runtime


interface Repository<E, I> {
    suspend fun delete(element: E): Boolean
    fun save(element: E)
    suspend fun find(identifier: I): E?
    suspend fun getAll(): List<E>
}