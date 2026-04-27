package com.demo.tourwave.adapter.`in`.web.tour

import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.tour.port.TourRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
class TourControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var tourRepository: TourRepository

    @BeforeEach
    fun setUp() {
        tourRepository.clear()
        organizationRepository.clear()
        userRepository.clear()
    }

    @Test
    fun `tour APIs support operator authoring publish and public content query`() {
        val owner =
            userRepository.save(
                User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = Instant.now()),
            )

        mockMvc.perform(
            post("/operator/organizations")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .header("Idempotency-Key", "tour-create-org-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"slug":"jeju-team","name":"Jeju Team","timezone":"Asia/Seoul"}"""),
        ).andExpect(status().isCreated)
        val organizationId = requireNotNull(organizationRepository.findBySlug("jeju-team")?.id)

        mockMvc.perform(
            post("/organizations/$organizationId/tours")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Jeju Coast Walk","summary":"Ocean route"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("DRAFT"))

        val tourId = requireNotNull(tourRepository.findByOrganizationId(organizationId).single().id)

        mockMvc.perform(
            put("/tours/$tourId/content")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{
                      "description":"Coastal content",
                      "highlights":["sunrise"],
                      "inclusions":["tea"],
                      "exclusions":["transport"],
                      "preparations":["walking shoes"],
                      "policies":["24h cancellation"]
                    }""",
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.highlights[0]").value("sunrise"))

        mockMvc.perform(
            post("/tours/$tourId/publish")
                .header("X-Actor-User-Id", requireNotNull(owner.id)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PUBLISHED"))

        mockMvc.perform(
            get("/organizations/$organizationId/tours")
                .header("X-Actor-User-Id", requireNotNull(owner.id)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].title").value("Jeju Coast Walk"))

        mockMvc.perform(get("/tours/$tourId/content"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.description").value("Coastal content"))
    }
}
