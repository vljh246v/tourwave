package com.demo.tourwave.domain.service

import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.domain.user.port.UserQueryPort
import com.demo.tourwave.domain.user.service.UserCommandService

class UserCommandServiceTest {

    private val userQueryPort: UserQueryPort = mock()
    private val userCommandService = UserCommandService(userQueryPort)

    @Test
    fun `registerUser should throw exception when user with email already exists`() {
        val existingEmail = "old@test.com"
        whenever(userQueryPort.getUserByEmail(existingEmail)).thenReturn(User.create("old user", existingEmail))

        val exception = assertThrows<IllegalArgumentException> {
            userCommandService.registerUser("new user", existingEmail)
        }
        assertEquals("User with email $existingEmail already exists", exception.message)
    }

    @Test
    fun `registerUser should create new user when email does not exist`() {

        val newEmail = "new@test.com"
        whenever(userQueryPort.getUserByEmail(newEmail)).thenReturn(null)

        val user = userCommandService.registerUser("new user", newEmail)

        assertNotNull(user)
        assertEquals("new user", user.name)
        assertEquals(newEmail, user.email)
    }
}