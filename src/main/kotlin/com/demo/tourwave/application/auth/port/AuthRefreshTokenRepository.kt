package com.demo.tourwave.application.auth.port

import com.demo.tourwave.domain.auth.AuthRefreshToken
import java.time.Instant

interface AuthRefreshTokenRepository {
    fun save(token: AuthRefreshToken): AuthRefreshToken
    fun findByTokenHash(tokenHash: String): AuthRefreshToken?
    fun revokeAllByUserId(userId: Long, revokedAtUtc: Instant)
    fun clear()
}
