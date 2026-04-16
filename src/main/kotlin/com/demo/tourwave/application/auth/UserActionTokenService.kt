package com.demo.tourwave.application.auth

import com.demo.tourwave.application.auth.port.UserActionTokenRepository
import com.demo.tourwave.domain.auth.UserActionToken
import com.demo.tourwave.domain.auth.UserActionTokenPurpose
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.util.Base64

class UserActionTokenService(
    private val userActionTokenRepository: UserActionTokenRepository,
    private val actionTokenGenerator: ActionTokenGenerator,
    private val clock: Clock,
) {
    private val base64Encoder = Base64.getUrlEncoder().withoutPadding()

    fun issue(
        userId: Long,
        purpose: UserActionTokenPurpose,
        ttl: Duration,
    ): String {
        val now = clock.instant()
        userActionTokenRepository.findActiveByUserIdAndPurpose(userId, purpose, now)
            .forEach { userActionTokenRepository.save(it.consume(now)) }

        val rawToken = actionTokenGenerator.generate()
        userActionTokenRepository.save(
            UserActionToken(
                userId = userId,
                tokenHash = hash(rawToken),
                purpose = purpose,
                expiresAtUtc = now.plus(ttl),
                createdAtUtc = now,
            ),
        )
        return rawToken
    }

    fun consume(
        rawToken: String,
        purpose: UserActionTokenPurpose,
    ): UserActionToken {
        val now = clock.instant()
        val token = requireActive(rawToken, purpose, now)
        val consumed = token.consume(now)
        userActionTokenRepository.save(consumed)
        return consumed
    }

    fun hash(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(rawToken.trim().toByteArray())
        return base64Encoder.encodeToString(digest)
    }

    private fun requireActive(
        rawToken: String,
        purpose: UserActionTokenPurpose,
        now: java.time.Instant,
    ): UserActionToken {
        val normalized = rawToken.trim()
        if (normalized.isBlank()) {
            throw invalidToken()
        }
        val token =
            userActionTokenRepository.findByTokenHash(hash(normalized))
                ?: throw invalidToken()
        if (token.purpose != purpose || !token.isActive(now)) {
            throw invalidToken()
        }
        return token
    }

    private fun invalidToken(): DomainException {
        return DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 400,
            message = "action token is invalid",
        )
    }
}
