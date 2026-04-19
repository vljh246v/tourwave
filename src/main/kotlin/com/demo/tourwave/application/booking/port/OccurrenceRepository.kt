package com.demo.tourwave.application.booking.port

import com.demo.tourwave.domain.occurrence.Occurrence

interface OccurrenceRepository {
    fun nextId(): Long

    fun getOrCreate(occurrenceId: Long): Occurrence

    fun findById(occurrenceId: Long): Occurrence?

    fun findByTourId(tourId: Long): List<Occurrence>

    fun findAll(): List<Occurrence>

    fun lock(occurrenceId: Long): Occurrence

    fun save(occurrence: Occurrence)

    fun clear()
}
