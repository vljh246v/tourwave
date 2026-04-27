package com.demo.tourwave.application.announcement

import com.demo.tourwave.adapter.out.persistence.announcement.InMemoryAnnouncementRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.idempotency.InMemoryIdempotencyStoreAdapter
import com.demo.tourwave.adapter.out.persistence.organization.InMemoryOrganizationMembershipRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.organization.InMemoryOrganizationRepositoryAdapter
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.domain.announcement.AnnouncementVisibility
import com.demo.tourwave.domain.common.DomainException
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
import kotlin.test.assertFailsWith

class AnnouncementServiceTest {
    private val announcementRepository = InMemoryAnnouncementRepositoryAdapter()
    private val organizationRepository = InMemoryOrganizationRepositoryAdapter()
    private val membershipRepository = InMemoryOrganizationMembershipRepositoryAdapter()
    private val auditEventPort = FakeAuditEventPort()
    private val idempotencyStore = InMemoryIdempotencyStoreAdapter()
    private val fixedClock = Clock.fixed(Instant.parse("2026-03-19T09:00:00Z"), ZoneOffset.UTC)
    private val service =
        AnnouncementService(
            announcementRepository = announcementRepository,
            organizationAccessGuard = OrganizationAccessGuard(organizationRepository, membershipRepository),
            auditEventPort = auditEventPort,
            idempotencyStore = idempotencyStore,
            clock = fixedClock,
        )

    @BeforeEach
    fun setUp() {
        idempotencyStore.clear()
        announcementRepository.clear()
        membershipRepository.clear()
        organizationRepository.clear()
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
    fun `public listing exposes only currently visible public announcements`() {
        service.create(
            CreateAnnouncementCommand(
                actorUserId = 11L,
                organizationId = 1L,
                title = "Visible",
                body = "Body",
                visibility = AnnouncementVisibility.PUBLIC,
                publishStartsAtUtc = fixedClock.instant().minusSeconds(60),
                publishEndsAtUtc = fixedClock.instant().plusSeconds(60),
                idempotencyKey = "idem-visible",
            ),
        )
        service.create(
            CreateAnnouncementCommand(
                actorUserId = 11L,
                organizationId = 1L,
                title = "Draft",
                body = "Body",
                visibility = AnnouncementVisibility.DRAFT,
                publishStartsAtUtc = null,
                publishEndsAtUtc = null,
                idempotencyKey = "idem-draft",
            ),
        )
        service.create(
            CreateAnnouncementCommand(
                actorUserId = 11L,
                organizationId = 1L,
                title = "Future",
                body = "Body",
                visibility = AnnouncementVisibility.PUBLIC,
                publishStartsAtUtc = fixedClock.instant().plusSeconds(3600),
                publishEndsAtUtc = null,
                idempotencyKey = "idem-future",
            ),
        )

        val page = service.listPublicAnnouncements(organizationId = 1L, cursor = null, limit = 20)

        assertEquals(1, page.items.size)
        assertEquals("Visible", page.items.single().title)
    }

    @Test
    fun `announcement create requires operator membership`() {
        val exception =
            assertFailsWith<DomainException> {
                service.create(
                    CreateAnnouncementCommand(
                        actorUserId = 99L,
                        organizationId = 1L,
                        title = "Title",
                        body = "Body",
                        visibility = AnnouncementVisibility.PUBLIC,
                        publishStartsAtUtc = null,
                        publishEndsAtUtc = null,
                        idempotencyKey = "idem-forbidden",
                    ),
                )
            }

        assertEquals(403, exception.status)
    }
}
