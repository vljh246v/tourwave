package com.demo.tourwave.domain.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import com.demo.tourwave.application.user.UserCommandService
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.user.User
import org.mockito.kotlin.verify

class UserCommandServiceTest {

    private val userRepository: UserRepository = mock()
    private val userCommandService = UserCommandService(userRepository)

    @Test
    fun `registerUser should throw exception when user with email already exists`() {
        val existingEmail = "old@test.com"
        whenever(userRepository.findByEmail(existingEmail)).thenReturn(User.create("old user", existingEmail))

        val exception = assertThrows<IllegalArgumentException> {
            userCommandService.registerUser("new user", existingEmail)
        }
        assertEquals("User with email $existingEmail already exists", exception.message)
    }

    @Test
    fun `registerUser should create new user when email does not exist`() {
        val newEmail = "new@test.com"
        whenever(userRepository.findByEmail(newEmail)).thenReturn(null)
        whenever(userRepository.save(User.create("new user", newEmail)))
            .thenReturn(User(id = 1L, name = "new user", email = newEmail))

        val user = userCommandService.registerUser("new user", newEmail)

        assertNotNull(user)
        assertEquals(1L, user.id)
        assertEquals("new user", user.name)
        assertEquals(newEmail, user.email)
        verify(userRepository).save(User.create("new user", newEmail))
    }
}
