package com.demo.tourwave.application.topology

import com.demo.tourwave.application.topology.port.OrganizationMembershipRepository
import com.demo.tourwave.application.topology.port.OrganizationRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.organization.Organization
import com.demo.tourwave.domain.organization.OrganizationMembership
import com.demo.tourwave.domain.organization.OrganizationStatus

class OrganizationQueryService(
    private val organizationRepository: OrganizationRepository,
    private val membershipRepository: OrganizationMembershipRepository,
    private val organizationAccessGuard: OrganizationAccessGuard
) {
    fun getPublicOrganization(organizationId: Long): Organization {
        val organization = organizationRepository.findById(organizationId) ?: throw notFound(organizationId)
        if (organization.status != OrganizationStatus.ACTIVE) {
            throw notFound(organizationId)
        }
        return organization
    }

    fun getOperatorOrganization(actorUserId: Long, organizationId: Long): Organization {
        organizationAccessGuard.requireMembership(actorUserId, organizationId)
        return organizationRepository.findById(organizationId) ?: throw notFound(organizationId)
    }

    fun getMembershipsForUser(userId: Long): List<OrganizationMembership> {
        return membershipRepository.findByUserId(userId)
    }

    private fun notFound(organizationId: Long): DomainException {
        return DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 404,
            message = "organization $organizationId not found"
        )
    }
}
