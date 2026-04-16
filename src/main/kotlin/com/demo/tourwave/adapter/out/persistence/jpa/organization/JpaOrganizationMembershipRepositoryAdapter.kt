package com.demo.tourwave.adapter.out.persistence.jpa.organization

import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
import com.demo.tourwave.domain.organization.OrganizationMembership
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("mysql", "mysql-test")
class JpaOrganizationMembershipRepositoryAdapter(
    private val membershipJpaRepository: OrganizationMembershipJpaRepository
) : OrganizationMembershipRepository {
    override fun save(membership: OrganizationMembership): OrganizationMembership {
        return membershipJpaRepository.save(membership.toEntity()).toDomain()
    }

    override fun findByOrganizationIdAndUserId(organizationId: Long, userId: Long): OrganizationMembership? {
        return membershipJpaRepository.findByOrganizationIdAndUserId(organizationId, userId)?.toDomain()
    }

    override fun findByOrganizationId(organizationId: Long): List<OrganizationMembership> {
        return membershipJpaRepository.findByOrganizationIdOrderByIdAsc(organizationId).map { it.toDomain() }
    }

    override fun findByUserId(userId: Long): List<OrganizationMembership> {
        return membershipJpaRepository.findByUserIdOrderByIdAsc(userId).map { it.toDomain() }
    }

    override fun clear() {
        membershipJpaRepository.deleteAllInBatch()
    }
}

private fun OrganizationMembership.toEntity(): OrganizationMembershipJpaEntity =
    OrganizationMembershipJpaEntity(
        id = id,
        organizationId = organizationId,
        userId = userId,
        role = role,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

private fun OrganizationMembershipJpaEntity.toDomain(): OrganizationMembership =
    OrganizationMembership(
        id = id,
        organizationId = organizationId,
        userId = userId,
        role = role,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
