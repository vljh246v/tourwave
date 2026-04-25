package com.demo.tourwave.domain.auth

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

class AuthRefreshTokenTest {
    @Test
    fun `new token has version 0`() {
        val token = AuthRefreshToken(
            id = 1L,
            userId = 100L,
            tokenHash = "hash123",
            expiresAtUtc = Instant.parse("2026-05-01T00:00:00Z"),
            issuedAtUtc = Instant.parse("2026-04-26T00:00:00Z"),
        )
        assertEquals(0L, token.version)
    }

    @Test
    fun `revoke preserves version`() {
        val token = AuthRefreshToken(
            id = 1L,
            userId = 100L,
            tokenHash = "hash123",
            expiresAtUtc = Instant.parse("2026-05-01T00:00:00Z"),
            issuedAtUtc = Instant.parse("2026-04-26T00:00:00Z"),
            version = 3L,
        )
        val revoked = token.copy(revokedAtUtc = Instant.parse("2026-04-26T12:00:00Z"))
        assertEquals(3L, revoked.version)
    }
}
