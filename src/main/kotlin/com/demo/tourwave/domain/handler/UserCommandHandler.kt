package com.demo.tourwave.domain.handler

import com.demo.tourwave.domain.User

interface UserCommandHandler {
    fun registerUser(name: String, email: String): User
}