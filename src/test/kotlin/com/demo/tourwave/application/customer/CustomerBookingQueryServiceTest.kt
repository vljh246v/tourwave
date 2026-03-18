package com.demo.tourwave.application.customer

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.port.ActorAuthContext
import com.demo.tourwave.application.participant.ParticipantAccessPolicy
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.application.topology.port.OrganizationRepository
import com.demo.tourwave.application.topology.port.TourRepository
import com.demo.tourwave.domain.booking.AttendanceStatus
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.occurrence.OccurrenceStatus
import com.demo.tourwave.domain.organization.Organization
import com.demo.tourwave.domain.participant.BookingParticipant
import com.demo.tourwave.domain.participant.BookingParticipantStatus
import com.demo.tourwave.domain.tour.Tour
import com.demo.tourwave.support.FakeOrganizationRepository
import com.demo.tourwave.support.FakeTourRepository
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomerBookingQueryServiceTest {
    private val bookingRepository: BookingRepository =
        com.demo.tourwave.adapter.out.persistence.booking
            .InMemoryBookingRepositoryAdapter()
    private val occurrenceRepository: OccurrenceRepository =
        com.demo.tourwave.adapter.out.persistence.occurrence
            .InMemoryOccurrenceRepositoryAdapter()
    private val bookingParticipantRepository: BookingParticipantRepository =
        com.demo.tourwave.adapter.out.persistence.participant
            .InMemoryBookingParticipantRepositoryAdapter()
    private val tourRepository: TourRepository = FakeTourRepository()
    private val organizationRepository: OrganizationRepository = FakeOrganizationRepository()
    private val participantAccessPolicy = ParticipantAccessPolicy(bookingRepository, bookingParticipantRepository)
    private val service =
        CustomerBookingQueryService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            bookingParticipantRepository = bookingParticipantRepository,
            participantAccessPolicy = participantAccessPolicy,
            tourRepository = tourRepository,
            organizationRepository = organizationRepository,
        )

    @Test
    fun `my bookings list merges leader and participant bookings and generates calendars`() {
        organizationRepository.save(
            Organization.create(
                slug = "cust-org",
                name = "Customer Org",
                description = null,
                publicDescription = null,
                contactEmail = null,
                contactPhone = null,
                websiteUrl = null,
                businessName = null,
                businessRegistrationNumber = null,
                timezone = "Asia/Seoul",
                now = Instant.parse("2026-03-18T00:00:00Z"),
            ),
        )
        tourRepository.save(
            Tour
                .create(
                    organizationId = 1L,
                    title = "Night Walk",
                    summary = "City route",
                    now = Instant.parse("2026-03-18T00:00:00Z"),
                ).copy(id = 20L, status = com.demo.tourwave.domain.tour.TourStatus.PUBLISHED),
        )
        occurrenceRepository.save(
            Occurrence(
                id = 30L,
                organizationId = 1L,
                tourId = 20L,
                capacity = 10,
                startsAtUtc = Instant.parse("2026-04-10T10:00:00Z"),
                endsAtUtc = Instant.parse("2026-04-10T12:00:00Z"),
                timezone = "Asia/Seoul",
                unitPrice = 50000,
                currency = "KRW",
                locationText = "Seoul Station",
                meetingPoint = "Gate A",
                status = OccurrenceStatus.SCHEDULED,
                createdAt = Instant.parse("2026-03-18T00:00:00Z"),
                updatedAt = Instant.parse("2026-03-18T00:00:00Z"),
            ),
        )
        val leaderBooking =
            bookingRepository.save(
                Booking(
                    occurrenceId = 30L,
                    organizationId = 1L,
                    leaderUserId = 100L,
                    partySize = 2,
                    status = BookingStatus.CONFIRMED,
                    paymentStatus = PaymentStatus.PAID,
                    createdAt = Instant.parse("2026-03-19T00:00:00Z"),
                ),
            )
        val participantBooking =
            bookingRepository.save(
                Booking(
                    occurrenceId = 30L,
                    organizationId = 1L,
                    leaderUserId = 200L,
                    partySize = 3,
                    status = BookingStatus.CONFIRMED,
                    paymentStatus = PaymentStatus.PAID,
                    createdAt = Instant.parse("2026-03-20T00:00:00Z"),
                ),
            )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(leaderBooking.id),
                userId = 100L,
                createdAt = Instant.parse("2026-03-19T00:00:00Z"),
            ),
        )
        bookingParticipantRepository.save(
            BookingParticipant(
                bookingId = requireNotNull(participantBooking.id),
                userId = 100L,
                status = BookingParticipantStatus.ACCEPTED,
                attendanceStatus = AttendanceStatus.UNKNOWN,
                invitedAt = Instant.parse("2026-03-20T00:00:00Z"),
                respondedAt = Instant.parse("2026-03-20T01:00:00Z"),
                createdAt = Instant.parse("2026-03-20T00:00:00Z"),
            ),
        )

        val bookings = service.listMyBookings(100L)
        val bookingCalendar = service.bookingCalendar(requireNotNull(leaderBooking.id), ActorAuthContext(actorUserId = 100L))
        val occurrenceCalendar = service.occurrenceCalendar(30L)

        assertEquals(2, bookings.size)
        assertEquals("Night Walk", bookings.first().tourTitle)
        assertTrue(bookingCalendar.body.contains("BEGIN:VCALENDAR"))
        assertTrue(bookingCalendar.body.contains("SUMMARY:Night Walk"))
        assertTrue(occurrenceCalendar.body.contains("Night Walk"))
    }
}
