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
        Index(name = "idx_occurrences_status", columnList = "status"),
    ],
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
    @Column(name = "end_at_utc")
    val endsAtUtc: Instant? = null,
    @Column(nullable = false, length = 64)
    val timezone: String = "UTC",
    @Column(name = "unit_price", nullable = false)
    val unitPrice: Int = 0,
    @Column(nullable = false, length = 3)
    val currency: String = "KRW",
    @Column(name = "location_text", length = 255)
    val locationText: String? = null,
    @Column(name = "meeting_point", length = 500)
    val meetingPoint: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    val status: OccurrenceStatus = OccurrenceStatus.SCHEDULED,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.EPOCH,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.EPOCH,
)
