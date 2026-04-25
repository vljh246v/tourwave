package com.demo.tourwave.application.auth

import com.demo.tourwave.support.FakeAuthRefreshTokenRepository
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AuthTokenLifecycleServiceTest {
    private val clock = Clock.fixed(Instant.parse("2026-04-26T00:00:00Z"), ZoneOffset.UTC)
    private val fakeRepository = FakeAuthRefreshTokenRepository()
    private val service =
        AuthTokenLifecycleService(
            authRefreshTokenRepository = fakeRepository,
            refreshTokenTtlSeconds = 3600,
            clock = clock,
        )

    @BeforeEach
    fun setup() {
        fakeRepository.clear()
    }

    @Test
    fun `rotate should revoke all other refresh tokens for the same user`() {
        val userId = 100L

        val tokenA = service.issueRefreshToken(userId)
        val tokenB = service.issueRefreshToken(userId)
        val tokenC = service.issueRefreshToken(userId)

        val allTokensBefore = fakeRepository.findAll()
        assertNotNull(allTokensBefore.find { it.tokenHash == hashToken(tokenA) })
        assertNotNull(allTokensBefore.find { it.tokenHash == hashToken(tokenB) })
        assertNotNull(allTokensBefore.find { it.tokenHash == hashToken(tokenC) })

        service.rotate(tokenA)

        val allTokensAfter = fakeRepository.findAll()
        val tokenBAfter = allTokensAfter.find { it.tokenHash == hashToken(tokenB) }
        val tokenCAfter = allTokensAfter.find { it.tokenHash == hashToken(tokenC) }

        assertNotNull(tokenBAfter)
        assertNotNull(tokenCAfter)
        assertFalse(tokenBAfter!!.isActive(clock.instant()), "tokenB should be revoked")
        assertFalse(tokenCAfter!!.isActive(clock.instant()), "tokenC should be revoked")
    }

    private fun hashToken(value: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
