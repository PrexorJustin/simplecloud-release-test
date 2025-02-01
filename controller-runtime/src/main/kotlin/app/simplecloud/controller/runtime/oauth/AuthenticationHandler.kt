package app.simplecloud.controller.runtime.oauth

import app.simplecloud.droplet.api.auth.*
import com.nimbusds.jwt.JWTClaimsSet
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

class AuthenticationHandler(
    private val groupRepository: AuthGroupRepository,
    private val userRepository: AuthUserRepository,
    private val tokenRepository: AuthTokenRepository,
    private val jwtHandler: JwtHandler,
) {

    private suspend fun checkScope(call: RoutingCall, scope: String): Boolean {
        val claims = call.receive<JWTClaimsSet>()
        val providedScope = Scope.fromString(claims.claims["scope"].toString())
        val requiredScope = Scope.fromString(scope)
        if (!Scope.validate(requiredScope, providedScope)) {
            call.respond(HttpStatusCode.Unauthorized)
            return false
        }
        return true
    }

    suspend fun saveGroup(call: RoutingCall) {
        val params = call.receiveParameters()
        val groupName = params["group_name"]
        if (groupName == null) {
            call.respond(HttpStatusCode.BadRequest, "You must specify a group name")
            return
        }
        if (!checkScope(call, "simplecloud.auth.group.save.$groupName")) {
            return
        }
        val scopes = Scope.fromString(params["scopes"] ?: "")
        if (!checkScope(call, scopes.joinToString(" "))) {
            return
        }
        groupRepository.save(OAuthGroup(scopes, groupName))
        call.respond("Group successfully saved")
    }

    suspend fun getGroup(call: RoutingCall) {
        val group = loadGroup(call) ?: return
        if (!checkScope(call, "simplecloud.auth.group.get.${group.name}")) {
            return
        }
        call.respond(mapOf("group_name" to group.name, "scope" to group.scopes.joinToString(" ")))
    }

    suspend fun getGroups(call: RoutingCall) {
        if (!checkScope(call, "simplecloud.auth.group.get.*")) {
            return
        }
        val groups = groupRepository.getAll()
        call.respond(listOf(groups.map {
            mapOf(
                "group_name" to it.name,
                "scope" to it.scopes.joinToString(" ")
            )
        }).flatten())
    }

    suspend fun deleteGroup(call: RoutingCall) {
        val group = loadGroup(call) ?: return
        if (!checkScope(call, "simplecloud.auth.group.delete.${group.name}")) {
            return
        }
        groupRepository.delete(group)
        call.respond("Group successfully deleted")
    }

    private suspend fun loadGroup(call: RoutingCall): OAuthGroup? {
        val params = call.receiveParameters()
        val groupName = params["group_name"]
        if (groupName == null) {
            call.respond(HttpStatusCode.BadRequest, "You must specify a group name")
            return null
        }
        val group = groupRepository.find(groupName)
        if (group == null) {
            call.respond(HttpStatusCode.NotFound, "Group not found")
            return null
        }
        return group
    }

    suspend fun saveUser(call: RoutingCall) {
        if (!checkScope(call, "simplecloud.auth.user.save")) {
            return
        }
        val params = call.receiveParameters()
        val username = params["username"]
        val password = params["password"]
        val groups = (params["groups"] ?: "").split(" ")
        val scope = Scope.fromString(params["scope"] ?: "")
        if (username == null || password == null) {
            call.respond(HttpStatusCode.BadRequest, "You must specify a username or password")
            return
        }
        val existing = userRepository.findByName(username)
        val parsedGroups = groups.mapNotNull { group -> groupRepository.find(group) }
        val updated = OAuthUser(
            userId = existing?.userId ?: UUID.randomUUID().toString(),
            groups = parsedGroups,
            username = username,
            scopes = scope,
            hashedPassword = PasswordEncoder.hashPassword(password)
        )
        userRepository.save(updated)
        call.respond("User successfully saved")
    }

    suspend fun getUser(call: RoutingCall) {
        val params = call.receiveParameters()
        val username = params["username"]
        if (username == null) {
            call.respond(HttpStatusCode.BadRequest, "You must specify a user id")
            return
        }
        if (!checkScope(call, "simplecloud.auth.user.get.$username")) {
            return
        }
        val user = userRepository.findByName(username)
        if (user == null) {
            call.respond(HttpStatusCode.NotFound, "User not found")
            return
        }
        call.respond(
            mapOf(
            "user_id" to user.userId,
            "username" to user.username,
            "scope" to user.scopes.joinToString(" "),
            "groups" to user.groups.joinToString(" ") { group -> group.name }
        ))
    }

    suspend fun getUsers(call: RoutingCall) {
        if (!checkScope(call, "simplecloud.auth.user.get.*")) {
            return
        }
        val users = userRepository.getAll()
        call.respond(listOf(users.map {
            mapOf(
                "user_id" to it.userId,
                "username" to it.username,
                "scope" to it.scopes.joinToString(" "),
                "groups" to it.groups.joinToString(" ") { group -> group.name }
            )
        }).flatten())
    }

    suspend fun deleteUser(call: RoutingCall) {
        if (!checkScope(call, "simplecloud.auth.user.delete")) {
            return
        }
        val params = call.receiveParameters()
        val userId = params["user_id"]
        if (userId == null) {
            call.respond(HttpStatusCode.BadRequest, "You must specify a user id")
            return
        }
        val user = userRepository.find(userId)
        if (user == null) {
            call.respond(HttpStatusCode.NotFound, "User not found")
            return
        }
        userRepository.delete(user)
        call.respond("User successfully deleted")
    }

    suspend fun login(call: RoutingCall) {
        val params = call.receiveParameters()
        val username = params["username"]
        val password = params["password"]
        if (username == null || password == null) {
            call.respond(HttpStatusCode.BadRequest, "You must specify a username and password")
            return
        }
        val user = userRepository.findByName(username)
        if (user == null) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid username or password")
            return
        }
        if (!PasswordEncoder.verifyPassword(password, user.hashedPassword)) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid username or password")
            return
        }
        val token = tokenRepository.findByUserId(user.userId)
        if (token?.expiresIn != null && token.expiresIn!! > 0) {
            call.respond(
                mapOf(
                    "access_token" to token.accessToken,
                    "scope" to token.scope,
                    "exp" to token.expiresIn,
                )
            )
            return
        }
        val combinedScopes = mutableListOf<String>()
        combinedScopes.addAll(user.scopes)
        user.groups.forEach {
            combinedScopes.addAll(it.scopes)
        }
        val jwtToken = jwtHandler.generateJwtSigned(
            user.userId,
            3600,
            Scope.fromString(combinedScopes.joinToString(" ")).joinToString(" ")
        )
        val newToken = OAuthToken(
            id = UUID.randomUUID().toString(),
            userId = user.userId,
            accessToken = jwtToken,
            expiresIn = 3600,
            scope = combinedScopes.joinToString(" ")
        )
        call.respond(
            mapOf(
                "access_token" to newToken.accessToken,
                "scope" to newToken.scope,
                "exp" to newToken.expiresIn,
                "user_id" to newToken.userId,
                "client_id" to newToken.clientId
            )
        )
    }
}