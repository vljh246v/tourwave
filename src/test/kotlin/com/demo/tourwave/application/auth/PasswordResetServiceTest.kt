package com.demo.tourwave.application.auth

import com.demo.tourwave.application.auth.port.UserActionTokenRepository
import com.demo.tourwave.domain.auth.UserActionTokenPurpose
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.domain.user.UserStatus
import com.demo.tourwave.support.FakeAuditEventPort
import com.demo.tourwave.support.FakeNotificationChannelPort
import com.demo.tourwave.support.FakeUserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class PasswordResetServiceTest {
    private val userRepository = FakeUserRepository()
    private val notificationChannelPort = FakeNotificationChannelPort()
    private val auditEventPort = FakeAuditEventPort()
    private val userActionTokenRepository: UserActionTokenRepository = mock()
    private val clock = Clock.fixed(Instant.parse("2026-04-26T10:00:00Z"), ZoneOffset.UTC)
    private val actionTokenGenerator = ActionTokenGenerator { "test-reset-token-123" }
    private val userActionTokenService =
        UserActionTokenService(
            userActionTokenRepository = userActionTokenRepository,
            actionTokenGenerator = actionTokenGenerator,
            clock = clock,
        )

    private lateinit var service: PasswordResetService

    @BeforeEach
    fun setUp() {
        userRepository.clear()
        notificationChannelPort.clear()
        auditEventPort.clear()

        whenever(userActionTokenRepository.findActiveByUserIdAndPurpose(any(), eq(UserActionTokenPurpose.PASSWORD_RESET), any()))
            .thenReturn(emptyList())
        whenever(userActionTokenRepository.save(any())).thenAnswer { it.arguments[0] }

        service =
            PasswordResetService(
                notificationChannelPort = notificationChannelPort,
                userActionTokenService = userActionTokenService,
                userRepository = userRepository,
                auditEventPort = auditEventPort,
                clock = clock,
            )
    }

    @Test
    fun `requestPasswordReset for existing ACTIVE user sends email and records audit event`() {
        val user =
            User.create(
                displayName = "Test User",
                email = "test@example.com",
                passwordHash = "hash",
                now = clock.instant(),
            ).copy(id = 42L)
        userRepository.save(user)

        service.requestPasswordReset("test@example.com")

        // Verify email sent
        assertEquals(1, notificationChannelPort.sentMessages.size)
        val message = notificationChannelPort.sentMessages.first()
        assertEquals("test@example.com", message.recipient)
        assertEquals("PASSWORD_RESET", message.templateType)
        assertTrue(message.body.contains("test-reset-token-123"))
        assertTrue(message.body.contains(clock.instant().plus(Duration.ofHours(2)).toString()))
        assertTrue(message.body.contains("https://app.tourwave.com/auth/reset-password?token=test-reset-token-123"))

        // Verify audit event
        assertEquals(1, auditEventPort.events.size)
        val event = auditEventPort.events.first()
        assertEquals("PASSWORD_RESET_EMAIL_SENT", event.action)
        assertEquals("USER", event.resourceType)
        assertEquals(42L, event.resourceId)
    }

    @Test
    fun `requestPasswordReset for non-existent email returns early without sending email`() {
        service.requestPasswordReset("nonexistent@example.com")

        assertEquals(0, notificationChannelPort.sentMessages.size)
        assertEquals(0, auditEventPort.events.size)
    }

    @Test
    fun `requestPasswordReset for INACTIVE user returns early without sending email`() {
        val user =
            User.create(
                displayName = "Inactive User",
                email = "inactive@example.com",
                passwordHash = "hash",
                now = clock.instant(),
            ).copy(id = 99L, status = UserStatus.DEACTIVATED)
        userRepository.save(user)

        service.requestPasswordReset("inactive@example.com")

        assertEquals(0, notificationChannelPort.sentMessages.size)
        assertEquals(0, auditEventPort.events.size)
    }

    @Test
    fun `requestPasswordReset throws DomainException when email send fails but audit event is still recorded`() {
        val user =
            User.create(
                displayName = "Test User",
                email = "test@example.com",
                passwordHash = "hash",
                now = clock.instant(),
            ).copy(id = 42L)
        userRepository.save(user)
        notificationChannelPort.shouldThrow = RuntimeException("SMTP connection failed")

        val exception =
            assertThrows<DomainException> {
                service.requestPasswordReset("test@example.com")
            }

        assertEquals(500, exception.status)
        assertTrue(exception.message?.contains("failed to send password reset email") == true)

        // Verify audit event was recorded even though email send failed
        assertEquals(1, auditEventPort.events.size)
        val event = auditEventPort.events.first()
        assertEquals("PASSWORD_RESET_EMAIL_SENT", event.action)
        assertEquals("USER", event.resourceType)
        assertEquals(42L, event.resourceId)
    }
}
