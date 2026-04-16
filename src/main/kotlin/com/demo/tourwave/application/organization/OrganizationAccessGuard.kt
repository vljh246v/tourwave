package com.demo.tourwave.application.organization

import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.organization.Organization
import com.demo.tourwave.domain.organization.OrganizationMembership
import com.demo.tourwave.domain.organization.OrganizationMembershipStatus
import com.demo.tourwave.domain.organization.OrganizationRole

class OrganizationAccessGuard(
    private val organizationRepository: OrganizationRepository,
    private val membershipRepository: OrganizationMembershipRepository
) {
    fun requireOrganization(organizationId: Long): Organization {
        return organizationRepository.findById(organizationId) ?: throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 404,
            message = "organization $organizationId not found"
        )
    }

    fun requireMembership(actorUserId: Long, organizationId: Long): OrganizationMembership {
        requireOrganization(organizationId)
        val membership = membershipRepository.findByOrganizationIdAndUserId(organizationId, actorUserId)
            ?: throw forbidden("organization membership is required")
        if (membership.status != OrganizationMembershipStatus.ACTIVE) {
            throw forbidden("organization membership is not active")
        }
        return membership
    }

    fun requireOperator(actorUserId: Long, organizationId: Long): OrganizationMembership {
        val membership = requireMembership(actorUserId, organizationId)
        if (!membership.role.canManageMembers()) {
            throw forbidden("organization operator role is required")
        }
        return membership
    }

    fun requireOwner(actorUserId: Long, organizationId: Long): OrganizationMembership {
        val membership = requireMembership(actorUserId, organizationId)
        if (membership.role != OrganizationRole.OWNER) {
            throw forbidden("organization owner role is required")
        }
        return membership
    }

    fun requireAdmin(actorUserId: Long, organizationId: Long): OrganizationMembership {
        return requireOperator(actorUserId, organizationId)
    }

    private fun forbidden(message: String): DomainException {
        return DomainException(
            errorCode = ErrorCode.FORBIDDEN,
            status = 403,
            message = message
        )
    }
}
