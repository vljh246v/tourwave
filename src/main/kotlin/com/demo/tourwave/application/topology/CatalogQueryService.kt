package com.demo.tourwave.application.topology

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.TimeWindowPolicyService
import com.demo.tourwave.application.organization.requireValidTimezone
import com.demo.tourwave.application.review.port.ReviewRepository
import com.demo.tourwave.application.topology.port.TourRepository
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.occurrence.OccurrenceStatus
import com.demo.tourwave.domain.review.ReviewType
import com.demo.tourwave.domain.tour.Tour
import com.demo.tourwave.domain.tour.TourStatus

class CatalogQueryService(
    private val tourRepository: TourRepository,
    private val occurrenceRepository: OccurrenceRepository,
    private val bookingRepository: BookingRepository,
    private val reviewRepository: ReviewRepository,
    private val timeWindowPolicyService: TimeWindowPolicyService,
) {
    fun listPublicTours(query: PublicTourListQuery): Pair<List<PublicTourView>, String?> {
        val cursor = parseCursor(query.cursor)
        val limit = requireValidLimit(query.limit)
        val searchTerm =
            query.q
                ?.trim()
                ?.lowercase()
                .takeUnless { it.isNullOrBlank() }
        val items =
            tourRepository
                .findAllPublished()
                .asSequence()
                .filter { cursor == null || requireNotNull(it.id) > cursor }
                .filter { query.organizationId == null || it.organizationId == query.organizationId }
                .filter { tour ->
                    searchTerm == null ||
                        listOfNotNull(
                            tour.title,
                            tour.summary,
                            tour.content.description,
                        ).any { it.lowercase().contains(searchTerm) }
                }.sortedBy { requireNotNull(it.id) }
                .toList()
        val page = items.take(limit)
        val nextCursor =
            items
                .drop(limit)
                .firstOrNull()
                ?.id
                ?.toString()
        return page.map { it.toPublicView() } to nextCursor
    }

    fun getPublicTour(tourId: Long): PublicTourView = requirePublishedTour(tourId).toPublicView()

    fun listPublicOccurrences(query: TourOccurrenceListQuery): List<PublicOccurrenceView> {
        requirePublishedTour(query.tourId)
        return occurrenceRepository
            .findByTourId(query.tourId)
            .asSequence()
            .filter { it.status == OccurrenceStatus.SCHEDULED }
            .filter { query.dateFrom == null || (it.startsAtUtc != null && !it.startsAtUtc.isBefore(query.dateFrom)) }
            .filter { query.dateTo == null || (it.startsAtUtc != null && !it.startsAtUtc.isAfter(query.dateTo)) }
            .map { it.toPublicView() }
            .toList()
    }

    fun getPublicOccurrence(occurrenceId: Long): PublicOccurrenceView {
        val occurrence = requirePublishedOccurrence(occurrenceId)
        return occurrence.toPublicView()
    }

    fun getAvailability(query: AvailabilityQuery): AvailabilityView {
        val occurrence = requirePublishedOccurrence(query.occurrenceId)
        val partySize = requireValidPartySize(query.partySize)
        val confirmedCount = occupiedSeats(occurrence.id)
        val heldCount = heldSeats(occurrence.id)
        val available = (occurrence.capacity - confirmedCount - heldCount).coerceAtLeast(0)
        return AvailabilityView(
            partySize = partySize,
            capacity = occurrence.capacity,
            confirmedCount = confirmedCount,
            heldCount = heldCount,
            available = available,
            canConfirm = available >= partySize,
            willWaitlist = available < partySize,
        )
    }

    fun getQuote(query: AvailabilityQuery): QuoteView {
        val occurrence = requirePublishedOccurrence(query.occurrenceId)
        val availability = getAvailability(query)
        return QuoteView(
            partySize = availability.partySize,
            unitPrice = occurrence.unitPrice,
            totalPrice = occurrence.unitPrice * availability.partySize,
            currency = occurrence.currency,
            canConfirm = availability.canConfirm,
            willWaitlist = availability.willWaitlist,
            refundPolicySummary = "Leader cancellation keeps full refund until 48 hours before start.",
            fullRefundDeadlineUtc = timeWindowPolicyService.fullRefundDeadline(occurrence),
        )
    }

    fun search(query: OccurrenceSearchQuery): OccurrenceSearchResult {
        val cursor = parseCursor(query.cursor)
        val limit = requireValidLimit(query.limit)
        val normalizedTimezone = query.timezone?.let(::requireValidTimezone)
        val normalizedLocation =
            query.locationText
                ?.trim()
                ?.lowercase()
                .takeUnless { it.isNullOrBlank() }
        val partySize = query.partySize?.let(::requireValidPartySize)
        val occurrencesById =
            occurrenceRepository
                .findAll()
                .filter { it.status == OccurrenceStatus.SCHEDULED }
                .associateBy { it.id }
        val publishedTours = tourRepository.findAllPublished().associateBy { requireNotNull(it.id) }
        val items =
            occurrencesById.values
                .asSequence()
                .filter { publishedTours.containsKey(requireNotNull(it.tourId)) }
                .filter { cursor == null || it.id > cursor }
                .filter { query.dateFrom == null || (it.startsAtUtc != null && !it.startsAtUtc.isBefore(query.dateFrom)) }
                .filter { query.dateTo == null || (it.startsAtUtc != null && !it.startsAtUtc.isAfter(query.dateTo)) }
                .filter { normalizedTimezone == null || it.timezone == normalizedTimezone }
                .filter { occurrence ->
                    if (normalizedLocation == null) {
                        true
                    } else {
                        listOfNotNull(
                            occurrence.locationText,
                            occurrence.meetingPoint,
                            publishedTours[requireNotNull(occurrence.tourId)]?.title,
                            publishedTours[requireNotNull(occurrence.tourId)]?.summary,
                        ).any { it.lowercase().contains(normalizedLocation) }
                    }
                }.map { occurrence ->
                    val tour = publishedTours.getValue(requireNotNull(occurrence.tourId))
                    val availability = partySize?.let { getAvailability(AvailabilityQuery(occurrence.id, it)) }
                    OccurrenceSearchItemView(
                        occurrence = occurrence.toPublicView(),
                        tour = tour.toPublicView(),
                        ratingSummary = ratingSummary(occurrence.id),
                    ) to availability
                }.filter { (_, availability) -> !query.onlyAvailable || availability?.canConfirm == true }
                .sortedWith(searchComparator(query.sort))
                .toList()
        val page = items.take(limit).map { it.first }
        val nextCursor =
            items
                .drop(limit)
                .firstOrNull()
                ?.first
                ?.occurrence
                ?.id
                ?.toString()
        return OccurrenceSearchResult(
            items = page,
            nextCursor = nextCursor,
        )
    }

    private fun requirePublishedTour(tourId: Long): Tour {
        val tour = tourRepository.findById(tourId) ?: throw notFound("tour $tourId not found")
        if (tour.status != TourStatus.PUBLISHED) {
            throw notFound("tour $tourId not found")
        }
        return tour
    }

    private fun requirePublishedOccurrence(occurrenceId: Long): Occurrence {
        val occurrence = occurrenceRepository.findById(occurrenceId) ?: throw notFound("occurrence $occurrenceId not found")
        if (occurrence.status != OccurrenceStatus.SCHEDULED) {
            throw notFound("occurrence $occurrenceId not found")
        }
        val tourId = requireNotNull(occurrence.tourId)
        requirePublishedTour(tourId)
        return occurrence
    }

    private fun occupiedSeats(occurrenceId: Long): Int =
        bookingRepository
            .findByOccurrenceAndStatuses(
                occurrenceId = occurrenceId,
                statuses = setOf(BookingStatus.CONFIRMED, BookingStatus.COMPLETED),
            ).sumOf { it.partySize }

    private fun heldSeats(occurrenceId: Long): Int =
        bookingRepository
            .findByOccurrenceAndStatuses(
                occurrenceId = occurrenceId,
                statuses = setOf(BookingStatus.OFFERED),
            ).sumOf { it.partySize }

    private fun ratingSummary(occurrenceId: Long): RatingSummaryView? {
        val reviews = reviewRepository.findByOccurrenceAndType(occurrenceId, ReviewType.TOUR)
        if (reviews.isEmpty()) {
            return null
        }
        return RatingSummaryView(
            avgRating = reviews.map { it.rating }.average(),
            reviewCount = reviews.size,
        )
    }

    private fun searchComparator(sort: String?): Comparator<Pair<OccurrenceSearchItemView, AvailabilityView?>> =
        when (sort?.trim()?.lowercase()) {
            "price_asc" -> {
                compareBy({ it.first.occurrence.unitPrice }, { it.first.occurrence.id })
            }

            "price_desc" -> {
                compareByDescending<Pair<OccurrenceSearchItemView, AvailabilityView?>> { it.first.occurrence.unitPrice }
                    .thenBy { it.first.occurrence.id }
            }

            "start_desc" -> {
                compareByDescending<Pair<OccurrenceSearchItemView, AvailabilityView?>> { it.first.occurrence.startsAtUtc }
                    .thenByDescending { it.first.occurrence.id }
            }

            else -> {
                compareBy<Pair<OccurrenceSearchItemView, AvailabilityView?>> { it.first.occurrence.startsAtUtc }
                    .thenBy { it.first.occurrence.id }
            }
        }

    private fun Tour.toPublicView(): PublicTourView =
        PublicTourView(
            id = requireNotNull(id),
            organizationId = organizationId,
            title = title,
            summary = summary,
            description = content.description,
            highlights = content.highlights,
            attachmentAssetIds = attachmentAssetIds,
            publishedAt = publishedAt,
        )

    private fun Occurrence.toPublicView(): PublicOccurrenceView =
        PublicOccurrenceView(
            id = id,
            tourId = requireNotNull(tourId),
            organizationId = organizationId,
            instructorProfileId = instructorProfileId,
            capacity = capacity,
            startsAtUtc = startsAtUtc,
            endsAtUtc = endsAtUtc,
            timezone = timezone,
            unitPrice = unitPrice,
            currency = currency,
            locationText = locationText,
            meetingPoint = meetingPoint,
            status = status.name,
            createdAt = createdAt,
        )

    private fun notFound(message: String) =
        DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 404,
            message = message,
        )
}
