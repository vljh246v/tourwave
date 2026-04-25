package com.demo.tourwave.application.auth

import com.demo.tourwave.application.auth.port.AuthRefreshTokenRepository
import com.demo.tourwave.domain.auth.AuthRefreshToken
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import java.security.MessageDigest
import java.time.Clock
import java.util.Base64
import java.util.UUID

class AuthTokenLifecycleService(
    private val authRefreshTokenRepository: AuthRefreshTokenRepository,
    private val refreshTokenTtlSeconds: Long,
    private val clock: Clock,
) {
    private val base64Encoder = Base64.getUrlEncoder().withoutPadding()

    fun issueRefreshToken(userId: Long): String {
        val now = clock.instant()
        val refreshToken = "${UUID.randomUUID()}-${UUID.randomUUID()}"
        authRefreshTokenRepository.save(
            AuthRefreshToken(
                userId = userId,
                tokenHash = hash(refreshToken),
                issuedAtUtc = now,
                expiresAtUtc = now.plusSeconds(refreshTokenTtlSeconds),
            ),
        )
        return refreshToken
    }

    fun requireActiveRefreshToken(rawRefreshToken: String): AuthRefreshToken {
        val normalized = rawRefreshToken.trim()
        if (normalized.isBlank()) {
            throw unauthorized("refresh token is required")
        }
        val token =
            authRefreshTokenRepository.findByTokenHash(hash(normalized))
                ?: throw unauthorized("refresh token is invalid")
        if (!token.isActive(clock.instant())) {
            throw unauthorized("refresh token is expired")
        }
        return token
    }

    fun revokeAll(userId: Long) {
        authRefreshTokenRepository.revokeAllByUserId(userId = userId, revokedAtUtc = clock.instant())
    }

    fun rotate(rawRefreshToken: String): AuthRefreshToken {
        val token = requireActiveRefreshToken(rawRefreshToken)
        return authRefreshTokenRepository.rotate(token)
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return base64Encoder.encodeToString(digest)
    }

    private fun unauthorized(message: String): DomainException {
        return DomainException(
            errorCode = ErrorCode.UNAUTHORIZED,
            status = 401,
            message = message,
        )
    }
}
