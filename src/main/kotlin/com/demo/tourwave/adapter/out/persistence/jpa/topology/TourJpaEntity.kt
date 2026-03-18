package com.demo.tourwave.adapter.out.persistence.jpa.topology

import com.demo.tourwave.domain.tour.TourStatus
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
    name = "tours",
    indexes = [Index(name = "idx_tours_org_status", columnList = "organization_id,status")]
)
data class TourJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "organization_id", nullable = false)
    val organizationId: Long,
    @Column(nullable = false)
    val title: String,
    @Column(columnDefinition = "TEXT")
    val summary: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: TourStatus,
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    @Column(name = "highlights_json", columnDefinition = "TEXT", nullable = false)
    val highlightsJson: String,
    @Column(name = "inclusions_json", columnDefinition = "TEXT", nullable = false)
    val inclusionsJson: String,
    @Column(name = "exclusions_json", columnDefinition = "TEXT", nullable = false)
    val exclusionsJson: String,
    @Column(name = "preparations_json", columnDefinition = "TEXT", nullable = false)
    val preparationsJson: String,
    @Column(name = "policies_json", columnDefinition = "TEXT", nullable = false)
    val policiesJson: String,
    @Column(name = "attachment_asset_ids_json", columnDefinition = "TEXT", nullable = false)
    val attachmentAssetIdsJson: String,
    @Column(name = "published_at")
    val publishedAt: Instant? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant
)
