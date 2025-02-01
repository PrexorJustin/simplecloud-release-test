package app.simplecloud.controller.runtime.oauth

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

object PasswordEncoder {
    fun hashPassword(password: String): String {
        val passwordEncoder = BCryptPasswordEncoder()
        return passwordEncoder.encode(password)
    }

    fun verifyPassword(password: String, hashedPassword: String): Boolean {
        val passwordEncoder = BCryptPasswordEncoder()
        return passwordEncoder.matches(password, hashedPassword)
    }
}