package com.demo.tourwave.domain.port

import com.demo.tourwave.domain.User

interface UserQueryPort {
    fun getUserByEmail(email: String): User?
}