package com.demo.tourwave.application.user

import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.user.User

class UserCommandService(
    private val userRepository: UserRepository
): UserCommandHandler {
    override fun registerUser(name: String, email: String): User {
        if (userRepository.findByEmail(email) != null) {
            throw IllegalArgumentException("User with email $email already exists")
        }

        return userRepository.save(
            User.create(
                name = name,
                email = email
            )
        )
    }
}
