package com.demo.tourwave.application.auth

import com.demo.tourwave.application.common.port.ActorRole
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class JwtTokenServiceTest {
    private val clock = Clock.fixed(Instant.parse("2026-03-17T00:00:00Z"), ZoneOffset.UTC)
    private val service = JwtTokenService(
        secret = "jwt-test-secret",
        accessTokenTtlSeconds = 3600,
        clock = clock,
        objectMapper = ObjectMapper()
    )

    @Test
    fun `issue and parse token round trips claims`() {
        val token = service.issueAccessToken(
            userId = 101L,
            roles = setOf(ActorRole.USER, ActorRole.ORG_ADMIN),
            orgId = 44L
        )

        val claims = service.parse(token)

        assertEquals(101L, claims.userId)
        assertEquals(setOf(ActorRole.USER, ActorRole.ORG_ADMIN), claims.roles)
        assertEquals(44L, claims.orgId)
        assertEquals(clock.instant().epochSecond + 3600, claims.expiresAtEpochSeconds)
    }

    @Test
    fun `parse rejects tampered signature`() {
        val token = service.issueAccessToken(101L, setOf(ActorRole.USER), null)
        val tampered = token.replace("a", "b")

        val exception = assertThrows<com.demo.tourwave.domain.common.DomainException> {
            service.parse(tampered)
        }

        assertEquals(401, exception.status)
        assertTrue(exception.message.contains("invalid"))
    }
}
