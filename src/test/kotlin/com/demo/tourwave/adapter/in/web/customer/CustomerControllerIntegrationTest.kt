package com.demo.tourwave.adapter.`in`.web.customer

import com.demo.tourwave.application.asset.port.AssetRepository
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.customer.port.FavoriteRepository
import com.demo.tourwave.application.customer.port.NotificationRepository
import com.demo.tourwave.application.topology.port.OrganizationMembershipRepository
import com.demo.tourwave.application.topology.port.OrganizationRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class CustomerControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var organizationMembershipRepository: OrganizationMembershipRepository

    @Autowired
    private lateinit var tourRepository: TourRepository

    @Autowired
    private lateinit var occurrenceRepository: OccurrenceRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    private lateinit var assetRepository: AssetRepository

    @Autowired
    private lateinit var favoriteRepository: FavoriteRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @BeforeEach
    fun setUp() {
        notificationRepository.clear()
        favoriteRepository.clear()
        bookingRepository.clear()
        occurrenceRepository.clear()
        tourRepository.clear()
        organizationMembershipRepository.clear()
        organizationRepository.clear()
        assetRepository.clear()
        userRepository.clear()
    }

    @Test
    fun `customer surface exposes assets bookings calendars favorites and notifications`() {
        val owner = userRepository.save(User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash"))
        val customer = userRepository.save(User.create(displayName = "Customer", email = "customer@test.com", passwordHash = "hash"))

        mockMvc
            .perform(
                post("/operator/organizations")
                    .header("X-Actor-User-Id", requireNotNull(owner.id))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"slug":"cust-s12","name":"Customer Surface","timezone":"Asia/Seoul"}"""),
            ).andExpect(status().isCreated)
        val organizationId = requireNotNull(organizationRepository.findBySlug("cust-s12")?.id)

        mockMvc
            .perform(
                post("/organizations/$organizationId/tours")
                    .header("X-Actor-User-Id", requireNotNull(owner.id))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"title":"Han River Sunset","summary":"Evening route"}"""),
            ).andExpect(status().isCreated)
        val tourId = requireNotNull(tourRepository.findByOrganizationId(organizationId).single().id)

        mockMvc
            .perform(
                post("/tours/$tourId/occurrences")
                    .header("X-Actor-User-Id", requireNotNull(owner.id))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                      "capacity":10,
                      "startsAtUtc":"2026-04-20T09:00:00Z",
                      "endsAtUtc":"2026-04-20T11:00:00Z",
                      "timezone":"Asia/Seoul",
                      "unitPrice":42000,
                      "currency":"KRW",
                      "locationText":"Han River Park",
                      "meetingPoint":"Dock 3"
                    }""",
                    ),
            ).andExpect(status().isCreated)
        val occurrenceId = requireNotNull(occurrenceRepository.findByTourId(tourId).single().id)

        val uploadResult =
            mockMvc
                .perform(
                    post("/assets/uploads")
                        .header("X-Actor-User-Id", requireNotNull(owner.id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"organizationId":$organizationId,"fileName":"cover.jpg","contentType":"image/jpeg"}"""),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.status").value("UPLOADING"))
                .andReturn()
        val assetId =
            requireNotNull(
                com.fasterxml.jackson.module.kotlin
                    .jacksonObjectMapper()
                    .readTree(uploadResult.response.contentAsString)
                    .get("id"),
            ).asLong()

        mockMvc
            .perform(
                post("/assets/$assetId/complete")
                    .header("X-Actor-User-Id", requireNotNull(owner.id))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"sizeBytes":1234,"checksumSha256":"${"a".repeat(64)}"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("READY"))

        mockMvc
            .perform(
                put("/operator/organizations/$organizationId/assets")
                    .header("X-Actor-User-Id", requireNotNull(owner.id))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"assetIds":[$assetId]}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.assetIds[0]").value(assetId))

        mockMvc
            .perform(
                put("/tours/$tourId/assets")
                    .header("X-Actor-User-Id", requireNotNull(owner.id))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"assetIds":[$assetId]}"""),
            ).andExpect(status().isOk)

        mockMvc
            .perform(
                put("/tours/$tourId/content")
                    .header("X-Actor-User-Id", requireNotNull(owner.id))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"description":"Golden hour cruise"}"""),
            ).andExpect(status().isOk)

        mockMvc
            .perform(
                post("/tours/$tourId/publish")
                    .header("X-Actor-User-Id", requireNotNull(owner.id)),
            ).andExpect(status().isOk)

        mockMvc
            .perform(
                post("/tours/$tourId/favorite")
                    .header("X-Actor-User-Id", requireNotNull(customer.id)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.attachmentAssetIds[0]").value(assetId))

        mockMvc
            .perform(
                post("/occurrences/$occurrenceId/bookings")
                    .header("X-Actor-User-Id", requireNotNull(customer.id))
                    .header("Idempotency-Key", "book-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"partySize":2}"""),
            ).andExpect(status().isCreated)
        val bookingId = requireNotNull(bookingRepository.findByLeaderUserId(requireNotNull(customer.id)).single().id)

        mockMvc
            .perform(
                post("/occurrences/$occurrenceId/inquiries")
                    .header("X-Actor-User-Id", requireNotNull(customer.id))
                    .header("Idempotency-Key", "inq-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"bookingId":$bookingId,"subject":"Pickup","message":"Can I board early?"}"""),
            ).andExpect(status().isCreated)

        mockMvc
            .perform(
                get("/me/bookings")
                    .header("X-Actor-User-Id", requireNotNull(customer.id)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$[0].tourTitle").value("Han River Sunset"))

        mockMvc
            .perform(
                get("/bookings/$bookingId/calendar.ics")
                    .header("X-Actor-User-Id", requireNotNull(customer.id)),
            ).andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", """attachment; filename="booking-$bookingId.ics""""))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("BEGIN:VCALENDAR")))

        mockMvc
            .perform(get("/occurrences/$occurrenceId/calendar.ics"))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Han River Sunset")))

        mockMvc
            .perform(
                get("/me/favorites")
                    .header("X-Actor-User-Id", requireNotNull(customer.id)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$[0].title").value("Han River Sunset"))

        mockMvc
            .perform(
                get("/me/notifications")
                    .header("X-Actor-User-Id", requireNotNull(customer.id)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$[0].title").exists())

        val notificationId = requireNotNull(notificationRepository.findByUserId(requireNotNull(customer.id)).first().id)

        mockMvc
            .perform(
                post("/me/notifications/$notificationId/read")
                    .header("X-Actor-User-Id", requireNotNull(customer.id)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.readAt").exists())

        mockMvc
            .perform(
                post("/me/notifications/read-all")
                    .header("X-Actor-User-Id", requireNotNull(customer.id)),
            ).andExpect(status().isOk)

        mockMvc
            .perform(
                delete("/tours/$tourId/favorite")
                    .header("X-Actor-User-Id", requireNotNull(customer.id)),
            ).andExpect(status().isNoContent)
    }
}
