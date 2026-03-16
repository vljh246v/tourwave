package com.demo.tourwave.application.user.port

import com.demo.tourwave.domain.user.User

interface UserRepository {
    fun save(user: User): User
    fun findById(userId: Long): User?
    fun findByEmail(email: String): User?
    fun clear()
}
