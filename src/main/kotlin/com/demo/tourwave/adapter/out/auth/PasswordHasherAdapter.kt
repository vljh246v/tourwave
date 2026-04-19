package com.demo.tourwave.adapter.out.auth

import com.demo.tourwave.application.auth.port.PasswordHasher
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class PasswordHasherAdapter : PasswordHasher {
    private val encoder = BCryptPasswordEncoder()

    override fun hash(rawPassword: String): String = encoder.encode(rawPassword)

    override fun matches(
        rawPassword: String,
        passwordHash: String,
    ): Boolean {
        return encoder.matches(rawPassword, passwordHash)
    }
}
