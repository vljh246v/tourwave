package com.demo.tourwave.adapter.`in`.web.communication

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.announcement.port.AnnouncementRepository
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.tour.port.TourRepository
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.announcement.Announcement
import com.demo.tourwave.domain.announcement.AnnouncementVisibility
import com.demo.tourwave.domain.booking.AttendanceStatus
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.organization.Organization
import com.demo.tourwave.domain.organization.OrganizationMembership
import com.demo.tourwave.domain.organization.OrganizationRole
import com.demo.tourwave.domain.participant.BookingParticipant
import com.demo.tourwave.domain.participant.BookingParticipantStatus
import com.demo.tourwave.domain.payment.PaymentRecord
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import com.demo.tourwave.domain.tour.Tour
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest
@AutoConfigureMockMvc
class CommunicationReportingIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var organizationMembershipRepository: OrganizationMembershipRepository

    @Autowired
    private lateinit var announcementRepository: AnnouncementRepository

    @Autowired
    private lateinit var tourRepository: TourRepository

    @Autowired
    private lateinit var occurrenceRepository: OccurrenceRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    private lateinit var bookingParticipantRepository: BookingParticipantRepository

    @Autowired
    private lateinit var paymentRecordRepository: PaymentRecordRepository

    @BeforeEach
    fun setUp() {
        paymentRecordRepository.clear()
        bookingParticipantRepository.clear()
        bookingRepository.clear()
        occurrenceRepository.clear()
        tourRepository.clear()
        announcementRepository.clear()
        organizationMembershipRepository.clear()
        organizationRepository.clear()
        userRepository.clear()
    }

    @Test
    fun `announcement and report endpoints enforce authz and expose csv export`() {
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)

        val owner = userRepository.save(User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = now))
        val member = userRepository.save(User.create(displayName = "Member", email = "member@test.com", passwordHash = "hash", now = now))
        val organization = organizationRepository.save(
            Organization.create(
                slug = "comm-report",
                name = "Comm Report",
                description = null,
                publicDescription = null,
                contactEmail = null,
                contactPhone = null,
                websiteUrl = null,
                businessName = null,
                businessRegistrationNumber = null,
                timezone = "Asia/Seoul",
                now = now
            )
        )
        organizationMembershipRepository.save(
            OrganizationMembership.active(
                organizationId = requireNotNull(organization.id),
                userId = requireNotNull(owner.id),
                role = OrganizationRole.OWNER,
                now = now
            )
        )
        organizationMembershipRepository.save(
            OrganizationMembership.active(
                organizationId = requireNotNull(organization.id),
                userId = requireNotNull(member.id),
                role = OrganizationRole.MEMBER,
                now = now
            )
        )

        val announcementStart = now.minus(1, ChronoUnit.DAYS)
        val announcementEnd = now.plus(3, ChronoUnit.DAYS)

        val createResponse = mockMvc.perform(
            post("/organizations/${organization.id}/announcements")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{
                      "title":"Operational notice",
                      "body":"Bring water.",
                      "visibility":"PUBLIC",
                      "publishStartsAtUtc":"$announcementStart",
                      "publishEndsAtUtc":"$announcementEnd"
                    }"""
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.visibility").value("PUBLIC"))
            .andReturn()

        val announcementId = Regex("\"id\":(\\d+)").find(createResponse.response.contentAsString)?.groupValues?.get(1)?.toLong()
            ?: error("announcement id missing")

        mockMvc.perform(
            get("/public/announcements")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].title").value("Operational notice"))

        mockMvc.perform(
            get("/operator/organizations/${organization.id}/announcements")
                .header("X-Actor-User-Id", requireNotNull(member.id))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))

        mockMvc.perform(
            patch("/announcements/$announcementId")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Updated notice","visibility":"INTERNAL"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Updated notice"))

        mockMvc.perform(get("/public/announcements"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items").isEmpty)

        val tourCreatedAt = now.minus(2, ChronoUnit.DAYS)
        val occurrenceStartsAt = now.plus(2, ChronoUnit.DAYS)
        val bookingCreatedAt = now.minus(1, ChronoUnit.DAYS)

        val tour = tourRepository.save(
            Tour.create(
                organizationId = requireNotNull(organization.id),
                title = "Evening Walk",
                summary = "Summary",
                now = tourCreatedAt
            )
        )
        occurrenceRepository.save(
            Occurrence(
                id = 3001L,
                organizationId = requireNotNull(organization.id),
                tourId = requireNotNull(tour.id),
                capacity = 8,
                startsAtUtc = occurrenceStartsAt,
                status = com.demo.tourwave.domain.occurrence.OccurrenceStatus.SCHEDULED,
                createdAt = tourCreatedAt,
                updatedAt = tourCreatedAt
            )
        )
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 3001L,
                organizationId = requireNotNull(organization.id),
                leaderUserId = requireNotNull(owner.id),
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = bookingCreatedAt
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = requireNotNull(owner.id),
                createdAt = bookingCreatedAt
            ).recordAttendance(AttendanceStatus.ATTENDED)
        )
        bookingParticipantRepository.save(
            BookingParticipant(
                bookingId = requireNotNull(booking.id),
                userId = requireNotNull(member.id),
                status = BookingParticipantStatus.ACCEPTED,
                attendanceStatus = AttendanceStatus.NO_SHOW,
                createdAt = bookingCreatedAt
            )
        )
        paymentRecordRepository.save(
            PaymentRecord(
                bookingId = requireNotNull(booking.id),
                status = PaymentRecordStatus.REFUND_PENDING,
                createdAtUtc = bookingCreatedAt,
                updatedAtUtc = bookingCreatedAt.plus(5, ChronoUnit.MINUTES)
            )
        )

        mockMvc.perform(
            get("/organizations/${organization.id}/reports/bookings")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .param("tourId", requireNotNull(tour.id).toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].bookingId").value(booking.id!!.toInt()))

        mockMvc.perform(
            get("/organizations/${organization.id}/reports/occurrences")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
                .param("tourId", requireNotNull(tour.id).toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].seatUtilizationPercent").value(25))
            .andExpect(jsonPath("$.items[0].refundPendingCount").value(1))

        mockMvc.perform(
            get("/organizations/${organization.id}/reports/occurrences/export")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"organization-${organization.id}-occurrences.csv\""))
            .andExpect(content().contentType("text/csv"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("seatUtilizationPercent")))

        mockMvc.perform(
            delete("/announcements/$announcementId")
                .header("X-Actor-User-Id", requireNotNull(owner.id))
        )
            .andExpect(status().isNoContent)
    }
}
