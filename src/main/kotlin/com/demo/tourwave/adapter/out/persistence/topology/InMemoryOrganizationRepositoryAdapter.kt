package com.demo.tourwave.adapter.out.persistence.topology

import com.demo.tourwave.application.topology.port.OrganizationRepository
import com.demo.tourwave.domain.organization.Organization
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
class InMemoryOrganizationRepositoryAdapter : OrganizationRepository {
    private val sequence = AtomicLong(0L)
    private val organizations = ConcurrentHashMap<Long, Organization>()

    override fun save(organization: Organization): Organization {
        val organizationId = organization.id ?: sequence.incrementAndGet()
        val saved = organization.copy(id = organizationId)
        organizations[organizationId] = saved
        return saved
    }

    override fun findById(organizationId: Long): Organization? = organizations[organizationId]

    override fun clear() {
        sequence.set(0L)
        organizations.clear()
    }
}
