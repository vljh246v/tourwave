package com.demo.tourwave.domain.auth

import java.time.Instant

data class AuthRefreshToken(
    val id: Long? = null,
    val userId: Long,
    val tokenHash: String,
    val expiresAtUtc: Instant,
    val issuedAtUtc: Instant,
    val revokedAtUtc: Instant? = null
) {
    fun isActive(now: Instant): Boolean {
        return revokedAtUtc == null && expiresAtUtc.isAfter(now)
    }

    fun revoke(now: Instant): AuthRefreshToken = copy(revokedAtUtc = now)
}
