package com.demo.tourwave.application.user

import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.user.User
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Clock

class UserCommandService(
    private val userRepository: UserRepository,
    private val clock: Clock = Clock.systemUTC(),
) : UserCommandHandler {
    private val passwordEncoder = BCryptPasswordEncoder()

    override fun registerUser(
        name: String,
        email: String,
    ): User {
        if (userRepository.findByEmail(email) != null) {
            throw IllegalArgumentException("User with email $email already exists")
        }

        return userRepository.save(
            User.create(
                displayName = name,
                email = email,
                passwordHash = passwordEncoder.encode("temporary-password"),
                now = clock.instant(),
            ),
        )
    }
}
