package com.demo.tourwave.application.occurrence

import java.time.Instant

data class CreateOccurrenceCommand(
    val actorUserId: Long,
    val tourId: Long,
    val instructorProfileId: Long? = null,
    val capacity: Int,
    val startsAtUtc: Instant,
    val endsAtUtc: Instant,
    val timezone: String,
    val unitPrice: Int,
    val currency: String,
    val locationText: String? = null,
    val meetingPoint: String? = null,
)

data class UpdateOccurrenceCommand(
    val actorUserId: Long,
    val occurrenceId: Long,
    val instructorProfileId: Long? = null,
    val capacity: Int,
    val startsAtUtc: Instant,
    val endsAtUtc: Instant,
    val timezone: String,
    val locationText: String? = null,
    val meetingPoint: String? = null,
)

data class RescheduleOccurrenceCommand(
    val actorUserId: Long,
    val occurrenceId: Long,
    val startsAtUtc: Instant,
    val endsAtUtc: Instant,
    val timezone: String,
    val locationText: String? = null,
    val meetingPoint: String? = null,
)

data class PublicTourListQuery(
    val cursor: String? = null,
    val limit: Int? = null,
    val q: String? = null,
    val organizationId: Long? = null,
)

data class TourOccurrenceListQuery(
    val tourId: Long,
    val dateFrom: Instant? = null,
    val dateTo: Instant? = null,
)

data class AvailabilityQuery(
    val occurrenceId: Long,
    val partySize: Int,
)

data class OccurrenceSearchQuery(
    val cursor: String? = null,
    val limit: Int? = null,
    val dateFrom: Instant? = null,
    val dateTo: Instant? = null,
    val timezone: String? = null,
    val locationText: String? = null,
    val partySize: Int? = null,
    val onlyAvailable: Boolean = false,
    val sort: String? = null,
)

data class PublicTourView(
    val id: Long,
    val organizationId: Long,
    val title: String,
    val summary: String?,
    val description: String?,
    val highlights: List<String>,
    val attachmentAssetIds: List<Long>,
    val publishedAt: Instant?,
)

data class PublicOccurrenceView(
    val id: Long,
    val tourId: Long,
    val organizationId: Long,
    val instructorProfileId: Long?,
    val capacity: Int,
    val startsAtUtc: Instant?,
    val endsAtUtc: Instant?,
    val timezone: String,
    val unitPrice: Int,
    val currency: String,
    val locationText: String?,
    val meetingPoint: String?,
    val status: String,
    val createdAt: Instant,
)

data class AvailabilityView(
    val partySize: Int,
    val capacity: Int,
    val confirmedCount: Int,
    val heldCount: Int,
    val available: Int,
    val canConfirm: Boolean,
    val willWaitlist: Boolean,
)

data class QuoteView(
    val partySize: Int,
    val unitPrice: Int,
    val totalPrice: Int,
    val currency: String,
    val canConfirm: Boolean,
    val willWaitlist: Boolean,
    val refundPolicySummary: String,
    val fullRefundDeadlineUtc: Instant?,
)

data class RatingSummaryView(
    val avgRating: Double,
    val reviewCount: Int,
)

data class OccurrenceSearchItemView(
    val occurrence: PublicOccurrenceView,
    val tour: PublicTourView,
    val ratingSummary: RatingSummaryView?,
)

data class OccurrenceSearchResult(
    val items: List<OccurrenceSearchItemView>,
    val nextCursor: String?,
)
