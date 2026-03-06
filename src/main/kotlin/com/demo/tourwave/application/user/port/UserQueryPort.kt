package com.demo.tourwave.application.user.port

import com.demo.tourwave.domain.user.User

interface UserQueryPort {
    fun findByEmail(email: String): User?
}
