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

    override fun suspendUser(
        userId: Long,
        reason: String,
    ) {
        val user =
            userRepository.findById(userId)
                ?: throw NoSuchElementException("User $userId not found")
        userRepository.save(user.suspend(clock.instant()))
    }

    override fun deleteUser(userId: Long) {
        val user =
            userRepository.findById(userId)
                ?: throw NoSuchElementException("User $userId not found")
        userRepository.save(user.delete(clock.instant()))
    }

    override fun restoreUser(userId: Long) {
        val user =
            userRepository.findById(userId)
                ?: throw NoSuchElementException("User $userId not found")
        userRepository.save(user.restore(clock.instant()))
    }
}
