package com.demo.tourwave.application.auth

import com.demo.tourwave.application.auth.port.AuthRefreshTokenRepository
import com.demo.tourwave.application.auth.port.PasswordHasher
import com.demo.tourwave.application.auth.port.UserActionTokenRepository
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.auth.AuthRefreshToken
import com.demo.tourwave.domain.auth.UserActionToken
import com.demo.tourwave.domain.user.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
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
    private val clock = Clock.fixed(Instant.parse("2026-03-17T00:00:00Z"), ZoneOffset.UTC)
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
        userActionTokenRepository = userActionTokenRepository,
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
}
