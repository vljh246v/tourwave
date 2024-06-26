package com.demo.tourwave.domain.user.port

import com.demo.tourwave.domain.user.User

interface UserQueryPort {
    fun getUserByEmail(email: String): User?
}