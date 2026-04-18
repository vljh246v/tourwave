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
    val emailVerifiedAt: Instant? = null,
    val deletedAt: Instant? = null
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

        private val ALLOWED_TRANSITIONS: Map<UserStatus, Set<UserStatus>> = mapOf(
            UserStatus.ACTIVE to setOf(UserStatus.DEACTIVATED, UserStatus.SUSPENDED, UserStatus.DELETED),
            UserStatus.DEACTIVATED to setOf(UserStatus.ACTIVE, UserStatus.DELETED),
            UserStatus.SUSPENDED to setOf(UserStatus.ACTIVE, UserStatus.DELETED),
            UserStatus.DELETED to emptySet()
        )
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

    fun transition(toStatus: UserStatus, now: Instant): User {
        val allowed = ALLOWED_TRANSITIONS[status] ?: emptySet()
        require(toStatus in allowed) {
            "$status 상태에서 $toStatus 으로 전이할 수 없습니다"
        }
        return copy(status = toStatus, updatedAt = now)
    }

    fun suspend(now: Instant): User = transition(UserStatus.SUSPENDED, now)

    fun delete(now: Instant): User {
        val allowed = ALLOWED_TRANSITIONS[status] ?: emptySet()
        require(UserStatus.DELETED in allowed) {
            "$status 상태에서 DELETED 으로 전이할 수 없습니다"
        }
        val userId = id ?: 0L
        return copy(
            status = UserStatus.DELETED,
            displayName = "Deleted User #$userId",
            email = "deleted_${userId}@deleted.local",
            passwordHash = "[DELETED]",
            deletedAt = now,
            updatedAt = now
        )
    }

    fun restore(now: Instant): User = transition(UserStatus.ACTIVE, now)
}
