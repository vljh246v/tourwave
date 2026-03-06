package com.demo.tourwave.application.user

import com.demo.tourwave.domain.user.User

interface UserCommandHandler {
    fun registerUser(name: String, email: String): User
}
