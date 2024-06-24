package com.demo.tourwave.domain.service

import org.springframework.stereotype.Service
import com.demo.tourwave.domain.User
import com.demo.tourwave.domain.handler.UserCommandHandler
import com.demo.tourwave.domain.port.UserQueryPort

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