package com.demo.tourwave.application.reporting

import com.demo.tourwave.adapter.out.persistence.booking.InMemoryBookingRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.occurrence.InMemoryOccurrenceRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.participant.InMemoryBookingParticipantRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.payment.InMemoryPaymentRecordRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.organization.InMemoryOrganizationMembershipRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.organization.InMemoryOrganizationRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.tour.InMemoryTourRepositoryAdapter
import com.demo.tourwave.application.organization.OrganizationAccessGuard
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrganizationReportServiceTest {
    private val bookingRepository = InMemoryBookingRepositoryAdapter()
    private val occurrenceRepository = InMemoryOccurrenceRepositoryAdapter()
    private val bookingParticipantRepository = InMemoryBookingParticipantRepositoryAdapter()
    private val paymentRecordRepository = InMemoryPaymentRecordRepositoryAdapter()
    private val tourRepository = InMemoryTourRepositoryAdapter()
    private val organizationRepository = InMemoryOrganizationRepositoryAdapter()
    private val membershipRepository = InMemoryOrganizationMembershipRepositoryAdapter()
    private val service = OrganizationReportService(
        bookingRepository = bookingRepository,
        occurrenceRepository = occurrenceRepository,
        bookingParticipantRepository = bookingParticipantRepository,
        paymentRecordRepository = paymentRecordRepository,
        tourRepository = tourRepository,
        organizationAccessGuard = OrganizationAccessGuard(organizationRepository, membershipRepository)
    )

    @BeforeEach
    fun setUp() {
        bookingRepository.clear()
        occurrenceRepository.clear()
        bookingParticipantRepository.clear()
        paymentRecordRepository.clear()
        tourRepository.clear()
        membershipRepository.clear()
        organizationRepository.clear()

        organizationRepository.save(
            Organization.create(
                slug = "reporting-org",
                name = "Reporting Org",
                description = null,
                publicDescription = null,
                contactEmail = null,
                contactPhone = null,
                websiteUrl = null,
                businessName = null,
                businessRegistrationNumber = null,
                timezone = "Asia/Seoul",
                now = Instant.parse("2026-03-01T00:00:00Z")
            )
        )
        membershipRepository.save(
            OrganizationMembership.active(
                organizationId = 1L,
                userId = 7L,
                role = OrganizationRole.ADMIN,
                now = Instant.parse("2026-03-01T00:00:00Z")
            )
        )
        val tour = tourRepository.save(
            Tour.create(
                organizationId = 1L,
                title = "Night Walk",
                summary = "Summary",
                now = Instant.parse("2026-03-01T00:00:00Z")
            )
        )
        occurrenceRepository.save(
            Occurrence(
                id = 101L,
                organizationId = 1L,
                tourId = requireNotNull(tour.id),
                capacity = 10,
                startsAtUtc = Instant.parse("2026-03-20T09:00:00Z"),
                status = com.demo.tourwave.domain.occurrence.OccurrenceStatus.SCHEDULED,
                createdAt = Instant.parse("2026-03-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-03-01T00:00:00Z")
            )
        )
        val confirmed = bookingRepository.save(
            Booking(
                occurrenceId = 101L,
                organizationId = 1L,
                leaderUserId = 1001L,
                partySize = 3,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-18T01:00:00Z")
            )
        )
        val waitlisted = bookingRepository.save(
            Booking(
                occurrenceId = 101L,
                organizationId = 1L,
                leaderUserId = 1002L,
                partySize = 2,
                status = BookingStatus.WAITLISTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-18T02:00:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(confirmed.id),
                userId = 1001L,
                createdAt = Instant.parse("2026-03-18T01:00:00Z")
            ).recordAttendance(AttendanceStatus.ATTENDED)
        )
        bookingParticipantRepository.save(
            BookingParticipant(
                bookingId = requireNotNull(confirmed.id),
                userId = 1003L,
                status = BookingParticipantStatus.ACCEPTED,
                attendanceStatus = AttendanceStatus.NO_SHOW,
                createdAt = Instant.parse("2026-03-18T01:00:00Z")
            )
        )
        paymentRecordRepository.save(
            PaymentRecord(
                bookingId = requireNotNull(confirmed.id),
                status = PaymentRecordStatus.REFUNDED,
                createdAtUtc = Instant.parse("2026-03-18T01:00:00Z"),
                updatedAtUtc = Instant.parse("2026-03-18T01:05:00Z")
            )
        )
        paymentRecordRepository.save(
            PaymentRecord(
                bookingId = requireNotNull(waitlisted.id),
                status = PaymentRecordStatus.REFUND_FAILED_RETRYABLE,
                createdAtUtc = Instant.parse("2026-03-18T02:00:00Z"),
                updatedAtUtc = Instant.parse("2026-03-18T02:05:00Z")
            )
        )
    }

    @Test
    fun `booking report applies filters and includes refund status`() {
        val page = service.getBookingReport(
            BookingReportQuery(
                actorUserId = 7L,
                organizationId = 1L,
                dateFrom = java.time.LocalDate.parse("2026-03-18"),
                dateTo = java.time.LocalDate.parse("2026-03-18"),
                tourId = null,
                occurrenceId = 101L,
                cursor = null,
                limit = 20
            )
        )

        assertEquals(2, page.items.size)
        assertEquals(PaymentRecordStatus.REFUND_FAILED_RETRYABLE, page.items.first().refundStatus)
        assertTrue(service.exportBookingReportCsv(
            BookingReportQuery(7L, 1L, null, null, null, null, null, 100)
        ).contains("refundStatus"))
    }

    @Test
    fun `occurrence ops report includes utilization attendance and refund signals`() {
        val page = service.getOccurrenceOpsReport(
            OccurrenceOpsReportQuery(
                actorUserId = 7L,
                organizationId = 1L,
                dateFrom = java.time.LocalDate.parse("2026-03-20"),
                dateTo = java.time.LocalDate.parse("2026-03-20"),
                tourId = null,
                occurrenceId = 101L,
                cursor = null,
                limit = 20
            )
        )

        val item = page.items.single()
        assertEquals(30, item.seatUtilizationPercent)
        assertEquals(1, item.attendedCount)
        assertEquals(1, item.noShowCount)
        assertEquals(1, item.refundedBookingCount)
        assertEquals(1, item.refundPendingCount)
        assertTrue(service.exportOccurrenceOpsReportCsv(
            OccurrenceOpsReportQuery(7L, 1L, null, null, null, null, null, 100)
        ).contains("seatUtilizationPercent"))
    }
}
