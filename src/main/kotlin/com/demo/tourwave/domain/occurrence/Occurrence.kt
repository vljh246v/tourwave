package com.demo.tourwave.domain.occurrence

import java.time.Instant

data class Occurrence(
    val id: Long,
    val organizationId: Long,
    val tourId: Long? = null,
    val instructorProfileId: Long? = null,
    val capacity: Int,
    val startsAtUtc: Instant? = null,
    val endsAtUtc: Instant? = null,
    val timezone: String = "UTC",
    val unitPrice: Int = 0,
    val currency: String = "KRW",
    val locationText: String? = null,
    val meetingPoint: String? = null,
    val status: OccurrenceStatus = OccurrenceStatus.SCHEDULED,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = createdAt,
) {
    companion object {
        fun create(
            organizationId: Long,
            tourId: Long,
            instructorProfileId: Long?,
            capacity: Int,
            startsAtUtc: Instant,
            endsAtUtc: Instant,
            timezone: String,
            unitPrice: Int,
            currency: String,
            locationText: String?,
            meetingPoint: String?,
            now: Instant,
        ): Occurrence {
            return Occurrence(
                id = 0L,
                organizationId = organizationId,
                tourId = tourId,
                instructorProfileId = instructorProfileId,
                capacity = capacity,
                startsAtUtc = startsAtUtc,
                endsAtUtc = endsAtUtc,
                timezone = timezone,
                unitPrice = unitPrice,
                currency = currency,
                locationText = locationText,
                meetingPoint = meetingPoint,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    fun updateAuthoring(
        instructorProfileId: Long?,
        capacity: Int,
        startsAtUtc: Instant,
        endsAtUtc: Instant,
        timezone: String,
        locationText: String?,
        meetingPoint: String?,
        now: Instant,
    ): Occurrence {
        return copy(
            instructorProfileId = instructorProfileId,
            capacity = capacity,
            startsAtUtc = startsAtUtc,
            endsAtUtc = endsAtUtc,
            timezone = timezone,
            locationText = locationText,
            meetingPoint = meetingPoint,
            updatedAt = now,
        )
    }

    fun reschedule(
        startsAtUtc: Instant,
        endsAtUtc: Instant,
        timezone: String,
        locationText: String?,
        meetingPoint: String?,
        now: Instant,
    ): Occurrence {
        return copy(
            startsAtUtc = startsAtUtc,
            endsAtUtc = endsAtUtc,
            timezone = timezone,
            locationText = locationText,
            meetingPoint = meetingPoint,
            updatedAt = now,
        )
    }
}
