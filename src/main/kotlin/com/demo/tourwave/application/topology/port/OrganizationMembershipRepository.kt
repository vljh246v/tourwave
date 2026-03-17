package com.demo.tourwave.application.topology.port

import com.demo.tourwave.domain.organization.OrganizationMembership

interface OrganizationMembershipRepository {
    fun save(membership: OrganizationMembership): OrganizationMembership
    fun findByOrganizationIdAndUserId(organizationId: Long, userId: Long): OrganizationMembership?
    fun findByOrganizationId(organizationId: Long): List<OrganizationMembership>
    fun findByUserId(userId: Long): List<OrganizationMembership>
    fun clear()
}
