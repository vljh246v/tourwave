package com.demo.tourwave.adapter.out.persistence.jpa.announcement

import com.demo.tourwave.domain.announcement.AnnouncementVisibility
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "announcements",
    indexes = [
        Index(name = "idx_announcements_org_created", columnList = "organization_id,created_at"),
        Index(name = "idx_announcements_visibility_window", columnList = "visibility,publish_starts_at_utc,publish_ends_at_utc"),
    ],
)
data class AnnouncementJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "organization_id", nullable = false)
    val organizationId: Long,
    @Column(nullable = false, length = 200)
    val title: String,
    @Column(columnDefinition = "TEXT", nullable = false)
    val body: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    val visibility: AnnouncementVisibility,
    @Column(name = "publish_starts_at_utc")
    val publishStartsAtUtc: Instant? = null,
    @Column(name = "publish_ends_at_utc")
    val publishEndsAtUtc: Instant? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
)
