package com.demo.tourwave.application.topology

import com.demo.tourwave.adapter.out.persistence.booking.InMemoryBookingRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.occurrence.InMemoryOccurrenceRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.topology.InMemoryInstructorProfileRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.topology.InMemoryOrganizationMembershipRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.topology.InMemoryOrganizationRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.topology.InMemoryTourRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.user.UserQueryAdapter
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.instructor.InstructorProfile
import com.demo.tourwave.domain.user.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class OccurrenceCommandServiceTest {
    private val clock = Clock.fixed(Instant.parse("2026-03-18T00:00:00Z"), ZoneOffset.UTC)
    private val occurrenceRepository = InMemoryOccurrenceRepositoryAdapter()
    private val bookingRepository = InMemoryBookingRepositoryAdapter()
    private val organizationRepository = InMemoryOrganizationRepositoryAdapter()
    private val membershipRepository = InMemoryOrganizationMembershipRepositoryAdapter()
    private val instructorProfileRepository = InMemoryInstructorProfileRepositoryAdapter()
    private val tourRepository = InMemoryTourRepositoryAdapter()
    private val userRepository = UserQueryAdapter()
    private val accessGuard = OrganizationAccessGuard(organizationRepository, membershipRepository)
    private val organizationCommandService =
        OrganizationCommandService(
            organizationRepository = organizationRepository,
            membershipRepository = membershipRepository,
            userRepository = userRepository,
            organizationAccessGuard = accessGuard,
            clock = clock,
        )
    private val tourCommandService =
        TourCommandService(
            tourRepository = tourRepository,
            organizationRepository = organizationRepository,
            organizationAccessGuard = accessGuard,
            clock = clock,
        )
    private val occurrenceCommandService =
        OccurrenceCommandService(
            occurrenceRepository = occurrenceRepository,
            bookingRepository = bookingRepository,
            tourRepository = tourRepository,
            instructorProfileRepository = instructorProfileRepository,
            organizationAccessGuard = accessGuard,
            clock = clock,
        )

    @BeforeEach
    fun setUp() {
        occurrenceRepository.clear()
        bookingRepository.clear()
        instructorProfileRepository.clear()
        tourRepository.clear()
        membershipRepository.clear()
        organizationRepository.clear()
        userRepository.clear()
    }

    @Test
    fun `operator can create update and reschedule occurrence`() {
        val owner =
            userRepository.save(
                User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = clock.instant()),
            )
        val organization =
            organizationCommandService.createOrganization(
                CreateOrganizationCommand(
                    actorUserId = requireNotNull(owner.id),
                    slug = "seoul-occ",
                    name = "Seoul Occ",
                    timezone = "Asia/Seoul",
                ),
            )
        val tour =
            tourCommandService.create(
                CreateTourCommand(
                    actorUserId = requireNotNull(owner.id),
                    organizationId = requireNotNull(organization.id),
                    title = "Night Walk",
                ),
            )
        val profile =
            instructorProfileRepository.save(
                InstructorProfile.create(
                    organizationId = requireNotNull(organization.id),
                    userId = 77L,
                    headline = "Lead guide",
                    bio = "Guide",
                    languages = listOf("ko"),
                    specialties = listOf("history"),
                    certifications = listOf("first aid"),
                    yearsOfExperience = 3,
                    internalNote = null,
                    approvedAt = clock.instant(),
                    now = clock.instant(),
                ),
            )

        val created =
            occurrenceCommandService.create(
                CreateOccurrenceCommand(
                    actorUserId = requireNotNull(owner.id),
                    tourId = requireNotNull(tour.id),
                    instructorProfileId = requireNotNull(profile.id),
                    capacity = 12,
                    startsAtUtc = Instant.parse("2026-04-01T01:00:00Z"),
                    endsAtUtc = Instant.parse("2026-04-01T04:00:00Z"),
                    timezone = "Asia/Seoul",
                    unitPrice = 42000,
                    currency = "krw",
                    locationText = "Jongno",
                    meetingPoint = "Exit 1",
                ),
            )
        val updated =
            occurrenceCommandService.update(
                UpdateOccurrenceCommand(
                    actorUserId = requireNotNull(owner.id),
                    occurrenceId = created.id,
                    instructorProfileId = requireNotNull(profile.id),
                    capacity = 14,
                    startsAtUtc = Instant.parse("2026-04-01T02:00:00Z"),
                    endsAtUtc = Instant.parse("2026-04-01T05:00:00Z"),
                    timezone = "Asia/Seoul",
                    locationText = "Jung-gu",
                    meetingPoint = "Lobby",
                ),
            )
        val rescheduled =
            occurrenceCommandService.reschedule(
                RescheduleOccurrenceCommand(
                    actorUserId = requireNotNull(owner.id),
                    occurrenceId = created.id,
                    startsAtUtc = Instant.parse("2026-04-02T02:00:00Z"),
                    endsAtUtc = Instant.parse("2026-04-02T05:00:00Z"),
                    timezone = "Asia/Seoul",
                    locationText = "Myeongdong",
                    meetingPoint = "Gate A",
                ),
            )

        assertEquals(42000, created.unitPrice)
        assertEquals("KRW", created.currency)
        assertEquals(14, updated.capacity)
        assertEquals("Jung-gu", updated.locationText)
        assertEquals("Myeongdong", rescheduled.locationText)
    }

    @Test
    fun `cannot shrink capacity below occupied seats`() {
        val owner =
            userRepository.save(
                User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = clock.instant()),
            )
        val organization =
            organizationCommandService.createOrganization(
                CreateOrganizationCommand(
                    actorUserId = requireNotNull(owner.id),
                    slug = "seoul-cap",
                    name = "Seoul Cap",
                    timezone = "Asia/Seoul",
                ),
            )
        val tour =
            tourCommandService.create(
                CreateTourCommand(
                    actorUserId = requireNotNull(owner.id),
                    organizationId = requireNotNull(organization.id),
                    title = "Capacity Tour",
                ),
            )
        val occurrence =
            occurrenceCommandService.create(
                CreateOccurrenceCommand(
                    actorUserId = requireNotNull(owner.id),
                    tourId = requireNotNull(tour.id),
                    capacity = 10,
                    startsAtUtc = Instant.parse("2026-04-03T01:00:00Z"),
                    endsAtUtc = Instant.parse("2026-04-03T03:00:00Z"),
                    timezone = "Asia/Seoul",
                    unitPrice = 30000,
                    currency = "KRW",
                ),
            )
        bookingRepository.save(
            Booking(
                occurrenceId = occurrence.id,
                organizationId = requireNotNull(organization.id),
                leaderUserId = 200L,
                partySize = 4,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = clock.instant(),
            ),
        )

        assertThrows(DomainException::class.java) {
            occurrenceCommandService.update(
                UpdateOccurrenceCommand(
                    actorUserId = requireNotNull(owner.id),
                    occurrenceId = occurrence.id,
                    capacity = 3,
                    startsAtUtc = Instant.parse("2026-04-03T01:00:00Z"),
                    endsAtUtc = Instant.parse("2026-04-03T03:00:00Z"),
                    timezone = "Asia/Seoul",
                ),
            )
        }
    }
}
