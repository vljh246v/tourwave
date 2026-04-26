package com.demo.tourwave.adapter.`in`.web.auth

import com.demo.tourwave.adapter.out.persistence.audit.InMemoryAuditEventAdapter
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.domain.user.UserStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
class MeControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var auditEventAdapter: InMemoryAuditEventAdapter

    @BeforeEach
    fun setUp() {
        userRepository.clear()
        auditEventAdapter.clear()
    }

    private fun createTestUser(
        displayName: String = "Test User",
        email: String = "test@example.com",
    ): User {
        return userRepository.save(
            User.create(
                displayName = displayName,
                email = email,
                passwordHash = "hash",
                now = Instant.now(),
            ),
        )
    }

    @Test
    fun `GET me returns authenticated user profile`() {
        val user = createTestUser()

        mockMvc.perform(
            get("/me")
                .header("X-Actor-User-Id", requireNotNull(user.id)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.id").value(user.id))
            .andExpect(jsonPath("$.user.displayName").value("Test User"))
            .andExpect(jsonPath("$.user.email").value("test@example.com"))
            .andExpect(jsonPath("$.user.status").value("ACTIVE"))
            .andExpect(jsonPath("$.memberships").isArray)
    }

    @Test
    fun `GET me returns 401 when X-Actor-User-Id header is missing`() {
        mockMvc.perform(get("/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
    }

    @Test
    fun `PATCH me updates displayName and returns updated user`() {
        val user = createTestUser()

        mockMvc.perform(
            patch("/me")
                .header("X-Actor-User-Id", requireNotNull(user.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"Updated Name"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(user.id))
            .andExpect(jsonPath("$.displayName").value("Updated Name"))
            .andExpect(jsonPath("$.email").value("test@example.com"))

        val events = auditEventAdapter.all()
        assertEquals(1, events.size)
        assertEquals("USER_PROFILE_UPDATED", events[0].action)
        assertEquals("USER", events[0].resourceType)
        assertEquals(user.id, events[0].resourceId)
    }

    @Test
    fun `PATCH me returns 422 when displayName is blank`() {
        val user = createTestUser()

        mockMvc.perform(
            patch("/me")
                .header("X-Actor-User-Id", requireNotNull(user.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"   "}"""),
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
    }

    @Test
    fun `PATCH me returns 422 when displayName exceeds 100 characters`() {
        val user = createTestUser()
        val longName = "A".repeat(101)

        mockMvc.perform(
            patch("/me")
                .header("X-Actor-User-Id", requireNotNull(user.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"$longName"}"""),
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
    }

    @Test
    fun `POST me deactivate returns 204 and deactivates user`() {
        val user = createTestUser()

        mockMvc.perform(
            post("/me/deactivate")
                .header("X-Actor-User-Id", requireNotNull(user.id)),
        )
            .andExpect(status().isNoContent)

        val deactivatedUser = userRepository.findById(requireNotNull(user.id))
        assertEquals(UserStatus.DEACTIVATED, deactivatedUser?.status)

        val events = auditEventAdapter.all()
        assertEquals(1, events.size)
        assertEquals("USER_DEACTIVATED", events[0].action)
        assertEquals("USER", events[0].resourceType)
        assertEquals(user.id, events[0].resourceId)
    }

    @Test
    fun `PATCH me trims whitespace from displayName`() {
        val user = createTestUser()

        mockMvc.perform(
            patch("/me")
                .header("X-Actor-User-Id", requireNotNull(user.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"  Trimmed Name  "}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.displayName").value("Trimmed Name"))
    }
}
