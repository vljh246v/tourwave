package com.demo.tourwave.application.topology

import com.demo.tourwave.adapter.out.persistence.topology.InMemoryOrganizationMembershipRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.topology.InMemoryOrganizationRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.topology.InMemoryTourRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.user.UserQueryAdapter
import com.demo.tourwave.adapter.out.persistence.auth.InMemoryUserActionTokenRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.customer.FakeEmailNotificationChannelAdapter
import com.demo.tourwave.adapter.out.persistence.customer.InMemoryNotificationDeliveryRepositoryAdapter
import com.demo.tourwave.application.auth.ActionTokenGenerator
import com.demo.tourwave.application.auth.UserActionTokenService
import com.demo.tourwave.application.customer.NotificationDeliveryService
import com.demo.tourwave.application.customer.NotificationTemplateFactory
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.tour.TourStatus
import com.demo.tourwave.domain.user.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TourCommandServiceTest {
    private val clock = Clock.fixed(Instant.parse("2026-03-18T00:00:00Z"), ZoneOffset.UTC)
    private val organizationRepository = InMemoryOrganizationRepositoryAdapter()
    private val membershipRepository = InMemoryOrganizationMembershipRepositoryAdapter()
    private val tourRepository = InMemoryTourRepositoryAdapter()
    private val userRepository = UserQueryAdapter()
    private val accessGuard = OrganizationAccessGuard(organizationRepository, membershipRepository)
    private val invitationDeliveryService = OrganizationInvitationDeliveryService(
        userRepository = userRepository,
        organizationRepository = organizationRepository,
        userActionTokenService = UserActionTokenService(
            userActionTokenRepository = InMemoryUserActionTokenRepositoryAdapter(),
            actionTokenGenerator = ActionTokenGenerator { "org-invite-token" },
            clock = clock
        ),
        notificationDeliveryService = NotificationDeliveryService(
            notificationDeliveryRepository = InMemoryNotificationDeliveryRepositoryAdapter(),
            notificationChannelPort = FakeEmailNotificationChannelAdapter(),
            clock = clock
        ),
        notificationTemplateFactory = NotificationTemplateFactory(),
        appBaseUrl = "https://app.test",
        invitationTokenTtl = java.time.Duration.ofDays(7),
        clock = clock
    )
    private val organizationCommandService = OrganizationCommandService(
        organizationRepository = organizationRepository,
        membershipRepository = membershipRepository,
        userRepository = userRepository,
        organizationAccessGuard = accessGuard,
        clock = clock
    )
    private val membershipService = OrganizationMembershipService(
        membershipRepository = membershipRepository,
        userRepository = userRepository,
        organizationAccessGuard = accessGuard,
        organizationInvitationDeliveryService = invitationDeliveryService,
        clock = clock
    )
    private val tourCommandService = TourCommandService(
        tourRepository = tourRepository,
        organizationRepository = organizationRepository,
        organizationAccessGuard = accessGuard,
        clock = clock
    )
    private val tourQueryService = TourQueryService(tourRepository, accessGuard)

    @BeforeEach
    fun setUp() {
        tourRepository.clear()
        membershipRepository.clear()
        organizationRepository.clear()
        userRepository.clear()
    }

    @Test
    fun `operator can create update content and publish tour`() {
        val owner = userRepository.save(User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = clock.instant()))
        val organization = organizationCommandService.createOrganization(
            CreateOrganizationCommand(
                actorUserId = requireNotNull(owner.id),
                slug = "jeju-tours",
                name = "Jeju Tours",
                timezone = "Asia/Seoul"
            )
        )

        val created = tourCommandService.create(
            CreateTourCommand(
                actorUserId = requireNotNull(owner.id),
                organizationId = requireNotNull(organization.id),
                title = "Jeju Coast Walk",
                summary = "Morning ocean route"
            )
        )
        val updated = tourCommandService.update(
            UpdateTourCommand(
                actorUserId = requireNotNull(owner.id),
                tourId = requireNotNull(created.id),
                title = "Jeju Coast Walk Updated",
                summary = "Updated summary"
            )
        )
        val contentUpdated = tourCommandService.updateContent(
            UpdateTourContentCommand(
                actorUserId = requireNotNull(owner.id),
                tourId = requireNotNull(created.id),
                description = "Tour description",
                highlights = listOf("sunrise"),
                inclusions = listOf("tea"),
                exclusions = listOf("transport"),
                preparations = listOf("walking shoes"),
                policies = listOf("24h cancellation")
            )
        )
        val published = tourCommandService.publish(
            PublishTourCommand(
                actorUserId = requireNotNull(owner.id),
                tourId = requireNotNull(created.id)
            )
        )

        assertEquals("Jeju Coast Walk Updated", updated.title)
        assertEquals(listOf("sunrise"), contentUpdated.content.highlights)
        assertEquals(TourStatus.PUBLISHED, published.status)
        assertEquals("Tour description", tourQueryService.getPublicContent(requireNotNull(created.id)).description)
    }

    @Test
    fun `member cannot manage tour`() {
        val owner = userRepository.save(User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = clock.instant()))
        val member = userRepository.save(User.create(displayName = "Member", email = "member@test.com", passwordHash = "hash", now = clock.instant()))
        val organization = organizationCommandService.createOrganization(
            CreateOrganizationCommand(
                actorUserId = requireNotNull(owner.id),
                slug = "seoul-tour-op",
                name = "Seoul Tour Op",
                timezone = "Asia/Seoul"
            )
        )
        membershipService.invite(
            InviteOrganizationMemberCommand(
                actorUserId = requireNotNull(owner.id),
                organizationId = requireNotNull(organization.id),
                userId = requireNotNull(member.id),
                role = com.demo.tourwave.domain.organization.OrganizationRole.MEMBER
            )
        )
        membershipService.acceptInvitation(
            AcceptOrganizationInvitationCommand(
                actorUserId = requireNotNull(member.id),
                organizationId = requireNotNull(organization.id)
            )
        )

        assertThrows(DomainException::class.java) {
            tourCommandService.create(
                CreateTourCommand(
                    actorUserId = requireNotNull(member.id),
                    organizationId = requireNotNull(organization.id),
                    title = "Blocked tour"
                )
            )
        }
    }
}
