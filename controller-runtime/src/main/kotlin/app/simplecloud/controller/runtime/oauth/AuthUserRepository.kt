package app.simplecloud.controller.runtime.oauth

import app.simplecloud.controller.runtime.Repository
import app.simplecloud.controller.runtime.database.Database
import app.simplecloud.controller.shared.db.tables.records.Oauth2UsersRecord
import app.simplecloud.controller.shared.db.tables.references.OAUTH2_TOKENS
import app.simplecloud.controller.shared.db.tables.references.OAUTH2_USERS
import app.simplecloud.controller.shared.db.tables.references.OAUTH2_USER_GROUPS
import app.simplecloud.droplet.api.auth.OAuthGroup
import app.simplecloud.droplet.api.auth.OAuthToken
import app.simplecloud.droplet.api.auth.OAuthUser
import app.simplecloud.droplet.api.auth.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withContext
import org.jooq.exception.DataAccessException

class AuthUserRepository(
    private val database: Database
) : Repository<OAuthUser, String> {

    override suspend fun getAll(): List<OAuthUser> {
        return database.context.selectFrom(
            OAUTH2_USERS
        )
            .asFlow().toCollection(mutableListOf()).map { mapRecordToUser(it) }
    }

    override suspend fun find(identifier: String): OAuthUser? {
        return database.context.selectFrom(
            OAUTH2_USERS
        )
            .where(OAUTH2_USERS.USER_ID.eq(identifier))
            .limit(1)
            .awaitFirstOrNull()?.let { mapRecordToUser(it) }
    }

    suspend fun findByName(identifier: String): OAuthUser? {
        return database.context.selectFrom(
            OAUTH2_USERS
        )
            .where(OAUTH2_USERS.USERNAME.eq(identifier))
            .limit(1)
            .awaitFirstOrNull()?.let { mapRecordToUser(it) }
    }


    override fun save(element: OAuthUser) {
        database.context.insertInto(
            OAUTH2_USERS,

            OAUTH2_USERS.USER_ID,
            OAUTH2_USERS.SCOPES,
            OAUTH2_USERS.USERNAME,
            OAUTH2_USERS.HASHED_PASSWORD,
        ).values(
            element.userId,
            element.scopes.joinToString(";"),
            element.username,
            element.hashedPassword,
        ).onDuplicateKeyUpdate()
            .set(OAUTH2_USERS.USER_ID, element.userId)
            .set(OAUTH2_USERS.SCOPES, element.scopes.joinToString(";"))
            .set(OAUTH2_USERS.USERNAME, element.username)
            .set(OAUTH2_USERS.HASHED_PASSWORD, element.hashedPassword)
            .executeAsync()
        database.context.deleteFrom(OAUTH2_USER_GROUPS).where(OAUTH2_USER_GROUPS.USER_ID.eq(element.userId))
            .executeAsync()
        element.groups.forEach {
            database.context.insertInto(
                OAUTH2_USER_GROUPS,

                OAUTH2_USER_GROUPS.USER_ID,
                OAUTH2_USER_GROUPS.GROUP_NAME,
            ).values(
                element.userId,
                it.name,
            ).onConflictDoNothing().executeAsync()
        }
    }

    override suspend fun delete(element: OAuthUser): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                database.context.deleteFrom(OAUTH2_USERS)
                    .where(OAUTH2_USERS.USER_ID.eq(element.userId))
                    .execute()
                return@withContext true
            } catch (e: DataAccessException) {
                return@withContext false
            }
        }
    }

    private suspend fun mapRecordToUser(
        record: Oauth2UsersRecord,
    ): OAuthUser {
        val token = getToken(record.userId!!)
        val groups = getGroups(record.userId!!)
        return OAuthUser(
            scopes = Scope.fromString(record.scopes ?: ";"),
            userId = record.userId!!,
            username = record.username!!,
            hashedPassword = record.hashedPassword!!,
            token = token,
            groups = groups
        )
    }

    private suspend fun getToken(userId: String): OAuthToken? {
        return database.context.selectFrom(OAUTH2_TOKENS).where(OAUTH2_TOKENS.USER_ID.eq(userId)).limit(1)
            .awaitFirstOrNull()
            ?.let { AuthTokenRepository.mapRecordToToken(it) }
    }

    private suspend fun getGroups(userId: String): List<OAuthGroup> {
        return database.context.select(OAUTH2_USER_GROUPS, OAUTH2_USER_GROUPS.oauth2Groups()).from(OAUTH2_USER_GROUPS)
            .where(OAUTH2_USER_GROUPS.USER_ID.eq(userId))
            .asFlow().toCollection(mutableListOf()).map {
                if (it != null) {
                    return@map AuthGroupRepository.mapRecordToGroup(it.component2())
                }
                return@map null
            }.filterNotNull()
    }
}