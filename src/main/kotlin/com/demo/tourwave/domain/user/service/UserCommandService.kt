package com.demo.tourwave.domain.user.service

import org.springframework.stereotype.Service
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.domain.user.handler.UserCommandHandler
import com.demo.tourwave.domain.user.port.UserQueryPort

@Service
class UserCommandService(
    private val userQueryPort: UserQueryPort
): UserCommandHandler {
    override fun registerUser(name: String, email: String): User {
        if (userQueryPort.getUserByEmail(email) != null) {
            throw IllegalArgumentException("User with email $email already exists")
        }
        return User.create(name, email)
    }
}