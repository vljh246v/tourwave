package com.demo.tourwave.adapter.out.persistence.jpa.instructor

import com.demo.tourwave.domain.instructor.InstructorRegistrationStatus
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
    name = "instructor_registrations",
    indexes = [
        Index(name = "idx_instructor_registrations_org_status", columnList = "organization_id,status"),
        Index(name = "idx_instructor_registrations_user_status", columnList = "user_id,status"),
        Index(name = "uk_instructor_registrations_org_user", columnList = "organization_id,user_id", unique = true)
    ]
)
data class InstructorRegistrationJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "organization_id", nullable = false)
    val organizationId: Long,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    val headline: String? = null,
    @Column(columnDefinition = "TEXT")
    val bio: String? = null,
    @Column(name = "languages_json", columnDefinition = "TEXT", nullable = false)
    val languagesJson: String,
    @Column(name = "specialties_json", columnDefinition = "TEXT", nullable = false)
    val specialtiesJson: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: InstructorRegistrationStatus,
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    val rejectionReason: String? = null,
    @Column(name = "reviewed_by_user_id")
    val reviewedByUserId: Long? = null,
    @Column(name = "reviewed_at")
    val reviewedAt: Instant? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant
)
