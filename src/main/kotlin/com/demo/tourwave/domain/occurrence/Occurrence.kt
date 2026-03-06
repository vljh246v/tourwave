package com.demo.tourwave.domain.occurrence

data class Occurrence(
    val id: Long,
    val organizationId: Long,
    val capacity: Int,
    val status: OccurrenceStatus = OccurrenceStatus.SCHEDULED
)
