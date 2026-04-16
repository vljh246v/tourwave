package com.demo.tourwave.adapter.out.persistence.topology

import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.domain.organization.Organization
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
@Profile("!mysql & !mysql-test")
class InMemoryOrganizationRepositoryAdapter : OrganizationRepository {
    private val sequence = AtomicLong(0L)
    private val organizations = ConcurrentHashMap<Long, Organization>()
    private val organizationIdBySlug = ConcurrentHashMap<String, Long>()

    override fun save(organization: Organization): Organization {
        val organizationId = organization.id ?: sequence.incrementAndGet()
        val saved = organization.copy(id = organizationId)
        organizations[organizationId] = saved
        organizationIdBySlug[saved.slug] = organizationId
        return saved
    }

    override fun findById(organizationId: Long): Organization? = organizations[organizationId]

    override fun findBySlug(slug: String): Organization? = organizationIdBySlug[slug]?.let(organizations::get)

    override fun findAll(): List<Organization> = organizations.values.sortedBy { it.id }

    override fun clear() {
        sequence.set(0L)
        organizations.clear()
        organizationIdBySlug.clear()
    }
}
