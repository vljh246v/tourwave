package com.demo.tourwave.application.tour

import com.demo.tourwave.adapter.out.persistence.idempotency.InMemoryIdempotencyStoreAdapter
import com.demo.tourwave.adapter.out.persistence.organization.InMemoryOrganizationMembershipRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.organization.InMemoryOrganizationRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.tour.InMemoryTourRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.user.UserQueryAdapter
import com.demo.tourwave.application.organization.CreateOrganizationCommand
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.organization.OrganizationCommandService
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.support.FakeAuditEventPort
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TourCommandAuditTest {
    private val clock = Clock.fixed(Instant.parse("2026-03-19T10:00:00Z"), ZoneOffset.UTC)
    private val organizationRepository = InMemoryOrganizationRepositoryAdapter()
    private val membershipRepository = InMemoryOrganizationMembershipRepositoryAdapter()
    private val tourRepository = InMemoryTourRepositoryAdapter()
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
    private val tourCommandService =
        TourCommandService(
            tourRepository = tourRepository,
            organizationRepository = organizationRepository,
            organizationAccessGuard = accessGuard,
            auditEventPort = auditEventPort,
            clock = clock,
        )

    private lateinit var owner: User
    private var organizationId: Long = 0L

    @BeforeEach
    fun setUp() {
        idempotencyStore.clear()
        organizationRepository.clear()
        membershipRepository.clear()
        tourRepository.clear()
        userRepository.clear()
        auditEventPort.clear()

        owner = userRepository.save(User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = clock.instant()))
        val org =
            organizationCommandService.createOrganization(
                CreateOrganizationCommand(
                    actorUserId = requireNotNull(owner.id),
                    slug = "tour-audit-org",
                    name = "Tour Audit Org",
                    timezone = "Asia/Seoul",
                    idempotencyKey = "create-org-tour-audit-001",
                ),
            )
        organizationId = requireNotNull(org.id)
        auditEventPort.clear()
    }

    @Test
    fun `create tour appends TOUR_CREATED audit event`() {
        tourCommandService.create(
            CreateTourCommand(
                actorUserId = requireNotNull(owner.id),
                organizationId = organizationId,
                title = "Jeju Walk",
            ),
        )

        val events = auditEventPort.events.filter { it.action == "TOUR_CREATED" }
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("TOUR", event.resourceType)
        assertEquals("OPERATOR:${owner.id}", event.actor)
        assertNotNull(event.afterJson)
    }

    @Test
    fun `update tour appends TOUR_UPDATED audit event`() {
        val tour =
            tourCommandService.create(
                CreateTourCommand(
                    actorUserId = requireNotNull(owner.id),
                    organizationId = organizationId,
                    title = "Jeju Walk",
                ),
            )
        auditEventPort.clear()

        tourCommandService.update(
            UpdateTourCommand(
                actorUserId = requireNotNull(owner.id),
                tourId = requireNotNull(tour.id),
                title = "Jeju Walk Updated",
            ),
        )

        val events = auditEventPort.events.filter { it.action == "TOUR_UPDATED" }
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("TOUR", event.resourceType)
        assertEquals("OPERATOR:${owner.id}", event.actor)
        assertNotNull(event.beforeJson)
        assertNotNull(event.afterJson)
    }

    @Test
    fun `publish tour appends TOUR_PUBLISHED audit event`() {
        val tour =
            tourCommandService.create(
                CreateTourCommand(
                    actorUserId = requireNotNull(owner.id),
                    organizationId = organizationId,
                    title = "Jeju Walk",
                ),
            )
        auditEventPort.clear()

        tourCommandService.publish(
            PublishTourCommand(
                actorUserId = requireNotNull(owner.id),
                tourId = requireNotNull(tour.id),
            ),
        )

        val events = auditEventPort.events.filter { it.action == "TOUR_PUBLISHED" }
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("TOUR", event.resourceType)
        assertEquals("OPERATOR:${owner.id}", event.actor)
        assertNotNull(event.beforeJson)
        assertNotNull(event.afterJson)
    }

    @Test
    fun `archive published tour appends TOUR_ARCHIVED audit event`() {
        val tour =
            tourCommandService.create(
                CreateTourCommand(
                    actorUserId = requireNotNull(owner.id),
                    organizationId = organizationId,
                    title = "Jeju Walk",
                ),
            )
        tourCommandService.publish(
            PublishTourCommand(
                actorUserId = requireNotNull(owner.id),
                tourId = requireNotNull(tour.id),
            ),
        )
        auditEventPort.clear()

        tourCommandService.archive(
            ArchiveTourCommand(
                actorUserId = requireNotNull(owner.id),
                tourId = requireNotNull(tour.id),
            ),
        )

        val events = auditEventPort.events.filter { it.action == "TOUR_ARCHIVED" }
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("TOUR", event.resourceType)
        assertEquals("OPERATOR:${owner.id}", event.actor)
        assertNotNull(event.beforeJson)
        assertNotNull(event.afterJson)
    }

    @Test
    fun `archive draft tour throws INVALID_STATE_TRANSITION`() {
        val tour =
            tourCommandService.create(
                CreateTourCommand(
                    actorUserId = requireNotNull(owner.id),
                    organizationId = organizationId,
                    title = "Draft Tour",
                ),
            )

        org.junit.jupiter.api.assertThrows<com.demo.tourwave.domain.common.DomainException> {
            tourCommandService.archive(
                ArchiveTourCommand(
                    actorUserId = requireNotNull(owner.id),
                    tourId = requireNotNull(tour.id),
                ),
            )
        }
    }

    @Test
    fun `archive already archived tour is idempotent no-op`() {
        val tour =
            tourCommandService.create(
                CreateTourCommand(
                    actorUserId = requireNotNull(owner.id),
                    organizationId = organizationId,
                    title = "Jeju Walk",
                ),
            )
        tourCommandService.publish(
            PublishTourCommand(
                actorUserId = requireNotNull(owner.id),
                tourId = requireNotNull(tour.id),
            ),
        )
        tourCommandService.archive(
            ArchiveTourCommand(
                actorUserId = requireNotNull(owner.id),
                tourId = requireNotNull(tour.id),
            ),
        )
        auditEventPort.clear()

        // Re-archive same tour — idempotent no-op
        tourCommandService.archive(
            ArchiveTourCommand(
                actorUserId = requireNotNull(owner.id),
                tourId = requireNotNull(tour.id),
            ),
        )

        val events = auditEventPort.events.filter { it.action == "TOUR_ARCHIVED" }
        assertEquals(0, events.size, "Re-archive should emit no audit event")
    }
}
