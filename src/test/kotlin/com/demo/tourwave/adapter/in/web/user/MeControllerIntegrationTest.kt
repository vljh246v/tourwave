package com.demo.tourwave.adapter.`in`.web.user

import com.demo.tourwave.adapter.out.persistence.audit.InMemoryAuditEventAdapter
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.user.User
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

    private fun savedUser(
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

    // ── GET /me ──────────────────────────────────────────────────────────────

    @Test
    fun `GET me returns user info for authenticated user`() {
        val user = savedUser()

        mockMvc.perform(
            get("/me")
                .header("X-Actor-User-Id", requireNotNull(user.id)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.displayName").value("Test User"))
            .andExpect(jsonPath("$.user.email").value("test@example.com"))
            .andExpect(jsonPath("$.user.status").value("ACTIVE"))
    }

    @Test
    fun `GET me returns 401 when X-Actor-User-Id header is missing`() {
        mockMvc.perform(get("/me"))
            .andExpect(status().isUnauthorized)
    }

    // ── PATCH /me ────────────────────────────────────────────────────────────

    @Test
    fun `PATCH me updates displayName and returns updated user`() {
        val user = savedUser()

        mockMvc.perform(
            patch("/me")
                .header("X-Actor-User-Id", requireNotNull(user.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"Updated Name"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.displayName").value("Updated Name"))

        val events = auditEventAdapter.all()
        assert(events.any { it.action == "USER_PROFILE_UPDATED" }) {
            "Expected USER_PROFILE_UPDATED audit event but got: ${events.map { it.action }}"
        }
    }

    @Test
    fun `PATCH me returns 422 when displayName is blank`() {
        val user = savedUser()

        mockMvc.perform(
            patch("/me")
                .header("X-Actor-User-Id", requireNotNull(user.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"   "}"""),
        )
            .andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `PATCH me returns 422 when displayName exceeds 100 characters`() {
        val user = savedUser()
        val longName = "A".repeat(101)

        mockMvc.perform(
            patch("/me")
                .header("X-Actor-User-Id", requireNotNull(user.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"$longName"}"""),
        )
            .andExpect(status().isUnprocessableEntity)
    }

    // ── POST /me/deactivate ──────────────────────────────────────────────────

    @Test
    fun `POST me deactivate returns 204 and records USER_DEACTIVATED audit event`() {
        val user = savedUser()

        mockMvc.perform(
            post("/me/deactivate")
                .header("X-Actor-User-Id", requireNotNull(user.id)),
        )
            .andExpect(status().isNoContent)

        val events = auditEventAdapter.all()
        assert(events.any { it.action == "USER_DEACTIVATED" }) {
            "Expected USER_DEACTIVATED audit event but got: ${events.map { it.action }}"
        }
    }

    @Test
    fun `POST me deactivate is idempotent - calling twice returns 204 both times`() {
        val user = savedUser()
        val userId = requireNotNull(user.id)

        mockMvc.perform(
            post("/me/deactivate")
                .header("X-Actor-User-Id", userId),
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/me/deactivate")
                .header("X-Actor-User-Id", userId),
        )
            .andExpect(status().isNoContent)
    }
}
