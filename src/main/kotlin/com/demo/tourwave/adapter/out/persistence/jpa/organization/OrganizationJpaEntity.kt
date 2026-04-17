package com.demo.tourwave.adapter.out.persistence.jpa.organization

import com.demo.tourwave.domain.organization.OrganizationStatus
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
    name = "organizations",
    indexes = [Index(name = "uk_organizations_slug", columnList = "slug", unique = true)],
)
data class OrganizationJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val slug: String,
    @Column(nullable = false)
    val name: String,
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    @Column(name = "public_description", columnDefinition = "TEXT")
    val publicDescription: String? = null,
    @Column(name = "contact_email")
    val contactEmail: String? = null,
    @Column(name = "contact_phone")
    val contactPhone: String? = null,
    @Column(name = "website_url")
    val websiteUrl: String? = null,
    @Column(name = "business_name")
    val businessName: String? = null,
    @Column(name = "business_registration_number")
    val businessRegistrationNumber: String? = null,
    @Column(name = "attachment_asset_ids_json", columnDefinition = "TEXT", nullable = false)
    val attachmentAssetIdsJson: String,
    @Column(nullable = false)
    val timezone: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: OrganizationStatus,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
)
