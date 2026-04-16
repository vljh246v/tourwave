package com.demo.tourwave.adapter.out.persistence.jpa.occurrence

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OccurrenceJpaRepository : JpaRepository<OccurrenceJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OccurrenceJpaEntity o where o.id = :id")
    fun findLockedById(
        @Param("id") id: Long,
    ): OccurrenceJpaEntity?

    fun findByTourIdOrderByStartsAtUtcAscIdAsc(tourId: Long): List<OccurrenceJpaEntity>

    @Query("select coalesce(max(o.id), 0) from OccurrenceJpaEntity o")
    fun findMaxId(): Long
}
