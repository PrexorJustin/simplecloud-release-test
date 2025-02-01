package app.simplecloud.controller.runtime.oauth

import app.simplecloud.controller.runtime.Repository
import app.simplecloud.controller.runtime.database.Database
import app.simplecloud.controller.shared.db.tables.records.Oauth2TokensRecord
import app.simplecloud.controller.shared.db.tables.references.OAUTH2_TOKENS
import app.simplecloud.droplet.api.auth.OAuthToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withContext
import org.jooq.exception.DataAccessException
import java.time.Duration
import java.time.LocalDateTime

class AuthTokenRepository(private val database: Database) : Repository<OAuthToken, String> {
    override suspend fun getAll(): List<OAuthToken> {
        return database.context.selectFrom(OAUTH2_TOKENS)
            .asFlow()
            .toCollection(mutableListOf())
            .map { mapRecordToToken(it) }
    }

    override suspend fun find(identifier: String): OAuthToken? {
        return database.context.selectFrom(OAUTH2_TOKENS)
            .where(OAUTH2_TOKENS.TOKEN_ID.eq(identifier))
            .limit(1)
            .awaitFirstOrNull()?.let { mapRecordToToken(it) }
    }

    suspend fun findByAccessToken(token: String): OAuthToken? {
        return database.context.selectFrom(OAUTH2_TOKENS)
            .where(OAUTH2_TOKENS.ACCESS_TOKEN.eq(token))
            .limit(1)
            .awaitFirstOrNull()?.let { mapRecordToToken(it) }
    }

    suspend fun findByUserId(userId: String): OAuthToken? {
        return database.context.selectFrom(OAUTH2_TOKENS)
            .where(OAUTH2_TOKENS.USER_ID.eq(userId))
            .limit(1)
            .awaitFirstOrNull()?.let { mapRecordToToken(it) }
    }

    override fun save(element: OAuthToken) {
        database.context.insertInto(
            OAUTH2_TOKENS,

            OAUTH2_TOKENS.TOKEN_ID,
            OAUTH2_TOKENS.ACCESS_TOKEN,
            OAUTH2_TOKENS.SCOPE,
            OAUTH2_TOKENS.CLIENT_ID,
            OAUTH2_TOKENS.EXPIRES_IN,
            OAUTH2_TOKENS.USER_ID,
        ).values(
            element.id,
            element.accessToken,
            element.scope,
            element.clientId,
            if (element.expiresIn != null) LocalDateTime.now().plusSeconds(element.expiresIn!!.toLong()) else null,
            element.userId,
        ).onDuplicateKeyUpdate()
            .set(OAUTH2_TOKENS.TOKEN_ID, element.id)
            .set(OAUTH2_TOKENS.ACCESS_TOKEN, element.accessToken)
            .set(OAUTH2_TOKENS.SCOPE, element.scope)
            .set(OAUTH2_TOKENS.CLIENT_ID, element.clientId)
            .set(
                OAUTH2_TOKENS.EXPIRES_IN,
                if (element.expiresIn != null) LocalDateTime.now().plusSeconds(element.expiresIn!!.toLong()) else null
            )
            .set(OAUTH2_TOKENS.USER_ID, element.userId)
            .executeAsync()
    }

    override suspend fun delete(element: OAuthToken): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                database.context.deleteFrom(OAUTH2_TOKENS)
                    .where(OAUTH2_TOKENS.CLIENT_ID.eq(element.clientId))
                    .execute()
                return@withContext true
            } catch (e: DataAccessException) {
                return@withContext false
            }
        }
    }

    companion object {
        fun mapRecordToToken(record: Oauth2TokensRecord): OAuthToken {
            return OAuthToken(
                id = record.tokenId!!,
                scope = record.scope ?: "",
                expiresIn = if (record.expiresIn != null) Duration.between(LocalDateTime.now(), record.expiresIn!!)
                    .toSeconds().toInt() else null,
                accessToken = record.accessToken!!,
                clientId = record.clientId,
                userId = record.userId,
            )
        }
    }
}