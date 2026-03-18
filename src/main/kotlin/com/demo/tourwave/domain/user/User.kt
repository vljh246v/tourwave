package com.demo.tourwave.domain.user

import java.time.Instant

enum class UserStatus {
    ACTIVE,
    DEACTIVATED,
    SUSPENDED,
    DELETED
}

data class User(
    val id: Long? = null,
    val displayName: String,
    val email: String,
    val passwordHash: String,
    val status: UserStatus = UserStatus.ACTIVE,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = createdAt,
    val emailVerifiedAt: Instant? = null
) {
    companion object {
        fun create(
            displayName: String,
            email: String,
            passwordHash: String,
            now: Instant = Instant.now()
        ): User {
            return User(
                id = null,
                displayName = displayName,
                email = email,
                passwordHash = passwordHash,
                status = UserStatus.ACTIVE,
                createdAt = now,
                updatedAt = now
            )
        }
    }

    fun persisted(userId: Long): User {
        return copy(id = userId)
    }

    fun updateProfile(
        displayName: String,
        now: Instant
    ): User {
        return copy(
            displayName = displayName,
            updatedAt = now
        )
    }

    fun verifyEmail(now: Instant): User {
        if (emailVerifiedAt != null) {
            return this
        }
        return copy(
            emailVerifiedAt = now,
            updatedAt = now
        )
    }

    fun updatePassword(passwordHash: String, now: Instant): User {
        return copy(
            passwordHash = passwordHash,
            updatedAt = now
        )
    }

    fun deactivate(now: Instant): User {
        if (status == UserStatus.DEACTIVATED) {
            return this
        }
        return copy(
            status = UserStatus.DEACTIVATED,
            updatedAt = now
        )
    }
}
