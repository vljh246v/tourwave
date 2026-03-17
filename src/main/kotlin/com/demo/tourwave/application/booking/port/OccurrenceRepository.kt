package com.demo.tourwave.application.booking.port

import com.demo.tourwave.domain.occurrence.Occurrence

interface OccurrenceRepository {
    fun getOrCreate(occurrenceId: Long): Occurrence
    fun lock(occurrenceId: Long): Occurrence
    fun save(occurrence: Occurrence)
    fun clear()
}
