package com.demo.tourwave.domain.service

import com.demo.tourwave.application.user.UserCommandService
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.domain.user.UserStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

class UserCommandServiceTest {
    private val userRepository: UserRepository = mock()
    private val userCommandService = UserCommandService(userRepository)

    @Test
    fun `registerUser should throw exception when user with email already exists`() {
        val existingEmail = "old@test.com"
        whenever(userRepository.findByEmail(existingEmail)).thenReturn(
            User.create(
                displayName = "old user",
                email = existingEmail,
                passwordHash = "hashed",
            ),
        )

        val exception =
            assertThrows<IllegalArgumentException> {
                userCommandService.registerUser("new user", existingEmail)
            }
        assertEquals("User with email $existingEmail already exists", exception.message)
    }

    @Test
    fun `registerUser should create new user when email does not exist`() {
        val newEmail = "new@test.com"
        whenever(userRepository.findByEmail(newEmail)).thenReturn(null)
        whenever(userRepository.save(org.mockito.kotlin.any()))
            .thenReturn(
                User(
                    id = 1L,
                    displayName = "new user",
                    email = newEmail,
                    passwordHash = "hashed",
                    status = UserStatus.ACTIVE,
                    createdAt = Instant.parse("2026-03-17T00:00:00Z"),
                    updatedAt = Instant.parse("2026-03-17T00:00:00Z"),
                ),
            )

        val user = userCommandService.registerUser("new user", newEmail)

        assertNotNull(user)
        assertEquals(1L, user.id)
        assertEquals("new user", user.displayName)
        assertEquals(newEmail, user.email)
        verify(userRepository).save(
            org.mockito.kotlin.check {
                assertEquals("new user", it.displayName)
                assertEquals(newEmail, it.email)
                assertTrue(it.passwordHash.isNotBlank())
            },
        )
    }
}
