package com.demo.tourwave.adapter.out.persistence.jpa.occurrence

import com.demo.tourwave.domain.occurrence.OccurrenceStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "occurrences",
    indexes = [
        Index(name = "idx_occurrences_org", columnList = "organization_id"),
        Index(name = "idx_occurrences_status", columnList = "status")
    ]
)
data class OccurrenceJpaEntity(
    @Id
    val id: Long,
    @Column(name = "organization_id", nullable = false)
    val organizationId: Long,
    @Column(name = "tour_id")
    val tourId: Long? = null,
    @Column(name = "instructor_profile_id")
    val instructorProfileId: Long? = null,
    @Column(nullable = false)
    val capacity: Int,
    @Column(name = "starts_at_utc")
    val startsAtUtc: Instant? = null,
    @Column(nullable = false, length = 64)
    val timezone: String = "UTC",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    val status: OccurrenceStatus = OccurrenceStatus.SCHEDULED
)
