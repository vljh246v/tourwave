package com.demo.tourwave.adapter.`in`.web.occurrence

import com.demo.tourwave.application.occurrence.AvailabilityView
import com.demo.tourwave.application.occurrence.OccurrenceSearchItemView
import com.demo.tourwave.application.occurrence.OccurrenceSearchResult
import com.demo.tourwave.application.occurrence.PublicOccurrenceView
import com.demo.tourwave.application.occurrence.PublicTourView
import com.demo.tourwave.application.occurrence.QuoteView
import com.demo.tourwave.adapter.`in`.web.tour.PublicTourResponse
import com.demo.tourwave.domain.occurrence.Occurrence
import java.time.Instant

data class CreateOccurrenceWebRequest(
    val instructorProfileId: Long? = null,
    val capacity: Int,
    val startsAtUtc: Instant,
    val endsAtUtc: Instant,
    val timezone: String,
    val unitPrice: Int,
    val currency: String,
    val locationText: String? = null,
    val meetingPoint: String? = null
)

data class UpdateOccurrenceWebRequest(
    val instructorProfileId: Long? = null,
    val capacity: Int,
    val startsAtUtc: Instant,
    val endsAtUtc: Instant,
    val timezone: String,
    val locationText: String? = null,
    val meetingPoint: String? = null
)

data class RescheduleOccurrenceWebRequest(
    val startsAtUtc: Instant,
    val endsAtUtc: Instant,
    val timezone: String,
    val locationText: String? = null,
    val meetingPoint: String? = null
)

data class OccurrenceResponse(
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
    val createdAt: Instant
)

data class AvailabilityResponse(
    val partySize: Int,
    val capacity: Int,
    val confirmedCount: Int,
    val heldCount: Int,
    val available: Int,
    val canConfirm: Boolean,
    val willWaitlist: Boolean
)

data class QuoteResponse(
    val partySize: Int,
    val unitPrice: Int,
    val totalPrice: Int,
    val currency: String,
    val canConfirm: Boolean,
    val willWaitlist: Boolean,
    val refundPolicySummary: String,
    val fullRefundDeadlineUtc: Instant?
)

data class RatingSummaryResponse(
    val avgRating: Double,
    val reviewCount: Int
)

data class OccurrenceSearchItemResponse(
    val occurrence: OccurrenceResponse,
    val tour: PublicTourResponse,
    val ratingSummary: RatingSummaryResponse?
)

data class OccurrenceSearchResponse(
    val items: List<OccurrenceSearchItemResponse>,
    val nextCursor: String?
)

fun PublicTourView.toResponse(): PublicTourResponse =
    PublicTourResponse(
        id = id,
        organizationId = organizationId,
        title = title,
        summary = summary,
        description = description,
        highlights = highlights,
        attachmentAssetIds = attachmentAssetIds,
        publishedAt = publishedAt
    )

fun Occurrence.toPublicView(): PublicOccurrenceView =
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
        createdAt = createdAt
    )

fun PublicOccurrenceView.toResponse(): OccurrenceResponse =
    OccurrenceResponse(
        id = id,
        tourId = tourId,
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
        status = status,
        createdAt = createdAt
    )

fun AvailabilityView.toResponse(): AvailabilityResponse =
    AvailabilityResponse(
        partySize = partySize,
        capacity = capacity,
        confirmedCount = confirmedCount,
        heldCount = heldCount,
        available = available,
        canConfirm = canConfirm,
        willWaitlist = willWaitlist
    )

fun QuoteView.toResponse(): QuoteResponse =
    QuoteResponse(
        partySize = partySize,
        unitPrice = unitPrice,
        totalPrice = totalPrice,
        currency = currency,
        canConfirm = canConfirm,
        willWaitlist = willWaitlist,
        refundPolicySummary = refundPolicySummary,
        fullRefundDeadlineUtc = fullRefundDeadlineUtc
    )

fun OccurrenceSearchItemView.toResponse(): OccurrenceSearchItemResponse =
    OccurrenceSearchItemResponse(
        occurrence = occurrence.toResponse(),
        tour = tour.toResponse(),
        ratingSummary = ratingSummary?.let {
            RatingSummaryResponse(
                avgRating = it.avgRating,
                reviewCount = it.reviewCount
            )
        }
    )

fun OccurrenceSearchResult.toResponse(): OccurrenceSearchResponse =
    OccurrenceSearchResponse(
        items = items.map { it.toResponse() },
        nextCursor = nextCursor
    )
