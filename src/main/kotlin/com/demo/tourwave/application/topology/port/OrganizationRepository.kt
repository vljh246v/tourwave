package com.demo.tourwave.application.topology.port

import com.demo.tourwave.domain.organization.Organization

interface OrganizationRepository {
    fun save(organization: Organization): Organization
    fun findById(organizationId: Long): Organization?
    fun findBySlug(slug: String): Organization?
    fun findAll(): List<Organization>
    fun clear()
}
