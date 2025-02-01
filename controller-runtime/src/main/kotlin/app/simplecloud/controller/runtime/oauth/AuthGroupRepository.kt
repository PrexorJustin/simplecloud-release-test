package app.simplecloud.controller.runtime.oauth

import app.simplecloud.controller.runtime.Repository
import app.simplecloud.controller.runtime.database.Database
import app.simplecloud.controller.shared.db.tables.records.Oauth2GroupsRecord
import app.simplecloud.controller.shared.db.tables.references.OAUTH2_GROUPS
import app.simplecloud.droplet.api.auth.OAuthGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withContext
import org.jooq.exception.DataAccessException

class AuthGroupRepository(private val database: Database) : Repository<OAuthGroup, String> {
    override suspend fun getAll(): List<OAuthGroup> {
        return database.context.selectFrom(OAUTH2_GROUPS).asFlow().toCollection(mutableListOf())
            .map { mapRecordToGroup(it) }
    }

    override suspend fun find(identifier: String): OAuthGroup? {
        return database.context.selectFrom(OAUTH2_GROUPS).where(OAUTH2_GROUPS.GROUP_NAME.eq(identifier)).limit(1)
            .awaitFirstOrNull()?.let { mapRecordToGroup(it) }
    }

    override fun save(element: OAuthGroup) {
        database.context.insertInto(
            OAUTH2_GROUPS,

            OAUTH2_GROUPS.GROUP_NAME, OAUTH2_GROUPS.SCOPES
        ).values(
            element.name,
            element.scopes.joinToString(";"),
        ).onDuplicateKeyUpdate().set(OAUTH2_GROUPS.GROUP_NAME, element.name)
            .set(OAUTH2_GROUPS.SCOPES, element.scopes.joinToString(";")).executeAsync()
    }

    override suspend fun delete(element: OAuthGroup): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                database.context.deleteFrom(OAUTH2_GROUPS).where(OAUTH2_GROUPS.GROUP_NAME.eq(element.name)).execute()
                return@withContext true
            } catch (e: DataAccessException) {
                return@withContext false
            }
        }
    }

    companion object {
        fun mapRecordToGroup(record: Oauth2GroupsRecord): OAuthGroup {
            return OAuthGroup(
                scopes = record.scopes?.split(";") ?: emptyList(),
                name = record.groupName!!,
            )
        }
    }
}