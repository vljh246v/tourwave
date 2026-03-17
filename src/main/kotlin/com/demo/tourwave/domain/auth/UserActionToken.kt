package com.demo.tourwave.domain.auth

import java.time.Instant

enum class UserActionTokenPurpose {
    EMAIL_VERIFICATION,
    PASSWORD_RESET
}

data class UserActionToken(
    val id: Long? = null,
    val userId: Long,
    val tokenHash: String,
    val purpose: UserActionTokenPurpose,
    val expiresAtUtc: Instant,
    val createdAtUtc: Instant,
    val consumedAtUtc: Instant? = null
)
