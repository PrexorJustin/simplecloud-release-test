package app.simplecloud.controller.runtime.oauth

import app.simplecloud.controller.runtime.database.Database
import app.simplecloud.controller.runtime.launcher.ControllerStartCommand
import app.simplecloud.droplet.api.auth.JwtHandler
import app.simplecloud.droplet.api.auth.OAuthIntrospector
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*

class OAuthServer(private val args: ControllerStartCommand, database: Database) {
    private val issuer = "http://${args.grpcHost}:${args.authorizationPort}"
    private val secret = args.authSecret
    private val jwtHandler = JwtHandler(secret, issuer)
    private val pkceHandler = PKCEHandler()
    private val clientRepository = AuthClientRepository(database)
    private val groupRepository = AuthGroupRepository(database)
    private val userRepository = AuthUserRepository(database)
    private val tokenRepository = AuthTokenRepository(database)

    //code to client_id, code_challenge and scope (this is in memory because it is only in use temporary)
    private val flowData = mutableMapOf<String, List<String>>()

    private val authorizationHandler =
        AuthorizationHandler(secret, clientRepository, tokenRepository, pkceHandler, jwtHandler, flowData)

    private val authenticationHandler =
        AuthenticationHandler(groupRepository, userRepository, tokenRepository, jwtHandler)

    private val introspector = OAuthIntrospector(issuer)


    fun start() {
        embeddedServer(Netty, host = args.grpcHost, port = args.authorizationPort) {
            install(ContentNegotiation) {
                jackson {
                    enable(SerializationFeature.INDENT_OUTPUT)
                }
            }

            install(Authentication) {
                bearer {
                    authenticate { credential -> introspector.introspect(credential.token) }
                }
            }

            routing {

                // AUTHORIZATION

                // Client registration endpoint
                post("/oauth/register_client") {
                    authorizationHandler.registerClient(call)
                }

                // Client retrieval endpoint
                get("/oauth/client") {
                    authorizationHandler.getClient(call)
                }
                // Authorization endpoint (simulating authorization code flow)
                post("/oauth/authorize") {
                    authorizationHandler.authorizeRequest(call)
                }
                // Token endpoint
                post("/oauth/token") {
                    authorizationHandler.tokenRequest(call)
                }
                // Revocation endpoint
                post("/oauth/revoke") {
                    authorizationHandler.revokeRequest(call)
                }
                // Introspection endpoint
                post("/oauth/introspect") {
                    authorizationHandler.introspectRequest(call)
                }

                // AUTHENTICATION

                authenticate {
                    // Save group endpoint
                    put("/group") {
                        authenticationHandler.saveGroup(call)
                    }
                    // Get group endpoint
                    get("/group") {
                        authenticationHandler.getGroup(call)
                    }
                    // Delete group endpoint
                    delete("/group") {
                        authenticationHandler.deleteGroup(call)
                    }
                    // Get all groups endpoint
                    get("/groups") {
                        authenticationHandler.getGroups(call)
                    }

                    put("/user") {
                        authenticationHandler.saveUser(call)
                    }

                    get("/user") {
                        authenticationHandler.getUser(call)
                    }

                    get("/users") {
                        authenticationHandler.getUsers(call)
                    }

                    delete("/user") {
                        authenticationHandler.deleteUser(call)
                    }

                    //Login endpoint
                    post("/login") {
                        authenticationHandler.login(call)
                    }
                }
            }
        }.start(wait = true)
    }
}