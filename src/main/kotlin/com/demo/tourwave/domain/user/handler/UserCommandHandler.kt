package com.demo.tourwave.domain.user.handler

import com.demo.tourwave.domain.user.User

interface UserCommandHandler {
    fun registerUser(name: String, email: String): User
}