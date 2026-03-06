package com.demo.tourwave.domain.occurrence

import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryOccurrenceRepository : OccurrenceRepository {
    private val occurrences = ConcurrentHashMap<Long, Occurrence>()

    override fun getOrCreate(occurrenceId: Long): Occurrence {
        return occurrences.computeIfAbsent(occurrenceId) {
            Occurrence(
                id = occurrenceId,
                organizationId = 1L,
                capacity = 10,
                status = OccurrenceStatus.SCHEDULED
            )
        }
    }

    override fun save(occurrence: Occurrence) {
        occurrences[occurrence.id] = occurrence
    }

    override fun clear() {
        occurrences.clear()
    }
}
