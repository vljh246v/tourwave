package com.demo.tourwave.application.announcement

import com.demo.tourwave.adapter.out.persistence.announcement.InMemoryAnnouncementRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.organization.InMemoryOrganizationMembershipRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.organization.InMemoryOrganizationRepositoryAdapter
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.domain.announcement.AnnouncementVisibility
import com.demo.tourwave.domain.organization.Organization
import com.demo.tourwave.domain.organization.OrganizationMembership
import com.demo.tourwave.domain.organization.OrganizationRole
import com.demo.tourwave.support.FakeAuditEventPort
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AnnouncementAuditTest {
    private val announcementRepository = InMemoryAnnouncementRepositoryAdapter()
    private val organizationRepository = InMemoryOrganizationRepositoryAdapter()
    private val membershipRepository = InMemoryOrganizationMembershipRepositoryAdapter()
    private val auditEventPort = FakeAuditEventPort()
    private val fixedClock = Clock.fixed(Instant.parse("2026-03-19T09:00:00Z"), ZoneOffset.UTC)
    private val service =
        AnnouncementService(
            announcementRepository = announcementRepository,
            organizationAccessGuard = OrganizationAccessGuard(organizationRepository, membershipRepository),
            auditEventPort = auditEventPort,
            clock = fixedClock,
        )

    @BeforeEach
    fun setUp() {
        announcementRepository.clear()
        membershipRepository.clear()
        organizationRepository.clear()
        auditEventPort.clear()
        organizationRepository.save(
            Organization.create(
                slug = "seoul-ops",
                name = "Seoul Ops",
                description = null,
                publicDescription = null,
                contactEmail = null,
                contactPhone = null,
                websiteUrl = null,
                businessName = null,
                businessRegistrationNumber = null,
                timezone = "Asia/Seoul",
                now = fixedClock.instant(),
            ),
        )
        membershipRepository.save(
            OrganizationMembership.active(
                organizationId = 1L,
                userId = 11L,
                role = OrganizationRole.OWNER,
                now = fixedClock.instant(),
            ),
        )
    }

    @Test
    fun `create announcement appends ANNOUNCEMENT_CREATED audit event`() {
        service.create(
            CreateAnnouncementCommand(
                actorUserId = 11L,
                organizationId = 1L,
                title = "New Announcement",
                body = "Body text",
                visibility = AnnouncementVisibility.PUBLIC,
                publishStartsAtUtc = null,
                publishEndsAtUtc = null,
            ),
        )

        assertEquals(1, auditEventPort.events.size)
        val event = auditEventPort.events.first()
        assertEquals("ANNOUNCEMENT_CREATED", event.action)
        assertEquals("ANNOUNCEMENT", event.resourceType)
        assertEquals("OPERATOR:11", event.actor)
        assertNotNull(event.afterJson)
    }

    @Test
    fun `update announcement appends ANNOUNCEMENT_UPDATED audit event`() {
        val created =
            service.create(
                CreateAnnouncementCommand(
                    actorUserId = 11L,
                    organizationId = 1L,
                    title = "Original",
                    body = "Body",
                    visibility = AnnouncementVisibility.DRAFT,
                    publishStartsAtUtc = null,
                    publishEndsAtUtc = null,
                ),
            )
        auditEventPort.clear()

        service.update(
            UpdateAnnouncementCommand(
                actorUserId = 11L,
                announcementId = requireNotNull(created.id),
                title = "Updated Title",
                body = null,
                visibility = null,
                publishStartsAtUtc = null,
                publishEndsAtUtc = null,
            ),
        )

        assertEquals(1, auditEventPort.events.size)
        val event = auditEventPort.events.first()
        assertEquals("ANNOUNCEMENT_UPDATED", event.action)
        assertEquals("ANNOUNCEMENT", event.resourceType)
        assertEquals("OPERATOR:11", event.actor)
        assertNotNull(event.beforeJson)
        assertNotNull(event.afterJson)
    }

    @Test
    fun `delete announcement appends ANNOUNCEMENT_DELETED audit event`() {
        val created =
            service.create(
                CreateAnnouncementCommand(
                    actorUserId = 11L,
                    organizationId = 1L,
                    title = "To Delete",
                    body = "Body",
                    visibility = AnnouncementVisibility.DRAFT,
                    publishStartsAtUtc = null,
                    publishEndsAtUtc = null,
                ),
            )
        auditEventPort.clear()

        service.delete(actorUserId = 11L, announcementId = requireNotNull(created.id))

        assertEquals(1, auditEventPort.events.size)
        val event = auditEventPort.events.first()
        assertEquals("ANNOUNCEMENT_DELETED", event.action)
        assertEquals("ANNOUNCEMENT", event.resourceType)
        assertEquals("OPERATOR:11", event.actor)
        assertNotNull(event.beforeJson)
    }
}
