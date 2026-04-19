package com.demo.tourwave.application.user

import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.domain.user.UserStatus
import com.demo.tourwave.support.FakeUserPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class UserServiceTest {
    private val fakeUserPort = FakeUserPort()
    private val capturedEvents = mutableListOf<AuditEventCommand>()
    private val fakeAuditEventPort =
        object : AuditEventPort {
            override fun append(event: AuditEventCommand) {
                capturedEvents.add(event)
            }
        }
    private val fixedInstant = Instant.parse("2026-04-19T00:00:00Z")
    private val clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

    private val userService =
        UserService(
            userPort = fakeUserPort,
            auditEventPort = fakeAuditEventPort,
            clock = clock,
        )

    @BeforeEach
    fun setUp() {
        fakeUserPort.clear()
        capturedEvents.clear()
    }

    private fun savedUser(
        displayName: String = "Test User",
        email: String = "test@example.com",
    ): User {
        return fakeUserPort.save(
            User.create(
                displayName = displayName,
                email = email,
                passwordHash = "hash",
                now = fixedInstant,
            ),
        )
    }

    @Test
    fun `getCurrentUser returns user when exists`() {
        val user = savedUser()
        val result = userService.getCurrentUser(requireNotNull(user.id))
        assertEquals(user.id, result.id)
        assertEquals("Test User", result.displayName)
    }

    @Test
    fun `getCurrentUser throws 401 when user not found`() {
        val ex =
            assertThrows<DomainException> {
                userService.getCurrentUser(9999L)
            }
        assertEquals(401, ex.status)
    }

    @Test
    fun `updateProfile updates displayName and records audit event`() {
        val user = savedUser()
        val updated = userService.updateProfile(requireNotNull(user.id), "New Name")

        assertEquals("New Name", updated.displayName)
        assertEquals(1, capturedEvents.size)
        val event = capturedEvents[0]
        assertEquals("USER_PROFILE_UPDATED", event.action)
        assertEquals("USER", event.resourceType)
        assertEquals(user.id, event.resourceId)
        assertEquals("USER_PROFILE_UPDATED", event.action)
    }

    @Test
    fun `updateProfile trims whitespace from displayName`() {
        val user = savedUser()
        val updated = userService.updateProfile(requireNotNull(user.id), "  Trimmed  ")
        assertEquals("Trimmed", updated.displayName)
    }

    @Test
    fun `updateProfile throws 422 when displayName is blank`() {
        val user = savedUser()
        val ex =
            assertThrows<DomainException> {
                userService.updateProfile(requireNotNull(user.id), "   ")
            }
        assertEquals(422, ex.status)
    }

    @Test
    fun `updateProfile throws 422 when displayName exceeds 100 chars`() {
        val user = savedUser()
        val longName = "A".repeat(101)
        val ex =
            assertThrows<DomainException> {
                userService.updateProfile(requireNotNull(user.id), longName)
            }
        assertEquals(422, ex.status)
    }

    @Test
    fun `updateProfile accepts displayName of exactly 100 chars`() {
        val user = savedUser()
        val exactName = "A".repeat(100)
        val updated = userService.updateProfile(requireNotNull(user.id), exactName)
        assertEquals(exactName, updated.displayName)
    }

    @Test
    fun `getOrCreate returns existing user when email matches`() {
        val user = savedUser(email = "existing@example.com")
        val result = userService.getOrCreate("existing@example.com", "Different Name")
        assertEquals(user.id, result.id)
        assertEquals("Test User", result.displayName)
    }

    @Test
    fun `getOrCreate creates new user when email not found`() {
        val result = userService.getOrCreate("new@example.com", "New User")
        assertNotNull(result.id)
        assertEquals("new@example.com", result.email)
        assertEquals("New User", result.displayName)
        assertEquals(UserStatus.ACTIVE, result.status)
    }

    @Test
    fun `getOrCreate normalizes email to lowercase`() {
        val result = userService.getOrCreate("UPPER@EXAMPLE.COM", "Some User")
        assertEquals("upper@example.com", result.email)
    }
}
