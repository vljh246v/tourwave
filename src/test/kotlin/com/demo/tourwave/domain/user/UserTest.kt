package com.demo.tourwave.domain.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class UserTest {
    private val now = Instant.parse("2026-04-26T00:00:00Z")
    private val later = now.plusSeconds(60)

    // ── 생성 ─────────────────────────────────────────────────────────────────

    @Test
    fun `create sets displayName email passwordHash and ACTIVE status`() {
        val user = User.create(displayName = "Test User", email = "test@example.com", passwordHash = "hash", now = now)
        assertEquals("Test User", user.displayName)
        assertEquals("test@example.com", user.email)
        assertEquals("hash", user.passwordHash)
        assertEquals(UserStatus.ACTIVE, user.status)
    }

    @Test
    fun `create sets id to null`() {
        val user = User.create(displayName = "Test User", email = "test@example.com", passwordHash = "hash", now = now)
        assertNull(user.id)
    }

    @Test
    fun `create sets createdAt and updatedAt to now`() {
        val user = User.create(displayName = "Test User", email = "test@example.com", passwordHash = "hash", now = now)
        assertEquals(now, user.createdAt)
        assertEquals(now, user.updatedAt)
    }

    @Test
    fun `create sets emailVerifiedAt to null`() {
        val user = User.create(displayName = "Test User", email = "test@example.com", passwordHash = "hash", now = now)
        assertNull(user.emailVerifiedAt)
    }

    // ── persisted ────────────────────────────────────────────────────────────

    @Test
    fun `persisted assigns id and preserves other fields`() {
        val user = User.create(displayName = "Test User", email = "test@example.com", passwordHash = "hash", now = now)
        val persistedUser = user.persisted(42L)
        assertEquals(42L, persistedUser.id)
        assertEquals("Test User", persistedUser.displayName)
        assertEquals("test@example.com", persistedUser.email)
    }

    // ── updateProfile ────────────────────────────────────────────────────────

    @Test
    fun `updateProfile changes displayName and updatedAt`() {
        val user = User.create(displayName = "Old Name", email = "test@example.com", passwordHash = "hash", now = now)
        val updated = user.updateProfile(displayName = "New Name", now = later)
        assertEquals("New Name", updated.displayName)
        assertEquals(later, updated.updatedAt)
    }

    @Test
    fun `updateProfile does not change other fields`() {
        val user = User.create(displayName = "Old Name", email = "test@example.com", passwordHash = "hash", now = now)
        val updated = user.updateProfile(displayName = "New Name", now = later)
        assertEquals("test@example.com", updated.email)
        assertEquals("hash", updated.passwordHash)
        assertEquals(UserStatus.ACTIVE, updated.status)
        assertEquals(now, updated.createdAt)
    }

    // ── verifyEmail ──────────────────────────────────────────────────────────

    @Test
    fun `verifyEmail sets emailVerifiedAt`() {
        val user = User.create(displayName = "Test User", email = "test@example.com", passwordHash = "hash", now = now)
        val verified = user.verifyEmail(later)
        assertEquals(later, verified.emailVerifiedAt)
        assertEquals(later, verified.updatedAt)
    }

    @Test
    fun `verifyEmail is idempotent - second call does not change emailVerifiedAt`() {
        val user = User.create(displayName = "Test User", email = "test@example.com", passwordHash = "hash", now = now)
        val firstVerified = user.verifyEmail(later)
        val secondVerified = firstVerified.verifyEmail(later.plusSeconds(10))
        assertEquals(later, secondVerified.emailVerifiedAt)
    }

    // ── updatePassword ───────────────────────────────────────────────────────

    @Test
    fun `updatePassword changes passwordHash and updatedAt`() {
        val user = User.create(displayName = "Test User", email = "test@example.com", passwordHash = "old-hash", now = now)
        val updated = user.updatePassword(passwordHash = "new-hash", now = later)
        assertEquals("new-hash", updated.passwordHash)
        assertEquals(later, updated.updatedAt)
    }

    @Test
    fun `updatePassword does not change displayName or email`() {
        val user = User.create(displayName = "Test User", email = "test@example.com", passwordHash = "old-hash", now = now)
        val updated = user.updatePassword(passwordHash = "new-hash", now = later)
        assertEquals("Test User", updated.displayName)
        assertEquals("test@example.com", updated.email)
    }

    // ── deactivate ───────────────────────────────────────────────────────────

    @Test
    fun `deactivate changes status to DEACTIVATED`() {
        val user = User.create(displayName = "Test User", email = "test@example.com", passwordHash = "hash", now = now)
        val deactivated = user.deactivate(later)
        assertEquals(UserStatus.DEACTIVATED, deactivated.status)
        assertEquals(later, deactivated.updatedAt)
    }

    @Test
    fun `deactivate is idempotent when already DEACTIVATED`() {
        val user = User.create(displayName = "Test User", email = "test@example.com", passwordHash = "hash", now = now)
        val deactivated = user.deactivate(later)
        val deactivatedAgain = deactivated.deactivate(later.plusSeconds(10))
        assertEquals(UserStatus.DEACTIVATED, deactivatedAgain.status)
        assertEquals(later, deactivatedAgain.updatedAt)
    }
}
