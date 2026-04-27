package com.demo.tourwave.application.organization

import com.demo.tourwave.adapter.out.persistence.idempotency.InMemoryIdempotencyStoreAdapter
import com.demo.tourwave.adapter.out.persistence.organization.InMemoryOrganizationMembershipRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.organization.InMemoryOrganizationRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.user.UserQueryAdapter
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.support.FakeAuditEventPort
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OrganizationCommandAuditTest {
    private val clock = Clock.fixed(Instant.parse("2026-03-19T10:00:00Z"), ZoneOffset.UTC)
    private val organizationRepository = InMemoryOrganizationRepositoryAdapter()
    private val membershipRepository = InMemoryOrganizationMembershipRepositoryAdapter()
    private val userRepository = UserQueryAdapter()
    private val auditEventPort = FakeAuditEventPort()
    private val idempotencyStore = InMemoryIdempotencyStoreAdapter()
    private val accessGuard = OrganizationAccessGuard(organizationRepository, membershipRepository)
    private val organizationCommandService =
        OrganizationCommandService(
            organizationRepository = organizationRepository,
            membershipRepository = membershipRepository,
            userRepository = userRepository,
            organizationAccessGuard = accessGuard,
            auditEventPort = auditEventPort,
            idempotencyStore = idempotencyStore,
            clock = clock,
        )

    @BeforeEach
    fun setUp() {
        idempotencyStore.clear()
        organizationRepository.clear()
        membershipRepository.clear()
        userRepository.clear()
        auditEventPort.clear()
    }

    @Test
    fun `createOrganization appends ORGANIZATION_CREATED audit event`() {
        val owner =
            userRepository.save(
                User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = clock.instant()),
            )

        organizationCommandService.createOrganization(
            CreateOrganizationCommand(
                actorUserId = requireNotNull(owner.id),
                slug = "audit-org",
                name = "Audit Org",
                timezone = "Asia/Seoul",
                idempotencyKey = "audit-create-001",
            ),
        )

        assertEquals(1, auditEventPort.events.size)
        val event = auditEventPort.events.first()
        assertEquals("ORGANIZATION_CREATED", event.action)
        assertEquals("ORGANIZATION", event.resourceType)
        assertEquals("USER:${owner.id}", event.actor)
        assertNotNull(event.afterJson)
    }

    @Test
    fun `updateOrganizationProfile appends ORGANIZATION_PROFILE_UPDATED audit event`() {
        val owner =
            userRepository.save(
                User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = clock.instant()),
            )
        val org =
            organizationCommandService.createOrganization(
                CreateOrganizationCommand(
                    actorUserId = requireNotNull(owner.id),
                    slug = "audit-org-2",
                    name = "Audit Org 2",
                    timezone = "Asia/Seoul",
                    idempotencyKey = "audit-create-002",
                ),
            )
        auditEventPort.clear()

        organizationCommandService.updateOrganizationProfile(
            UpdateOrganizationProfileCommand(
                actorUserId = requireNotNull(owner.id),
                organizationId = requireNotNull(org.id),
                name = "Updated Name",
                timezone = "Asia/Seoul",
                idempotencyKey = "audit-update-001",
            ),
        )

        assertEquals(1, auditEventPort.events.size)
        val event = auditEventPort.events.first()
        assertEquals("ORGANIZATION_PROFILE_UPDATED", event.action)
        assertEquals("ORGANIZATION", event.resourceType)
        assertEquals("OPERATOR:${owner.id}", event.actor)
        assertNotNull(event.beforeJson)
        assertNotNull(event.afterJson)
    }
}
