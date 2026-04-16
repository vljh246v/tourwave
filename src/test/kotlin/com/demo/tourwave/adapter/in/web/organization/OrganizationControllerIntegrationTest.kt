package com.demo.tourwave.adapter.`in`.web.organization

import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.customer.port.NotificationDeliveryRepository
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
class OrganizationControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var membershipRepository: OrganizationMembershipRepository

    @Autowired
    private lateinit var notificationDeliveryRepository: NotificationDeliveryRepository

    @BeforeEach
    fun setUp() {
        notificationDeliveryRepository.clear()
        membershipRepository.clear()
        organizationRepository.clear()
        userRepository.clear()
    }

    @Test
    fun `operator organization APIs support create update membership and public query`() {
        val owner = userRepository.save(User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = Instant.now()))
        val invitee = userRepository.save(User.create(displayName = "Invitee", email = "invitee@test.com", passwordHash = "hash", now = Instant.now()))

        mockMvc.perform(
            post("/operator/organizations")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{
                      "slug":"seoul-operators",
                      "name":"Seoul Operators",
                      "publicDescription":"Public profile",
                      "contactEmail":"ops@seoul.test",
                      "timezone":"Asia/Seoul"
                    }"""
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.slug").value("seoul-operators"))

        val organizationId = requireNotNull(organizationRepository.findBySlug("seoul-operators")?.id)

        mockMvc.perform(
            patch("/operator/organizations/$organizationId")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{
                      "name":"Seoul Operators Updated",
                      "publicDescription":"Updated public profile",
                      "contactEmail":"team@seoul.test",
                      "timezone":"Asia/Seoul"
                    }"""
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Seoul Operators Updated"))

        mockMvc.perform(
            post("/operator/organizations/$organizationId/members/invitations")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":${requireNotNull(invitee.id)},"role":"MEMBER"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("INVITED"))
        val inviteDelivery = notificationDeliveryRepository.findAll().single()
        val token = Regex("token=([^\\s.]+)")
            .find(inviteDelivery.body)
            ?.groupValues
            ?.get(1)
            ?: error("invitation token not found in delivery body")

        mockMvc.perform(
            post("/organizations/$organizationId/memberships/accept")
                .header("X-Actor-User-Id", requireNotNull(invitee.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"$token"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ACTIVE"))

        mockMvc.perform(
            patch("/operator/organizations/$organizationId/members/${requireNotNull(invitee.id)}/role")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"role":"ADMIN"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.role").value("ADMIN"))

        mockMvc.perform(
            get("/operator/organizations/$organizationId/members")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].role").value("OWNER"))

        mockMvc.perform(get("/organizations/$organizationId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.publicDescription").value("Updated public profile"))
            .andExpect(jsonPath("$.businessRegistrationNumber").doesNotExist())
    }

    @Test
    fun `organization access is denied without active membership`() {
        val owner = userRepository.save(User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = Instant.now()))
        val outsider = userRepository.save(User.create(displayName = "Outsider", email = "outsider@test.com", passwordHash = "hash", now = Instant.now()))
        mockMvc.perform(
            post("/operator/organizations")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"slug":"jeju-ops","name":"Jeju Ops","timezone":"Asia/Seoul"}""")
        ).andExpect(status().isCreated)
        val organizationId = requireNotNull(organizationRepository.findBySlug("jeju-ops")?.id)

        mockMvc.perform(
            get("/operator/organizations/$organizationId")
                .header("X-Actor-User-Id", requireNotNull(outsider.id))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
    }
}
