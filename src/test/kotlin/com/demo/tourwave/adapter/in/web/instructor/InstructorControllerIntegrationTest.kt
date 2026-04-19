package com.demo.tourwave.adapter.`in`.web.instructor

import com.demo.tourwave.application.instructor.port.InstructorProfileRepository
import com.demo.tourwave.application.instructor.port.InstructorRegistrationRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
class InstructorControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var membershipRepository: OrganizationMembershipRepository

    @Autowired
    private lateinit var instructorRegistrationRepository: InstructorRegistrationRepository

    @Autowired
    private lateinit var instructorProfileRepository: InstructorProfileRepository

    @BeforeEach
    fun setUp() {
        instructorProfileRepository.clear()
        instructorRegistrationRepository.clear()
        membershipRepository.clear()
        organizationRepository.clear()
        userRepository.clear()
    }

    @Test
    fun `instructor registration and profile APIs support approval and public query`() {
        val owner = userRepository.save(User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = Instant.now()))
        val instructor = userRepository.save(User.create(displayName = "Guide", email = "guide@test.com", passwordHash = "hash", now = Instant.now()))

        mockMvc.perform(
            post("/operator/organizations")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"slug":"seoul-team","name":"Seoul Team","timezone":"Asia/Seoul"}""")
        ).andExpect(status().isCreated)

        val organizationId = requireNotNull(organizationRepository.findBySlug("seoul-team")?.id)

        mockMvc.perform(
            post("/instructor-registrations")
                .header("X-Actor-User-Id", requireNotNull(instructor.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{
                      "organizationId":$organizationId,
                      "headline":"City storyteller",
                      "languages":["ko","en"],
                      "specialties":["history"]
                    }"""
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("PENDING"))

        val registrationId = requireNotNull(
            instructorRegistrationRepository.findByOrganizationIdAndUserId(organizationId, requireNotNull(instructor.id))?.id
        )

        mockMvc.perform(
            post("/instructor-registrations/$registrationId/approve")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("APPROVED"))

        mockMvc.perform(
            get("/me/instructor-profile")
                .header("X-Actor-User-Id", requireNotNull(instructor.id))
                .param("organizationId", organizationId.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.headline").value("City storyteller"))

        mockMvc.perform(
            patch("/me/instructor-profile")
                .header("X-Actor-User-Id", requireNotNull(instructor.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{
                      "organizationId":$organizationId,
                      "headline":"Lead storyteller",
                      "bio":"Night specialist",
                      "languages":["ko","en"],
                      "specialties":["history","food"],
                      "certifications":["first aid"],
                      "yearsOfExperience":6,
                      "internalNote":"private"
                    }"""
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.certifications[0]").value("first aid"))
            .andExpect(jsonPath("$.internalNote").value("private"))

        val profileId = requireNotNull(
            instructorProfileRepository.findByOrganizationIdAndUserId(organizationId, requireNotNull(instructor.id))?.id
        )

        mockMvc.perform(get("/instructors/$profileId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.displayName").value("Guide"))
            .andExpect(jsonPath("$.certifications").doesNotExist())
    }
}
