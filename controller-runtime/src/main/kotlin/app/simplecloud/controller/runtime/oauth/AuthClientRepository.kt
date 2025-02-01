package app.simplecloud.controller.runtime.oauth

import app.simplecloud.controller.runtime.Repository
import app.simplecloud.controller.runtime.database.Database
import app.simplecloud.controller.shared.db.tables.records.Oauth2ClientDetailsRecord
import app.simplecloud.controller.shared.db.tables.references.OAUTH2_CLIENT_DETAILS
import app.simplecloud.droplet.api.auth.OAuthClient
import app.simplecloud.droplet.api.auth.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withContext
import org.jooq.exception.DataAccessException

class AuthClientRepository(
    private val database: Database
) : Repository<OAuthClient, String> {

    override suspend fun getAll(): List<OAuthClient> {
        return database.context.selectFrom(OAUTH2_CLIENT_DETAILS)
            .asFlow()
            .toCollection(mutableListOf())
            .map { mapRecordToClient(it) }
    }

    override suspend fun find(identifier: String): OAuthClient? {
        return database.context.selectFrom(OAUTH2_CLIENT_DETAILS)
            .where(OAUTH2_CLIENT_DETAILS.CLIENT_ID.eq(identifier))
            .limit(1)
            .awaitFirstOrNull()?.let { mapRecordToClient(it) }
    }

    override fun save(element: OAuthClient) {
        database.context.insertInto(
            OAUTH2_CLIENT_DETAILS,

            OAUTH2_CLIENT_DETAILS.CLIENT_ID,
            OAUTH2_CLIENT_DETAILS.CLIENT_SECRET,
            OAUTH2_CLIENT_DETAILS.GRANT_TYPES,
            OAUTH2_CLIENT_DETAILS.REDIRECT_URI,
            OAUTH2_CLIENT_DETAILS.SCOPE,
        ).values(
            element.clientId,
            element.clientSecret,
            element.grantTypes,
            element.redirectUri,
            element.scope.joinToString(";"),
        ).onDuplicateKeyUpdate()
            .set(OAUTH2_CLIENT_DETAILS.CLIENT_ID, element.clientId)
            .set(OAUTH2_CLIENT_DETAILS.CLIENT_SECRET, element.clientSecret)
            .set(OAUTH2_CLIENT_DETAILS.GRANT_TYPES, element.grantTypes)
            .set(OAUTH2_CLIENT_DETAILS.REDIRECT_URI, element.redirectUri)
            .set(OAUTH2_CLIENT_DETAILS.SCOPE, element.scope.joinToString(";"))
            .executeAsync()
    }

    override suspend fun delete(element: OAuthClient): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                database.context.deleteFrom(OAUTH2_CLIENT_DETAILS)
                    .where(OAUTH2_CLIENT_DETAILS.CLIENT_ID.eq(element.clientId))
                    .execute()
                return@withContext true
            } catch (e: DataAccessException) {
                return@withContext false
            }
        }
    }

    private fun mapRecordToClient(record: Oauth2ClientDetailsRecord): OAuthClient {
        return OAuthClient(
            clientId = record.clientId!!,
            clientSecret = record.clientSecret!!,
            grantTypes = record.grantTypes!!,
            redirectUri = record.redirectUri,
            scope = Scope.fromString(record.scope ?: "", ";"),
        )
    }
}