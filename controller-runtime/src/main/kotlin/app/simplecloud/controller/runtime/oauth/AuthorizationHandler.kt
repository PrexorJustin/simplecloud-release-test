package app.simplecloud.controller.runtime.oauth

import app.simplecloud.droplet.api.auth.JwtHandler
import app.simplecloud.droplet.api.auth.OAuthClient
import app.simplecloud.droplet.api.auth.OAuthToken
import app.simplecloud.droplet.api.auth.Scope
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

class AuthorizationHandler(
    private val secret: String,
    private val clientRepository: AuthClientRepository,
    private val tokenRepository: AuthTokenRepository,
    private val pkceHandler: PKCEHandler,
    private val jwtHandler: JwtHandler,
    private val flowData: MutableMap<String, List<String>>
) {
    suspend fun registerClient(call: RoutingCall) {
        val params = call.receiveParameters()
        val providedMasterToken = params["master_token"]
        if (providedMasterToken != secret) {
            call.respond(HttpStatusCode.Forbidden, "Invalid master token")
            return
        }
        val clientId = params["client_id"]
        if (clientId == null) {
            call.respond(HttpStatusCode.BadRequest, "Client id is required")
            return
        }
        val redirectUri = params["redirect_uri"]
        val grantTypes = params["grant_types"]
        if (grantTypes == null) {
            call.respond(HttpStatusCode.BadRequest, "Invalid grant_types")
            return
        }
        val scope = Scope.fromString(params["scope"] ?: "")
        val providedSecret = params["client_secret"]
        val clientSecret = providedSecret ?: "secret-${UUID.randomUUID().toString().replace("-", "")}"
        val client = OAuthClient(clientId, clientSecret, redirectUri, grantTypes, scope)
        clientRepository.save(client)
        call.respond(
            mapOf(
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "scope" to client.scope.joinToString(" "),
                "grant_types" to client.grantTypes,
                "redirect_uri" to client.redirectUri,
            )
        )
    }

    suspend fun getClient(call: RoutingCall) {
        val params = call.receiveParameters()
        val clientId = params["client_id"]
        if (clientId == null) {
            call.respond(HttpStatusCode.BadRequest, "You must provide a valid client_id")
            return
        }
        val masterToken = params["master_token"]
        val clientSecret = params["client_secret"]
        if (masterToken == null && clientSecret == null) {
            call.respond(HttpStatusCode.BadRequest, "You must provide either a valid master_token or client_secret")
            return
        }
        if (masterToken != null && secret != masterToken) {
            call.respond(HttpStatusCode.BadRequest, "You must provide either a valid master_token or client_secret")
            return
        }
        val client = clientRepository.find(clientId)
        if (client == null) {
            call.respond(HttpStatusCode.BadRequest, "You must provide a valid client_id")
            return
        }
        if (masterToken == null && client.clientSecret != clientSecret) {
            call.respond(HttpStatusCode.BadRequest, "You must provide either a valid master_token or client_secret")
            return
        }
        call.respond(
            mapOf(
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "scope" to client.scope.joinToString(" "),
                "grant_types" to client.grantTypes,
                "redirect_uri" to client.redirectUri,
            )
        )
    }

    suspend fun authorizeRequest(call: RoutingCall) {
        val params = call.receiveParameters()
        val clientId = params["client_id"]
        val redirectUri = params["redirect_uri"]
        val challengeMethod = params["code_challenge_method"]
        val challenge = params["code_challenge"]
        val scope = params["scope"]
        if (clientId == null || redirectUri == null || scope == null || challenge == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                "You have to provide redirect_uri, client_id, scope and challenge"
            )
            return
        }
        if (challengeMethod == null || challengeMethod != "S256") {
            call.respond(HttpStatusCode.BadRequest, "Invalid challenge, S256 is supported.")
            return
        }
        val client = clientRepository.find(clientId)
        if (client == null) {
            call.respond(HttpStatusCode.NotFound, "Client not found")
            return
        }
        if (!client.grantTypes.contains("authorization_code")) {
            call.respond(HttpStatusCode.BadRequest, "User authorization is not supported by the client")
            return
        }

        if (!client.grantTypes.contains("pkce")) {
            call.respond(
                HttpStatusCode.BadRequest,
                "User authorization using PKCE is not supported by the client"
            )
            return
        }

        if (!client.scope.contains(scope)) {
            call.respond(HttpStatusCode.BadRequest, "This scope is not supported by the client")
            return
        }

        val authorizationCode = UUID.randomUUID().toString().replace("-", "")
        flowData[authorizationCode] = listOf(client.clientId, challenge, scope)
        call.respond(mapOf("redirectUri" to "$redirectUri?code=$authorizationCode"))
    }

    suspend fun tokenRequest(call: RoutingCall) {
        val params = call.receiveParameters()
        val clientId = params["client_id"]
        val clientSecret = params["client_secret"]
        val code = params["code"]
        val codeVerifier = params["code_verifier"]

        if (clientId == null) {
            call.respond(HttpStatusCode.BadRequest, "You have to provide a client id")
            return
        }

        if (clientSecret == null) {
            call.respond(HttpStatusCode.BadRequest, "You have to provide a client secret")
            return
        }

        val client = clientRepository.find(clientId)
        if (client == null) {
            call.respond(HttpStatusCode.BadRequest, "You have to provide a valid client id")
            return
        }

        if (client.clientSecret != clientSecret) {
            call.respond(HttpStatusCode.BadRequest, "Invalid client secret")
            return
        }

        if (client.grantTypes.contains("authorization_code") && client.grantTypes.contains("pkce")) {
            if (codeVerifier == null || code == null) {
                call.respond(HttpStatusCode.BadRequest, "You have to provide a code and a code verifier")
                return
            }
            val reconstructedChallenge = pkceHandler.generateCodeChallenge(codeVerifier)
            val originalChallenge = flowData[code]?.get(1)
            //If we can reconstruct the challenge, the authorization context was made in a secure context
            if (originalChallenge == reconstructedChallenge) {
                val token = OAuthToken(
                    id = UUID.randomUUID().toString(),
                    clientId = clientId,
                    accessToken = jwtHandler.generateJwtSigned(
                        clientId,
                        expiresIn = 3600,
                        scope = flowData[code]?.get(2)!!
                    ),
                    expiresIn = 3600,
                    scope = flowData[code]?.get(2)!!
                )
                tokenRepository.save(token)
                call.respond(
                    mapOf(
                        "access_token" to token.accessToken,
                        "scope" to token.scope,
                        "exp" to (token.expiresIn ?: -1),
                        "user_id" to token.userId,
                        "client_id" to token.clientId
                    )
                )
                return
            }
            call.respond(
                HttpStatusCode.BadRequest,
                "The token request was not made in the same context as the authorization."
            )
            return
        } else if (client.grantTypes.contains("client_credentials")) {
            val scope = client.scope.ifEmpty { listOf("*") }
            val token = OAuthToken(
                id = UUID.randomUUID().toString(),
                clientId = clientId,
                accessToken = jwtHandler.generateJwtSigned(clientId, scope = scope.joinToString(" ")),
                scope = scope.joinToString(" ")
            )
            tokenRepository.save(token)
            call.respond(
                mapOf(
                    "access_token" to token.accessToken,
                    "scope" to token.scope,
                    "exp" to (token.expiresIn ?: -1),
                )
            )
            return
        } else {
            call.respond(HttpStatusCode.BadRequest, "Invalid client")
            return
        }
    }

    suspend fun revokeRequest(call: RoutingCall) {
        val params = call.receiveParameters()
        val accessToken = params["access_token"]
        if (accessToken == null) {
            call.respond(HttpStatusCode.BadRequest, "Access token is invalid")
            return
        }
        val token = tokenRepository.findByAccessToken(accessToken)
        if (token == null) {
            call.respond(HttpStatusCode.BadRequest, "Access token is invalid")
            return
        }

        if (tokenRepository.delete(token)) {
            call.respond("Access token revoked")
            return
        }

        call.respond(HttpStatusCode.InternalServerError, "Could not delete token")

    }

    suspend fun introspectRequest(call: RoutingCall) {
        val params = call.receiveParameters()
        val token = params["token"]
        if (token == null) {
            call.respond(HttpStatusCode.BadRequest, "Token is missing")
            return
        }
        if (token == secret) {
            call.respond(
                mapOf(
                    "active" to true,
                    "scope" to "*",
                    "exp" to -1,
                ),
            )
            return
        }
        val authToken = tokenRepository.findByAccessToken(token)
        if (authToken == null) {
            call.respond(HttpStatusCode.OK, mapOf("active" to false))
            return
        }

        val active = ((authToken.expiresIn ?: 1) > 0) && jwtHandler.verifyJwt(token)
        if (!active) {
            tokenRepository.delete(authToken)
            call.respond(mapOf("active" to false))
        }

        // If the token exists, respond with token details
        call.respond(
            mapOf(
                "active" to true,
                "token_id" to authToken.id,
                "client_id" to authToken.clientId,
                "scope" to authToken.scope,
                "exp" to (authToken.expiresIn ?: -1),
            )
        )
    }


}