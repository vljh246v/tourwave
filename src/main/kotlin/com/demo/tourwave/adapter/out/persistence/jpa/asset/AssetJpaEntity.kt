package com.demo.tourwave.adapter.out.persistence.jpa.asset

import com.demo.tourwave.domain.asset.AssetStatus
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
    name = "assets",
    indexes = [
        Index(name = "idx_assets_owner_created", columnList = "owner_user_id,created_at"),
        Index(name = "idx_assets_org_created", columnList = "organization_id,created_at")
    ]
)
data class AssetJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "owner_user_id", nullable = false)
    val ownerUserId: Long,
    @Column(name = "organization_id")
    val organizationId: Long? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: AssetStatus,
    @Column(name = "file_name", nullable = false)
    val fileName: String,
    @Column(name = "content_type", nullable = false)
    val contentType: String,
    @Column(name = "storage_key", nullable = false)
    val storageKey: String,
    @Column(name = "upload_url", columnDefinition = "TEXT", nullable = false)
    val uploadUrl: String,
    @Column(name = "public_url", columnDefinition = "TEXT")
    val publicUrl: String? = null,
    @Column(name = "size_bytes")
    val sizeBytes: Long? = null,
    @Column(name = "checksum_sha256")
    val checksumSha256: String? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "completed_at")
    val completedAt: Instant? = null
)
