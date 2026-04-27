package com.demo.tourwave.adapter.`in`.web.announcement

import com.demo.tourwave.application.announcement.port.AnnouncementRepository
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.user.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
class AnnouncementControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var membershipRepository: OrganizationMembershipRepository

    @Autowired
    private lateinit var announcementRepository: AnnouncementRepository

    @Autowired
    private lateinit var idempotencyStore: IdempotencyStore

    @BeforeEach
    fun setUp() {
        idempotencyStore.clear()
        announcementRepository.clear()
        membershipRepository.clear()
        organizationRepository.clear()
        userRepository.clear()
    }

    @Test
    fun `announcement APIs support create update and delete with public query`() {
        val owner =
            userRepository.save(
                User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = Instant.now()),
            )

        mockMvc.perform(
            post("/operator/organizations")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .header("Idempotency-Key", "ann-create-org-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"slug":"announce-org","name":"Announce Org","timezone":"Asia/Seoul"}"""),
        ).andExpect(status().isCreated)

        val organizationId = requireNotNull(organizationRepository.findBySlug("announce-org")?.id)

        mockMvc.perform(
            post("/organizations/$organizationId/announcements")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .header("Idempotency-Key", "create-ann-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Hello World","body":"First post","visibility":"PUBLIC"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("Hello World"))

        val announcementId = requireNotNull(announcementRepository.findByOrganizationId(organizationId).first().id)

        mockMvc.perform(
            patch("/announcements/$announcementId")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .header("Idempotency-Key", "update-ann-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Hello Updated"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Hello Updated"))

        mockMvc.perform(get("/public/announcements"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].title").value("Hello Updated"))

        mockMvc.perform(
            delete("/announcements/$announcementId")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .header("Idempotency-Key", "delete-ann-001"),
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `create announcement with same idempotency key and same payload replays response`() {
        val owner =
            userRepository.save(
                User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = Instant.now()),
            )

        mockMvc.perform(
            post("/operator/organizations")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .header("Idempotency-Key", "ann-create-org-002")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"slug":"replay-org","name":"Replay Org","timezone":"Asia/Seoul"}"""),
        ).andExpect(status().isCreated)

        val organizationId = requireNotNull(organizationRepository.findBySlug("replay-org")?.id)
        val idempotencyKey = "create-ann-replay-001"
        val payload = """{"title":"Idempotent Title","body":"Same body","visibility":"PUBLIC"}"""

        mockMvc.perform(
            post("/organizations/$organizationId/announcements")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("Idempotent Title"))

        // Same key + same payload → replay (idempotent)
        mockMvc.perform(
            post("/organizations/$organizationId/announcements")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("Idempotent Title"))

        // Only one announcement should be created
        assert(announcementRepository.findByOrganizationId(organizationId).size == 1)
    }

    @Test
    fun `create announcement with same idempotency key but different payload returns 422`() {
        val owner =
            userRepository.save(
                User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = Instant.now()),
            )

        mockMvc.perform(
            post("/operator/organizations")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .header("Idempotency-Key", "ann-create-org-003")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"slug":"conflict-org","name":"Conflict Org","timezone":"Asia/Seoul"}"""),
        ).andExpect(status().isCreated)

        val organizationId = requireNotNull(organizationRepository.findBySlug("conflict-org")?.id)
        val idempotencyKey = "create-ann-conflict-001"

        mockMvc.perform(
            post("/organizations/$organizationId/announcements")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Original Title","body":"Original body","visibility":"PUBLIC"}"""),
        )
            .andExpect(status().isCreated)

        // Same key + different payload → 422
        mockMvc.perform(
            post("/organizations/$organizationId/announcements")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Different Title","body":"Different body","visibility":"PUBLIC"}"""),
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD"))
    }

    @Test
    fun `update announcement with same idempotency key but different payload returns 422`() {
        val owner =
            userRepository.save(
                User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = Instant.now()),
            )

        mockMvc.perform(
            post("/operator/organizations")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .header("Idempotency-Key", "ann-create-org-004")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"slug":"update-conflict-org","name":"Update Conflict Org","timezone":"Asia/Seoul"}"""),
        ).andExpect(status().isCreated)

        val organizationId = requireNotNull(organizationRepository.findBySlug("update-conflict-org")?.id)

        mockMvc.perform(
            post("/organizations/$organizationId/announcements")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .header("Idempotency-Key", "create-for-update-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Announcement","body":"Body","visibility":"PUBLIC"}"""),
        ).andExpect(status().isCreated)

        val announcementId = requireNotNull(announcementRepository.findByOrganizationId(organizationId).first().id)
        val updateKey = "update-ann-conflict-001"

        mockMvc.perform(
            patch("/announcements/$announcementId")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .header("Idempotency-Key", updateKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Updated Title"}"""),
        ).andExpect(status().isOk)

        // Same key + different payload → 422
        mockMvc.perform(
            patch("/announcements/$announcementId")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .header("Idempotency-Key", updateKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Different Updated Title"}"""),
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD"))
    }
}
