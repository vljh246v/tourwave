package com.demo.tourwave.application.auth

import com.demo.tourwave.application.auth.port.AuthRefreshTokenRepository
import com.demo.tourwave.application.auth.port.PasswordHasher
import com.demo.tourwave.application.auth.port.UserActionTokenRepository
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.auth.AuthRefreshToken
import com.demo.tourwave.domain.auth.UserActionToken
import com.demo.tourwave.domain.auth.UserActionTokenPurpose
import com.demo.tourwave.domain.user.UserStatus
import com.demo.tourwave.domain.user.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AuthCommandServiceTest {
    private val userRepository: UserRepository = mock()
    private val passwordHasher: PasswordHasher = mock()
    private val authRefreshTokenRepository: AuthRefreshTokenRepository = mock()
    private val userActionTokenRepository: UserActionTokenRepository = mock()
    private val auditEventPort: AuditEventPort = mock()
    private val clock = Clock.fixed(Instant.parse("2026-03-17T00:00:00Z"), ZoneOffset.UTC)
    private val userActionTokenService = UserActionTokenService(
        userActionTokenRepository = userActionTokenRepository,
        actionTokenGenerator = ActionTokenGenerator { "fixed-action-token" },
        clock = clock
    )
    private val jwtTokenService = JwtTokenService(
        secret = "auth-command-secret",
        accessTokenTtlSeconds = 3600,
        clock = clock,
        objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
    )
    private val authTokenLifecycleService = AuthTokenLifecycleService(
        authRefreshTokenRepository = authRefreshTokenRepository,
        refreshTokenTtlSeconds = 600,
        clock = clock
    )
    private val service = AuthCommandService(
        userRepository = userRepository,
        passwordHasher = passwordHasher,
        jwtTokenService = jwtTokenService,
        authTokenLifecycleService = authTokenLifecycleService,
        userActionTokenService = userActionTokenService,
        auditEventPort = auditEventPort,
        clock = clock
    )

    @Test
    fun `signup creates user and verification token`() {
        whenever(userRepository.findByEmail("new@test.com")).thenReturn(null)
        whenever(passwordHasher.hash("Password12")).thenReturn("hashed-password")
        whenever(userRepository.save(any())).thenAnswer { invocation ->
            val user = invocation.arguments[0] as User
            user.copy(id = 11L)
        }
        whenever(userActionTokenRepository.save(any())).thenAnswer { invocation ->
            invocation.arguments[0] as UserActionToken
        }
        whenever(
            userActionTokenRepository.findActiveByUserIdAndPurpose(
                11L,
                UserActionTokenPurpose.EMAIL_VERIFICATION,
                clock.instant()
            )
        ).thenReturn(emptyList())
        whenever(authRefreshTokenRepository.save(any())).thenAnswer { invocation ->
            val token = invocation.arguments[0] as AuthRefreshToken
            token.copy(id = 1L)
        }

        val result = service.signup("Jae", "new@test.com", "Password12")

        assertNotNull(result.accessToken)
        assertNotNull(result.refreshToken)
        assertEquals(11L, result.user.id)
        val tokenCaptor = argumentCaptor<UserActionToken>()
        verify(userActionTokenRepository).save(tokenCaptor.capture())
        assertEquals(11L, tokenCaptor.firstValue.userId)
    }

    @Test
    fun `login rejects invalid password`() {
        whenever(userRepository.findByEmail("user@test.com")).thenReturn(
            User.create(
                displayName = "User",
                email = "user@test.com",
                passwordHash = "hashed",
                now = clock.instant()
            ).copy(id = 15L)
        )
        whenever(passwordHasher.matches("Password12", "hashed")).thenReturn(false)

        val exception = assertThrows<com.demo.tourwave.domain.common.DomainException> {
            service.login("user@test.com", "Password12")
        }

        assertEquals(401, exception.status)
        verify(authRefreshTokenRepository, never()).save(any())
    }

    @Test
    fun `confirm password reset updates password and revokes refresh tokens`() {
        val user = User.create(
            displayName = "User",
            email = "user@test.com",
            passwordHash = "old-hash",
            now = clock.instant()
        ).copy(id = 15L)
        whenever(userActionTokenRepository.findByTokenHash(any())).thenReturn(
            UserActionToken(
                id = 9L,
                userId = 15L,
                tokenHash = "hashed",
                purpose = UserActionTokenPurpose.PASSWORD_RESET,
                expiresAtUtc = clock.instant().plusSeconds(3600),
                createdAtUtc = clock.instant()
            )
        )
        whenever(userActionTokenRepository.save(any())).thenAnswer { it.arguments[0] as UserActionToken }
        whenever(userRepository.findById(15L)).thenReturn(user)
        whenever(passwordHasher.hash("Password99")).thenReturn("new-hash")
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] as User }

        service.confirmPasswordReset("fixed-action-token", "Password99")

        verify(passwordHasher).hash("Password99")
        verify(userRepository).save(check {
            assertEquals("new-hash", it.passwordHash)
        })
        verify(authRefreshTokenRepository).revokeAllByUserId(eq(15L), any())
        verify(auditEventPort).append(any())
    }

    @Test
    fun `deactivate marks user as deactivated and revokes sessions`() {
        val user = User.create(
            displayName = "User",
            email = "user@test.com",
            passwordHash = "hash",
            now = clock.instant()
        ).copy(id = 21L)
        whenever(userRepository.findById(21L)).thenReturn(user)
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] as User }

        service.deactivate(21L)

        verify(userRepository).save(check {
            assertEquals(UserStatus.DEACTIVATED, it.status)
        })
        verify(authRefreshTokenRepository).revokeAllByUserId(eq(21L), any())
        verify(auditEventPort).append(any())
    }

    @Test
    fun `confirm email verification persists verified timestamp`() {
        val user = User.create(
            displayName = "User",
            email = "user@test.com",
            passwordHash = "hash",
            now = clock.instant()
        ).copy(id = 31L)
        whenever(userActionTokenRepository.findByTokenHash(any())).thenReturn(
            UserActionToken(
                id = 10L,
                userId = 31L,
                tokenHash = "hashed",
                purpose = UserActionTokenPurpose.EMAIL_VERIFICATION,
                expiresAtUtc = clock.instant().plusSeconds(3600),
                createdAtUtc = clock.instant()
            )
        )
        whenever(userActionTokenRepository.save(any())).thenAnswer { it.arguments[0] as UserActionToken }
        whenever(userRepository.findById(31L)).thenReturn(user)
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] as User }

        service.confirmEmailVerification("fixed-action-token")

        verify(userRepository).save(check {
            assertTrue(it.emailVerifiedAt != null)
        })
        verify(auditEventPort).append(any())
    }
}
