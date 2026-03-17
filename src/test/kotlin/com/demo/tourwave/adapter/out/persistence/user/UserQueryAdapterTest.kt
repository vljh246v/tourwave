package com.demo.tourwave.adapter.out.persistence.user

import com.demo.tourwave.domain.user.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserQueryAdapterTest {
    private val adapter = UserQueryAdapter()

    @BeforeEach
    fun setUp() {
        adapter.clear()
    }

    @Test
    fun `save persists user and normalizes email for lookup`() {
        val saved = adapter.save(User.create(displayName = "Jae", email = "User@Test.com", passwordHash = "hashed"))

        assertNotNull(saved.id)
        assertEquals(saved, adapter.findById(requireNotNull(saved.id)))
        assertEquals(saved, adapter.findByEmail("user@test.com"))
    }

    @Test
    fun `findByEmail returns null when user is absent`() {
        assertNull(adapter.findByEmail("missing@test.com"))
    }
}
