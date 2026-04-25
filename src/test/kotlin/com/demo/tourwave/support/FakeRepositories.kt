package com.demo.tourwave.support

import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.tour.port.TourRepository
import com.demo.tourwave.application.user.UserPort
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.organization.Organization
import com.demo.tourwave.domain.organization.OrganizationMembership
import com.demo.tourwave.domain.tour.Tour
import com.demo.tourwave.domain.tour.TourStatus
import com.demo.tourwave.domain.user.User
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class FakeOrganizationRepository : OrganizationRepository {
    private val sequence = AtomicLong(0)
    private val organizations = ConcurrentHashMap<Long, Organization>()

    override fun save(organization: Organization): Organization {
        val id = organization.id ?: sequence.incrementAndGet()
        val saved = organization.copy(id = id)
        organizations[id] = saved
        return saved
    }

    override fun findById(organizationId: Long): Organization? = organizations[organizationId]

    override fun findBySlug(slug: String): Organization? = organizations.values.firstOrNull { it.slug == slug }

    override fun findAll(): List<Organization> = organizations.values.sortedBy { it.id }

    override fun clear() {
        organizations.clear()
        sequence.set(0)
    }
}

class FakeOrganizationMembershipRepository : OrganizationMembershipRepository {
    private val memberships = mutableListOf<OrganizationMembership>()

    override fun save(membership: OrganizationMembership): OrganizationMembership {
        memberships.removeIf { it.organizationId == membership.organizationId && it.userId == membership.userId }
        memberships += membership
        return membership
    }

    override fun findByOrganizationId(organizationId: Long): List<OrganizationMembership> = memberships.filter { it.organizationId == organizationId }

    override fun findByOrganizationIdAndUserId(
        organizationId: Long,
        userId: Long,
    ): OrganizationMembership? = memberships.firstOrNull { it.organizationId == organizationId && it.userId == userId }

    override fun findByUserId(userId: Long): List<OrganizationMembership> = memberships.filter { it.userId == userId }

    override fun clear() {
        memberships.clear()
    }
}

class FakeTourRepository : TourRepository {
    private val sequence = AtomicLong(0)
    private val tours = ConcurrentHashMap<Long, Tour>()

    override fun save(tour: Tour): Tour {
        val id = tour.id ?: sequence.incrementAndGet()
        val saved = tour.copy(id = id)
        tours[id] = saved
        return saved
    }

    override fun findById(tourId: Long): Tour? = tours[tourId]

    override fun findByOrganizationId(organizationId: Long): List<Tour> =
        tours.values.filter {
            it.organizationId == organizationId
        }.sortedBy { it.id }

    override fun findAllPublished(): List<Tour> = tours.values.filter { it.status == TourStatus.PUBLISHED }.sortedBy { it.id }

    override fun clear() {
        tours.clear()
        sequence.set(0)
    }
}

class FakeUserRepository : UserRepository {
    private val sequence = AtomicLong(0)
    private val users = ConcurrentHashMap<Long, User>()

    override fun save(user: User): User {
        val id = user.id ?: sequence.incrementAndGet()
        val saved = user.copy(id = id)
        users[id] = saved
        return saved
    }

    override fun findById(userId: Long): User? = users[userId]

    override fun findByEmail(email: String): User? = users.values.firstOrNull { it.email == email.trim().lowercase() }

    override fun findAll(): List<User> = users.values.sortedBy { it.id }

    override fun clear() {
        users.clear()
        sequence.set(0)
    }
}

class FakeUserPort : UserPort {
    private val sequence = AtomicLong(0)
    private val users = ConcurrentHashMap<Long, User>()

    override fun save(user: User): User {
        val id = user.id ?: sequence.incrementAndGet()
        val saved = user.copy(id = id)
        users[id] = saved
        return saved
    }

    override fun findById(userId: Long): User? = users[userId]

    override fun findByEmail(email: String): User? = users.values.firstOrNull { it.email == email.trim().lowercase() }

    override fun deleteById(userId: Long) {
        users.remove(userId)
    }

    fun clear() {
        users.clear()
        sequence.set(0)
    }
}

class FakeAuthRefreshTokenRepository : com.demo.tourwave.application.auth.port.AuthRefreshTokenRepository {
    private val sequence = AtomicLong(0)
    private val tokens = ConcurrentHashMap<Long, com.demo.tourwave.domain.auth.AuthRefreshToken>()
    private val tokenHashIndex = ConcurrentHashMap<String, Long>()

    override fun save(token: com.demo.tourwave.domain.auth.AuthRefreshToken): com.demo.tourwave.domain.auth.AuthRefreshToken {
        val id = token.id ?: sequence.incrementAndGet()
        val saved = token.copy(id = id)
        tokens[id] = saved
        tokenHashIndex[saved.tokenHash] = id
        return saved
    }

    override fun findByTokenHash(tokenHash: String): com.demo.tourwave.domain.auth.AuthRefreshToken? {
        val id = tokenHashIndex[tokenHash] ?: return null
        return tokens[id]
    }

    override fun revokeAllByUserId(userId: Long, revokedAtUtc: java.time.Instant) {
        tokens.values
            .filter { it.userId == userId && it.revokedAtUtc == null }
            .forEach { token ->
                val revoked = token.copy(revokedAtUtc = revokedAtUtc)
                tokens[requireNotNull(revoked.id)] = revoked
            }
    }

    override fun rotate(token: com.demo.tourwave.domain.auth.AuthRefreshToken): com.demo.tourwave.domain.auth.AuthRefreshToken {
        val revoked = token.copy(revokedAtUtc = java.time.Instant.now())
        tokens[requireNotNull(revoked.id)] = revoked
        return revoked
    }

    override fun clear() {
        tokens.clear()
        tokenHashIndex.clear()
        sequence.set(0)
    }

    fun findAll(): List<com.demo.tourwave.domain.auth.AuthRefreshToken> = tokens.values.sortedBy { it.id }
}
