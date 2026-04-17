package com.demo.tourwave.adapter.`in`.web.occurrence

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.review.port.ReviewRepository
import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.tour.port.TourRepository
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.review.Review
import com.demo.tourwave.domain.review.ReviewType
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest
@AutoConfigureMockMvc
class OccurrenceCatalogControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var membershipRepository: OrganizationMembershipRepository

    @Autowired
    private lateinit var tourRepository: TourRepository

    @Autowired
    private lateinit var occurrenceRepository: OccurrenceRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    private lateinit var reviewRepository: ReviewRepository

    @BeforeEach
    fun setUp() {
        reviewRepository.clear()
        bookingRepository.clear()
        occurrenceRepository.clear()
        tourRepository.clear()
        membershipRepository.clear()
        organizationRepository.clear()
        userRepository.clear()
    }

    @Test
    fun `occurrence operator and catalog APIs support authoring and public discovery`() {
        val baseDay = Instant.now().plus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS)
        val createStart = baseDay.plus(10, ChronoUnit.HOURS)
        val createEnd = baseDay.plus(12, ChronoUnit.HOURS)
        val patchStart = baseDay.plus(11, ChronoUnit.HOURS)
        val patchEnd = baseDay.plus(13, ChronoUnit.HOURS)
        val rescheduleStart = baseDay.plus(1, ChronoUnit.DAYS).plus(11, ChronoUnit.HOURS)
        val rescheduleEnd = baseDay.plus(1, ChronoUnit.DAYS).plus(13, ChronoUnit.HOURS)

        val owner = userRepository.save(User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = Instant.now()))

        mockMvc.perform(
            post("/operator/organizations")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"slug":"catalog-team","name":"Catalog Team","timezone":"Asia/Seoul"}""")
        ).andExpect(status().isCreated)
        val organizationId = requireNotNull(organizationRepository.findBySlug("catalog-team")?.id)

        mockMvc.perform(
            post("/organizations/$organizationId/tours")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Seoul Night Walk","summary":"Downtown route"}""")
        ).andExpect(status().isCreated)
        val tourId = requireNotNull(tourRepository.findByOrganizationId(organizationId).single().id)

        mockMvc.perform(
            post("/tours/$tourId/occurrences")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{
                      "capacity":12,
                      "startsAtUtc":"$createStart",
                      "endsAtUtc":"$createEnd",
                      "timezone":"Asia/Seoul",
                      "unitPrice":50000,
                      "currency":"KRW",
                      "locationText":"Seoul Station",
                      "meetingPoint":"Platform 1"
                    }"""
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.unitPrice").value(50000))

        val occurrence = occurrenceRepository.findByTourId(tourId).single()

        mockMvc.perform(
            patch("/occurrences/${occurrence.id}")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{
                      "capacity":15,
                      "startsAtUtc":"$patchStart",
                      "endsAtUtc":"$patchEnd",
                      "timezone":"Asia/Seoul",
                      "locationText":"Myeongdong",
                      "meetingPoint":"Gate B"
                    }"""
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.capacity").value(15))

        mockMvc.perform(
            post("/occurrences/${occurrence.id}/reschedule")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{
                      "startsAtUtc":"$rescheduleStart",
                      "endsAtUtc":"$rescheduleEnd",
                      "timezone":"Asia/Seoul",
                      "locationText":"Euljiro",
                      "meetingPoint":"Gate C"
                    }"""
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.locationText").value("Euljiro"))

        mockMvc.perform(
            put("/tours/$tourId/content")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{
                      "description":"Night route",
                      "highlights":["night market"],
                      "inclusions":["tea"],
                      "exclusions":["transport"],
                      "preparations":["walking shoes"],
                      "policies":["48h refund"]
                    }"""
                )
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/tours/$tourId/publish")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
        ).andExpect(status().isOk)

        bookingRepository.save(
            Booking(
                occurrenceId = occurrence.id,
                organizationId = organizationId,
                leaderUserId = 300L,
                partySize = 4,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-20T00:00:00Z")
            )
        )
        reviewRepository.save(
            Review(
                occurrenceId = occurrence.id,
                reviewerUserId = 300L,
                type = ReviewType.TOUR,
                rating = 5,
                comment = "great",
                createdAt = Instant.parse("2026-04-12T00:00:00Z")
            )
        )

        mockMvc.perform(get("/tours"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].title").value("Seoul Night Walk"))

        mockMvc.perform(get("/tours/$tourId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.description").value("Night route"))

        mockMvc.perform(get("/tours/$tourId/occurrences"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].locationText").value("Euljiro"))

        mockMvc.perform(get("/occurrences/${occurrence.id}/availability").param("partySize", "5"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.confirmedCount").value(4))
            .andExpect(jsonPath("$.canConfirm").value(true))

        mockMvc.perform(get("/occurrences/${occurrence.id}/quote").param("partySize", "5"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalPrice").value(250000))

        mockMvc.perform(get("/search/occurrences").param("locationText", "eulji").param("partySize", "5").param("onlyAvailable", "true"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].ratingSummary.reviewCount").value(1))
    }
}
