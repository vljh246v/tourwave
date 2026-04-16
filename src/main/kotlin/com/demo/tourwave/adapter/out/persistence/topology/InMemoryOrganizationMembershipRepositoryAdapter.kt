package com.demo.tourwave.adapter.out.persistence.topology

import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
import com.demo.tourwave.domain.organization.OrganizationMembership
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
@Profile("!mysql & !mysql-test")
class InMemoryOrganizationMembershipRepositoryAdapter : OrganizationMembershipRepository {
    private val sequence = AtomicLong(0L)
    private val memberships = ConcurrentHashMap<Long, OrganizationMembership>()
    private val membershipIdsByOrgAndUser = ConcurrentHashMap<String, Long>()

    override fun save(membership: OrganizationMembership): OrganizationMembership {
        val membershipId = membership.id ?: membershipIdsByOrgAndUser[keyOf(membership.organizationId, membership.userId)] ?: sequence.incrementAndGet()
        val saved = membership.copy(id = membershipId)
        memberships[membershipId] = saved
        membershipIdsByOrgAndUser[keyOf(saved.organizationId, saved.userId)] = membershipId
        return saved
    }

    override fun findByOrganizationIdAndUserId(organizationId: Long, userId: Long): OrganizationMembership? {
        return membershipIdsByOrgAndUser[keyOf(organizationId, userId)]?.let(memberships::get)
    }

    override fun findByOrganizationId(organizationId: Long): List<OrganizationMembership> {
        return memberships.values.filter { it.organizationId == organizationId }.sortedBy { it.id }
    }

    override fun findByUserId(userId: Long): List<OrganizationMembership> {
        return memberships.values.filter { it.userId == userId }.sortedBy { it.id }
    }

    override fun clear() {
        sequence.set(0L)
        memberships.clear()
        membershipIdsByOrgAndUser.clear()
    }

    private fun keyOf(organizationId: Long, userId: Long): String = "$organizationId:$userId"
}
