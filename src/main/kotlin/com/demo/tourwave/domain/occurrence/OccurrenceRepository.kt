package com.demo.tourwave.domain.occurrence

interface OccurrenceRepository {
    fun getOrCreate(occurrenceId: Long): Occurrence
    fun save(occurrence: Occurrence)
    fun clear()
}
