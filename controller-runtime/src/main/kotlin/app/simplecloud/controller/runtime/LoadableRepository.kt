package app.simplecloud.controller.runtime

interface LoadableRepository<E, I> : Repository<E, I> {
    fun load(): List<E>
}