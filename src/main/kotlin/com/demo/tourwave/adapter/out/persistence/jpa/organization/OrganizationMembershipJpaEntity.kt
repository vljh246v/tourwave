package com.demo.tourwave.adapter.out.persistence.jpa.organization

import com.demo.tourwave.domain.organization.OrganizationMembershipStatus
import com.demo.tourwave.domain.organization.OrganizationRole
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
    name = "organization_members",
    indexes = [
        Index(name = "idx_organization_members_org_status", columnList = "organization_id,status"),
        Index(name = "idx_organization_members_user_status", columnList = "user_id,status"),
        Index(name = "uk_organization_members_org_user", columnList = "organization_id,user_id", unique = true),
    ],
)
data class OrganizationMembershipJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "organization_id", nullable = false)
    val organizationId: Long,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: OrganizationRole,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: OrganizationMembershipStatus,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
)
