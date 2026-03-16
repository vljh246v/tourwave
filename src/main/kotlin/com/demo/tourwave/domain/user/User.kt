package com.demo.tourwave.domain.user

data class User(
    val id: Long? = null,
    val name: String,
    val email: String
) {
    companion object {
        fun create(name: String, email: String): User {
            return User(
                id = null,
                name = name,
                email = email
            )
        }
    }

    fun persisted(userId: Long): User {
        return copy(id = userId)
    }
}
