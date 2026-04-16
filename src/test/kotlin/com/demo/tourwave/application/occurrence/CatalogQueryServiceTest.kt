package com.demo.tourwave.application.occurrence

import com.demo.tourwave.adapter.out.persistence.booking.InMemoryBookingRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.occurrence.InMemoryOccurrenceRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.review.InMemoryReviewRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.tour.InMemoryTourRepositoryAdapter
import com.demo.tourwave.application.common.TimeWindowPolicyService
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.review.Review
import com.demo.tourwave.domain.review.ReviewType
import com.demo.tourwave.domain.tour.Tour
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class CatalogQueryServiceTest {
    private val tourRepository = InMemoryTourRepositoryAdapter()
    private val occurrenceRepository = InMemoryOccurrenceRepositoryAdapter()
    private val bookingRepository = InMemoryBookingRepositoryAdapter()
    private val reviewRepository = InMemoryReviewRepositoryAdapter()
    private val catalogQueryService = CatalogQueryService(
        tourRepository = tourRepository,
        occurrenceRepository = occurrenceRepository,
        bookingRepository = bookingRepository,
        reviewRepository = reviewRepository,
        timeWindowPolicyService = TimeWindowPolicyService()
    )

    @BeforeEach
    fun setUp() {
        reviewRepository.clear()
        bookingRepository.clear()
        occurrenceRepository.clear()
        tourRepository.clear()
    }

    @Test
    fun `catalog exposes published tours availability quote and search`() {
        val publishedTour = tourRepository.save(
            Tour.create(
                organizationId = 31L,
                title = "Seoul Night Walk",
                summary = "Downtown route",
                now = Instant.parse("2026-03-18T00:00:00Z")
            ).publish(Instant.parse("2026-03-18T01:00:00Z"))
        )
        val occurrence = Occurrence(
            id = occurrenceRepository.nextId(),
            organizationId = 31L,
            tourId = requireNotNull(publishedTour.id),
            capacity = 10,
            startsAtUtc = Instant.parse("2026-04-10T10:00:00Z"),
            endsAtUtc = Instant.parse("2026-04-10T12:00:00Z"),
            timezone = "Asia/Seoul",
            unitPrice = 50000,
            currency = "KRW",
            locationText = "Seoul Station",
            meetingPoint = "Platform 1",
            createdAt = Instant.parse("2026-03-18T00:00:00Z"),
            updatedAt = Instant.parse("2026-03-18T00:00:00Z")
        )
        occurrenceRepository.save(occurrence)
        bookingRepository.save(
            Booking(
                occurrenceId = occurrence.id,
                organizationId = 31L,
                leaderUserId = 101L,
                partySize = 3,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-20T00:00:00Z")
            )
        )
        bookingRepository.save(
            Booking(
                occurrenceId = occurrence.id,
                organizationId = 31L,
                leaderUserId = 102L,
                partySize = 2,
                status = BookingStatus.OFFERED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-20T00:00:00Z")
            )
        )
        reviewRepository.save(
            Review(
                occurrenceId = occurrence.id,
                reviewerUserId = 101L,
                type = ReviewType.TOUR,
                rating = 5,
                comment = "great",
                createdAt = Instant.parse("2026-04-11T00:00:00Z")
            )
        )

        val tours = catalogQueryService.listPublicTours(PublicTourListQuery(limit = 10)).first
        val occurrences = catalogQueryService.listPublicOccurrences(TourOccurrenceListQuery(requireNotNull(publishedTour.id)))
        val availability = catalogQueryService.getAvailability(AvailabilityQuery(occurrence.id, 4))
        val quote = catalogQueryService.getQuote(AvailabilityQuery(occurrence.id, 4))
        val search = catalogQueryService.search(
            OccurrenceSearchQuery(
                locationText = "station",
                partySize = 4,
                onlyAvailable = true
            )
        )

        assertEquals(1, tours.size)
        assertEquals(1, occurrences.size)
        assertEquals(3, availability.confirmedCount)
        assertEquals(2, availability.heldCount)
        assertEquals(5, availability.available)
        assertEquals(200000, quote.totalPrice)
        assertEquals(1, search.items.size)
        assertEquals(5.0, search.items.single().ratingSummary?.avgRating)
    }
}
