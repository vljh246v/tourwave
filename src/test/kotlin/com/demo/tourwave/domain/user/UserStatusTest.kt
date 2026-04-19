package com.demo.tourwave.domain.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class UserStatusTest {
    private val now = Instant.parse("2026-04-18T10:00:00Z")

    private fun activeUser(id: Long = 1L) =
        User(
            id = id,
            displayName = "Test User",
            email = "test@example.com",
            passwordHash = "hash",
            status = UserStatus.ACTIVE,
            createdAt = now,
            updatedAt = now,
        )

    // ── ACTIVE 전이 ──────────────────────────────────────────────────────────

    @Test
    fun `ACTIVE to DEACTIVATED via transition`() {
        val result = activeUser().transition(UserStatus.DEACTIVATED, now)
        assertEquals(UserStatus.DEACTIVATED, result.status)
    }

    @Test
    fun `ACTIVE to SUSPENDED via suspend`() {
        val result = activeUser().suspend(now)
        assertEquals(UserStatus.SUSPENDED, result.status)
    }

    @Test
    fun `ACTIVE to DELETED via delete`() {
        val result = activeUser().delete(now)
        assertEquals(UserStatus.DELETED, result.status)
    }

    // ── DEACTIVATED 전이 ─────────────────────────────────────────────────────

    @Test
    fun `DEACTIVATED to ACTIVE via restore`() {
        val result = activeUser().transition(UserStatus.DEACTIVATED, now).restore(now)
        assertEquals(UserStatus.ACTIVE, result.status)
    }

    @Test
    fun `DEACTIVATED to DELETED via delete`() {
        val result = activeUser().transition(UserStatus.DEACTIVATED, now).delete(now)
        assertEquals(UserStatus.DELETED, result.status)
    }

    // ── SUSPENDED 전이 ───────────────────────────────────────────────────────

    @Test
    fun `SUSPENDED to ACTIVE via restore`() {
        val result = activeUser().suspend(now).restore(now)
        assertEquals(UserStatus.ACTIVE, result.status)
    }

    @Test
    fun `SUSPENDED to DELETED via delete`() {
        val result = activeUser().suspend(now).delete(now)
        assertEquals(UserStatus.DELETED, result.status)
    }

    // ── DELETED 터미널 상태 ──────────────────────────────────────────────────

    @Test
    fun `DELETED is terminal - transition to ACTIVE throws`() {
        val deleted = activeUser().delete(now)
        assertThrows<IllegalArgumentException> {
            deleted.transition(UserStatus.ACTIVE, now)
        }
    }

    @Test
    fun `DELETED is terminal - suspend throws`() {
        val deleted = activeUser().delete(now)
        assertThrows<IllegalArgumentException> {
            deleted.suspend(now)
        }
    }

    @Test
    fun `DELETED is terminal - delete again throws`() {
        val deleted = activeUser().delete(now)
        assertThrows<IllegalArgumentException> {
            deleted.delete(now)
        }
    }

    // ── 금지된 전이 ──────────────────────────────────────────────────────────

    @Test
    fun `SUSPENDED to DEACTIVATED is not allowed`() {
        val suspended = activeUser().suspend(now)
        assertThrows<IllegalArgumentException> {
            suspended.transition(UserStatus.DEACTIVATED, now)
        }
    }

    @Test
    fun `DEACTIVATED to SUSPENDED is not allowed`() {
        val deactivated = activeUser().transition(UserStatus.DEACTIVATED, now)
        assertThrows<IllegalArgumentException> {
            deactivated.transition(UserStatus.SUSPENDED, now)
        }
    }

    // ── delete 마스킹 ────────────────────────────────────────────────────────

    @Test
    fun `delete masks displayName email and passwordHash`() {
        val deleted = activeUser(id = 42L).delete(now)
        assertEquals("Deleted User #42", deleted.displayName)
        assertEquals("deleted_42@deleted.local", deleted.email)
        assertEquals("[DELETED]", deleted.passwordHash)
    }

    @Test
    fun `delete sets deletedAt timestamp`() {
        val deleted = activeUser().delete(now)
        assertNotNull(deleted.deletedAt)
        assertEquals(now, deleted.deletedAt)
    }

    @Test
    fun `delete sets updatedAt timestamp`() {
        val laterTime = now.plusSeconds(60)
        val deleted = activeUser().delete(laterTime)
        assertEquals(laterTime, deleted.updatedAt)
    }

    // ── restore / deactivate 호환성 ──────────────────────────────────────────

    @Test
    fun `restore from deactivate result returns ACTIVE`() {
        val result = activeUser().deactivate(now).restore(now)
        assertEquals(UserStatus.ACTIVE, result.status)
    }
}
