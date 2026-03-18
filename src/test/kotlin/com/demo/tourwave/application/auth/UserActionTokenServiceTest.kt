package com.demo.tourwave.application.auth

import com.demo.tourwave.adapter.out.persistence.auth.InMemoryUserActionTokenRepositoryAdapter
import com.demo.tourwave.domain.auth.UserActionTokenPurpose
import com.demo.tourwave.domain.common.DomainException
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UserActionTokenServiceTest {
    private val clock = Clock.fixed(Instant.parse("2026-03-17T00:00:00Z"), ZoneOffset.UTC)
    private val repository = InMemoryUserActionTokenRepositoryAdapter()
    private val generated = ArrayDeque(listOf("first-token", "second-token"))
    private val service =
        UserActionTokenService(
            userActionTokenRepository = repository,
            actionTokenGenerator = { generated.removeFirst() },
            clock = clock,
        )

    @Test
    fun `issue replaces prior active token for same purpose`() {
        service.issue(7L, UserActionTokenPurpose.EMAIL_VERIFICATION, java.time.Duration.ofHours(24))
        service.issue(7L, UserActionTokenPurpose.EMAIL_VERIFICATION, java.time.Duration.ofHours(24))

        val active = repository.findActiveByUserIdAndPurpose(7L, UserActionTokenPurpose.EMAIL_VERIFICATION, clock.instant())

        assertEquals(1, active.size)
        assertEquals(service.hash("second-token"), active.single().tokenHash)
    }

    @Test
    fun `consume rejects replayed token`() {
        service.issue(9L, UserActionTokenPurpose.PASSWORD_RESET, java.time.Duration.ofHours(2))

        service.consume("first-token", UserActionTokenPurpose.PASSWORD_RESET)
        val exception =
            assertFailsWith<DomainException> {
                service.consume("first-token", UserActionTokenPurpose.PASSWORD_RESET)
            }

        assertEquals(400, exception.status)
        assertTrue(repository.findByTokenHash(service.hash("first-token"))?.consumedAtUtc != null)
    }
}
