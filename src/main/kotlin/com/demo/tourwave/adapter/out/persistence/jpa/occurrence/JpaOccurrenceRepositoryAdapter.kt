package com.demo.tourwave.adapter.out.persistence.jpa.occurrence

import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.occurrence.OccurrenceStatus
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("mysql", "mysql-test")
class JpaOccurrenceRepositoryAdapter(
    private val occurrenceJpaRepository: OccurrenceJpaRepository,
) : OccurrenceRepository {
    override fun nextId(): Long = occurrenceJpaRepository.findMaxId() + 1

    override fun getOrCreate(occurrenceId: Long): Occurrence {
        return occurrenceJpaRepository.findById(occurrenceId).orElseGet {
            occurrenceJpaRepository.save(
                OccurrenceJpaEntity(
                    id = occurrenceId,
                    organizationId = 1L,
                    capacity = 10,
                    status = OccurrenceStatus.SCHEDULED,
                ),
            )
        }.toDomain()
    }

    override fun findById(occurrenceId: Long): Occurrence? = occurrenceJpaRepository.findById(occurrenceId).orElse(null)?.toDomain()

    override fun findByTourId(tourId: Long): List<Occurrence> =
        occurrenceJpaRepository.findByTourIdOrderByStartsAtUtcAscIdAsc(tourId).map { it.toDomain() }

    override fun findAll(): List<Occurrence> = occurrenceJpaRepository.findAll().map { it.toDomain() }.sortedBy { it.id }

    override fun lock(occurrenceId: Long): Occurrence {
        if (!occurrenceJpaRepository.existsById(occurrenceId)) {
            getOrCreate(occurrenceId)
        }
        return requireNotNull(occurrenceJpaRepository.findLockedById(occurrenceId)).toDomain()
    }

    override fun save(occurrence: Occurrence) {
        occurrenceJpaRepository.save(occurrence.toEntity())
    }

    override fun clear() {
        occurrenceJpaRepository.deleteAllInBatch()
    }
}

private fun Occurrence.toEntity(): OccurrenceJpaEntity =
    OccurrenceJpaEntity(
        id = id,
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
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun OccurrenceJpaEntity.toDomain(): Occurrence =
    Occurrence(
        id = id,
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
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
