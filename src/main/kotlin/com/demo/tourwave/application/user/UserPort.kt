package com.demo.tourwave.application.user

import com.demo.tourwave.domain.user.User

interface UserPort {
    fun findById(userId: Long): User?
    fun findByEmail(email: String): User?
    fun save(user: User): User
    fun deleteById(userId: Long)
}
