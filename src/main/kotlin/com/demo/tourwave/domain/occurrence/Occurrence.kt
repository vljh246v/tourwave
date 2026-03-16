package com.demo.tourwave.domain.occurrence

import java.time.Instant

data class Occurrence(
    val id: Long,
    val organizationId: Long,
    val tourId: Long? = null,
    val instructorProfileId: Long? = null,
    val capacity: Int,
    val startsAtUtc: Instant? = null,
    val status: OccurrenceStatus = OccurrenceStatus.SCHEDULED
)
