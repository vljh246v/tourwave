package com.demo.tourwave.application.user

import com.demo.tourwave.domain.user.UserStatus
import com.demo.tourwave.support.FakeUserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * UserCommandService application-layer unit tests using FakeUserRepository.
 * Tests suspend/delete/restore lifecycle operations.
 */
class UserCommandServiceApplicationTest {
    private val fakeUserRepository = FakeUserRepository()
    private val fixedInstant = Instant.parse("2026-04-26T00:00:00Z")
    private val clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

    private val service = UserCommandService(fakeUserRepository, clock)

    @BeforeEach
    fun setUp() {
        fakeUserRepository.clear()
    }

    private fun saveActiveUser(
        email: String = "user@example.com",
        displayName: String = "Test User",
    ): Long {
        val user =
            fakeUserRepository.save(
                com.demo.tourwave.domain.user.User.create(
                    displayName = displayName,
                    email = email,
                    passwordHash = "hash",
                    now = fixedInstant,
                ),
            )
        return requireNotNull(user.id)
    }

    // ── suspendUser ──────────────────────────────────────────────────────────

    @Test
    fun `suspendUser transitions user to SUSPENDED`() {
        val userId = saveActiveUser()
        service.suspendUser(userId, "policy-violation")
        val suspended = fakeUserRepository.findById(userId)!!
        assertEquals(UserStatus.SUSPENDED, suspended.status)
    }

    @Test
    fun `suspendUser throws when user not found`() {
        assertThrows<NoSuchElementException> {
            service.suspendUser(9999L, "reason")
        }
    }

    // ── deleteUser ───────────────────────────────────────────────────────────

    @Test
    fun `deleteUser transitions user to DELETED and masks PII`() {
        val userId = saveActiveUser()
        service.deleteUser(userId)
        val deleted = fakeUserRepository.findById(userId)!!
        assertEquals(UserStatus.DELETED, deleted.status)
        assertEquals("Deleted User #$userId", deleted.displayName)
        assertEquals("deleted_$userId@deleted.local", deleted.email)
        assertEquals("[DELETED]", deleted.passwordHash)
    }

    @Test
    fun `deleteUser throws when user not found`() {
        assertThrows<NoSuchElementException> {
            service.deleteUser(9999L)
        }
    }

    // ── restoreUser ──────────────────────────────────────────────────────────

    @Test
    fun `restoreUser transitions SUSPENDED user back to ACTIVE`() {
        val userId = saveActiveUser()
        service.suspendUser(userId, "reason")
        service.restoreUser(userId)
        val restored = fakeUserRepository.findById(userId)!!
        assertEquals(UserStatus.ACTIVE, restored.status)
    }

    @Test
    fun `restoreUser throws when user not found`() {
        assertThrows<NoSuchElementException> {
            service.restoreUser(9999L)
        }
    }

    // ── registerUser ─────────────────────────────────────────────────────────

    @Test
    fun `registerUser creates new user with ACTIVE status`() {
        val user = service.registerUser("New User", "new@example.com")
        assertEquals("New User", user.displayName)
        assertEquals("new@example.com", user.email)
        assertEquals(UserStatus.ACTIVE, user.status)
    }

    @Test
    fun `registerUser throws when email already exists`() {
        saveActiveUser(email = "duplicate@example.com")
        assertThrows<IllegalArgumentException> {
            service.registerUser("Another User", "duplicate@example.com")
        }
    }
}
