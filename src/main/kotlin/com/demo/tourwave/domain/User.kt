package com.demo.tourwave.domain

class User(
    val name: String,
    val email: String
) {
    companion object {
        fun create(name: String, email: String): User {
            return User(
                name = name,
                email = email
            )
        }
    }
}