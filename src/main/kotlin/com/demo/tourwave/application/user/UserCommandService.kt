package com.demo.tourwave.application.user

import com.demo.tourwave.application.user.port.UserQueryPort
import com.demo.tourwave.domain.user.User

class UserCommandService(
    private val userQueryPort: UserQueryPort
): UserCommandHandler {
    override fun registerUser(name: String, email: String): User {
        if (userQueryPort.findByEmail(email) != null) {
            throw IllegalArgumentException("User with email $email already exists")
        }
        return User.create(name, email)
    }
}
