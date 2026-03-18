package com.demo.tourwave.adapter.out.persistence.occurrence

import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.occurrence.OccurrenceStatus
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
@Profile("!mysql & !mysql-test")
class InMemoryOccurrenceRepositoryAdapter : OccurrenceRepository {
    private val occurrences = ConcurrentHashMap<Long, Occurrence>()
    private var sequence = 0L

    override fun nextId(): Long {
        sequence += 1
        return sequence
    }

    override fun getOrCreate(occurrenceId: Long): Occurrence {
        return occurrences.computeIfAbsent(occurrenceId) {
            sequence = maxOf(sequence, occurrenceId)
            Occurrence(
                id = occurrenceId,
                organizationId = 1L,
                capacity = 10,
                status = OccurrenceStatus.SCHEDULED
            )
        }
    }

    override fun findById(occurrenceId: Long): Occurrence? = occurrences[occurrenceId]

    override fun findByTourId(tourId: Long): List<Occurrence> =
        occurrences.values
            .filter { it.tourId == tourId }
            .sortedWith(compareBy<Occurrence> { it.startsAtUtc ?: java.time.Instant.MAX }.thenBy { it.id })

    override fun findAll(): List<Occurrence> =
        occurrences.values.sortedBy { it.id }

    override fun lock(occurrenceId: Long): Occurrence = getOrCreate(occurrenceId)

    override fun save(occurrence: Occurrence) {
        val occurrenceId = occurrence.id
        sequence = maxOf(sequence, occurrenceId)
        occurrences[occurrenceId] = occurrence.copy(id = occurrenceId)
    }

    override fun clear() {
        sequence = 0L
        occurrences.clear()
    }
}
