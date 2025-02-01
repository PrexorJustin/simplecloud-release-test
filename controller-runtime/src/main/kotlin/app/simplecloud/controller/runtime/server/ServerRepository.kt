package app.simplecloud.controller.runtime.server

import app.simplecloud.controller.runtime.LoadableRepository
import app.simplecloud.controller.runtime.database.Database
import app.simplecloud.controller.shared.db.tables.records.CloudServersRecord
import app.simplecloud.controller.shared.db.tables.references.CLOUD_SERVERS
import app.simplecloud.controller.shared.db.tables.references.CLOUD_SERVER_PROPERTIES
import app.simplecloud.controller.shared.server.Server
import build.buf.gen.simplecloud.controller.v1.ServerState
import build.buf.gen.simplecloud.controller.v1.ServerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withContext
import org.jooq.exception.DataAccessException
import java.time.LocalDateTime

class ServerRepository(
    private val database: Database,
    private val numericalIdRepository: ServerNumericalIdRepository,
) : LoadableRepository<Server, String> {

    override suspend fun find(identifier: String): Server? {
       return database.context.selectFrom(CLOUD_SERVERS)
            .where(CLOUD_SERVERS.UNIQUE_ID.eq(identifier))
            .limit(1)
            .awaitFirstOrNull()
            ?.let { record -> mapCloudServersRecordToServer(record) }
    }


    suspend fun findServerByNumerical(group: String, id: Int): Server? {
        return database.context.selectFrom(CLOUD_SERVERS)
            .where(
                CLOUD_SERVERS.GROUP_NAME.eq(group)
                    .and(CLOUD_SERVERS.NUMERICAL_ID.eq(id))
            )
            .limit(1)
            .awaitFirstOrNull()
            ?.let { record -> mapCloudServersRecordToServer(record) }
    }

    override suspend fun getAll(): List<Server> {
        return database.context.selectFrom(CLOUD_SERVERS)
            .asFlow()
            .toCollection(mutableListOf())
            .map { record -> mapCloudServersRecordToServer(record) }
    }

    suspend fun findServersByHostId(id: String): List<Server> {
        return database.context.selectFrom(CLOUD_SERVERS)
            .where(CLOUD_SERVERS.HOST_ID.eq(id))
            .asFlow()
            .toCollection(mutableListOf())
            .map { record -> mapCloudServersRecordToServer(record) }
    }

    suspend fun findServersByGroup(group: String): List<Server> {
        return database.context.selectFrom(CLOUD_SERVERS)
            .where(CLOUD_SERVERS.GROUP_NAME.eq(group))
            .asFlow()
            .toCollection(mutableListOf())
            .map { record -> mapCloudServersRecordToServer(record) }
    }

    suspend fun findServersByType(type: ServerType): List<Server> {
        return database.context.selectFrom(CLOUD_SERVERS)
            .where(CLOUD_SERVERS.TYPE.eq(type.toString()))
            .asFlow()
            .toCollection(mutableListOf())
            .map { record -> mapCloudServersRecordToServer(record) }
    }

    private fun mapCloudServersRecordToServer(record: CloudServersRecord): Server {
        val propertiesQuery =
            database.context.select()
                .from(CLOUD_SERVER_PROPERTIES)
                .where(CLOUD_SERVER_PROPERTIES.SERVER_ID.eq(record.uniqueId))
                .fetchInto(CLOUD_SERVER_PROPERTIES)

        return Server(
            record.uniqueId!!,
            ServerType.valueOf(record.type!!),
            record.groupName!!,
            record.hostId!!,
            record.numericalId!!,
            record.ip!!,
            record.port!!.toLong(),
            record.minimumMemory!!.toLong(),
            record.maximumMemory!!.toLong(),
            record.maxPlayers!!.toLong(),
            record.playerCount!!.toLong(),
            propertiesQuery.map { item ->
                item.key!! to item.value!!
            }.toMap().toMutableMap(),
            ServerState.valueOf(record.state!!),
            record.createdAt!!,
            record.updatedAt!!
        )
    }

    override suspend fun delete(element: Server): Boolean {
        val canDelete =
            withContext(Dispatchers.IO) {
                try {
                    database.context.deleteFrom(CLOUD_SERVER_PROPERTIES)
                        .where(CLOUD_SERVER_PROPERTIES.SERVER_ID.eq(element.uniqueId))
                        .execute()
                    return@withContext true
                } catch (e: DataAccessException) {
                    return@withContext false
                }
            }
        if (!canDelete) return false
        numericalIdRepository.removeNumericalId(element.group, element.numericalId)
        return withContext(Dispatchers.IO) {
            database.context.deleteFrom(CLOUD_SERVERS)
                .where(CLOUD_SERVERS.UNIQUE_ID.eq(element.uniqueId))
                .execute() > 0
        }
    }

    @Synchronized
    override fun save(element: Server) {
        numericalIdRepository.saveNumericalId(element.group, element.numericalId)

        val currentTimestamp = LocalDateTime.now()
        database.context.insertInto(
            CLOUD_SERVERS,

            CLOUD_SERVERS.UNIQUE_ID,
            CLOUD_SERVERS.TYPE,
            CLOUD_SERVERS.GROUP_NAME,
            CLOUD_SERVERS.HOST_ID,
            CLOUD_SERVERS.NUMERICAL_ID,
            CLOUD_SERVERS.IP,
            CLOUD_SERVERS.PORT,
            CLOUD_SERVERS.MINIMUM_MEMORY,
            CLOUD_SERVERS.MAXIMUM_MEMORY,
            CLOUD_SERVERS.MAX_PLAYERS,
            CLOUD_SERVERS.PLAYER_COUNT,
            CLOUD_SERVERS.STATE,
            CLOUD_SERVERS.CREATED_AT,
            CLOUD_SERVERS.UPDATED_AT
        )
            .values(
                element.uniqueId,
                element.type.toString(),
                element.group,
                element.host,
                element.numericalId,
                element.ip,
                element.port.toInt(),
                element.minMemory.toInt(),
                element.maxMemory.toInt(),
                element.maxPlayers.toInt(),
                element.playerCount.toInt(),
                element.state.toString(),
                currentTimestamp,
                currentTimestamp
            )
            .onDuplicateKeyUpdate()
            .set(CLOUD_SERVERS.UNIQUE_ID, element.uniqueId)
            .set(CLOUD_SERVERS.TYPE, element.type.toString())
            .set(CLOUD_SERVERS.GROUP_NAME, element.group)
            .set(CLOUD_SERVERS.HOST_ID, element.host)
            .set(CLOUD_SERVERS.NUMERICAL_ID, element.numericalId)
            .set(CLOUD_SERVERS.IP, element.ip)
            .set(CLOUD_SERVERS.PORT, element.port.toInt())
            .set(CLOUD_SERVERS.MINIMUM_MEMORY, element.minMemory.toInt())
            .set(CLOUD_SERVERS.MAXIMUM_MEMORY, element.maxMemory.toInt())
            .set(CLOUD_SERVERS.MAX_PLAYERS, element.maxPlayers.toInt())
            .set(CLOUD_SERVERS.PLAYER_COUNT, element.playerCount.toInt())
            .set(CLOUD_SERVERS.STATE, element.state.toString())
            .set(CLOUD_SERVERS.UPDATED_AT, currentTimestamp)
            .executeAsync()
        element.properties.forEach {
            database.context.insertInto(
                CLOUD_SERVER_PROPERTIES,

                CLOUD_SERVER_PROPERTIES.SERVER_ID,
                CLOUD_SERVER_PROPERTIES.KEY,
                CLOUD_SERVER_PROPERTIES.VALUE
            )
                .values(
                    element.uniqueId,
                    it.key,
                    it.value
                )
                .onDuplicateKeyUpdate()
                .set(CLOUD_SERVER_PROPERTIES.SERVER_ID, element.uniqueId)
                .set(CLOUD_SERVER_PROPERTIES.KEY, it.key)
                .set(CLOUD_SERVER_PROPERTIES.VALUE, it.value)
                .executeAsync()
        }
    }

    override fun load(): List<Server> {
        val cloudServerList = database.context.selectFrom(CLOUD_SERVERS)
            .fetchInto(CLOUD_SERVERS)
            .map { record -> mapCloudServersRecordToServer(record) }

        cloudServerList.forEach {
            numericalIdRepository.saveNumericalId(it.group, it.numericalId)
        }

        return cloudServerList
    }

}