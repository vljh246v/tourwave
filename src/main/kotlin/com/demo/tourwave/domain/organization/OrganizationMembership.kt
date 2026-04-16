package com.demo.tourwave.domain.organization

import java.time.Instant

data class OrganizationMembership(
    val id: Long? = null,
    val organizationId: Long,
    val userId: Long,
    val role: OrganizationRole,
    val status: OrganizationMembershipStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun activate(now: Instant): OrganizationMembership {
        return copy(status = OrganizationMembershipStatus.ACTIVE, updatedAt = now)
    }

    fun deactivate(now: Instant): OrganizationMembership {
        return copy(status = OrganizationMembershipStatus.INACTIVE, updatedAt = now)
    }

    fun changeRole(
        role: OrganizationRole,
        now: Instant,
    ): OrganizationMembership {
        return copy(role = role, updatedAt = now)
    }

    companion object {
        fun invite(
            organizationId: Long,
            userId: Long,
            role: OrganizationRole,
            now: Instant,
        ): OrganizationMembership {
            return OrganizationMembership(
                organizationId = organizationId,
                userId = userId,
                role = role,
                status = OrganizationMembershipStatus.INVITED,
                createdAt = now,
                updatedAt = now,
            )
        }

        fun active(
            organizationId: Long,
            userId: Long,
            role: OrganizationRole,
            now: Instant,
        ): OrganizationMembership {
            return OrganizationMembership(
                organizationId = organizationId,
                userId = userId,
                role = role,
                status = OrganizationMembershipStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
