package app.simplecloud.controller.runtime.oauth

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*

class PKCEHandler {
    fun generateCodeVerifier(): String {
        // Code verifier is a random string (e.g., 43-128 characters)
        return UUID.randomUUID().toString().replace("-", "")
    }

    fun generateCodeChallenge(codeVerifier: String): String {
        // SHA256 the code verifier and base64 encode it
        val digest = MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}