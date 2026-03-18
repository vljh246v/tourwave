package com.demo.tourwave.application.topology

import com.demo.tourwave.adapter.out.persistence.auth.InMemoryUserActionTokenRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.customer.FakeEmailNotificationChannelAdapter
import com.demo.tourwave.adapter.out.persistence.customer.InMemoryNotificationDeliveryRepositoryAdapter
import com.demo.tourwave.application.auth.ActionTokenGenerator
import com.demo.tourwave.application.auth.UserActionTokenService
import com.demo.tourwave.application.customer.NotificationDeliveryService
import com.demo.tourwave.application.customer.NotificationTemplateFactory
import com.demo.tourwave.domain.organization.Organization
import com.demo.tourwave.domain.organization.OrganizationMembership
import com.demo.tourwave.domain.organization.OrganizationRole
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.support.FakeOrganizationMembershipRepository
import com.demo.tourwave.support.FakeOrganizationRepository
import com.demo.tourwave.support.FakeUserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OrganizationMembershipServiceTest {
    private val clock = Clock.fixed(Instant.parse("2026-03-19T00:00:00Z"), ZoneOffset.UTC)
    private val membershipRepository = FakeOrganizationMembershipRepository()
    private val userRepository = FakeUserRepository()
    private val organizationRepository = FakeOrganizationRepository()
    private val deliveryRepository = InMemoryNotificationDeliveryRepositoryAdapter()
    private val actionTokenRepository = InMemoryUserActionTokenRepositoryAdapter()
    private val service =
        OrganizationMembershipService(
            membershipRepository = membershipRepository,
            userRepository = userRepository,
            organizationAccessGuard = OrganizationAccessGuard(organizationRepository, membershipRepository),
            organizationInvitationDeliveryService =
                OrganizationInvitationDeliveryService(
                    userRepository = userRepository,
                    organizationRepository = organizationRepository,
                    userActionTokenService =
                        UserActionTokenService(
                            userActionTokenRepository = actionTokenRepository,
                            actionTokenGenerator = { "org-invite-token" },
                            clock = clock,
                        ),
                    notificationDeliveryService =
                        NotificationDeliveryService(
                            notificationDeliveryRepository = deliveryRepository,
                            notificationChannelPort = FakeEmailNotificationChannelAdapter(),
                            clock = clock,
                        ),
                    notificationTemplateFactory = NotificationTemplateFactory(),
                    appBaseUrl = "https://app.test",
                    invitationTokenTtl = java.time.Duration.ofDays(7),
                    clock = clock,
                ),
            clock = clock,
        )

    @BeforeEach
    fun setUp() {
        membershipRepository.clear()
        userRepository.clear()
        organizationRepository.clear()
        deliveryRepository.clear()
        actionTokenRepository.clear()
        val owner =
            userRepository.save(
                User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = clock.instant()),
            )
        val invitee =
            userRepository.save(
                User.create(displayName = "Invitee", email = "invitee@test.com", passwordHash = "hash", now = clock.instant()),
            )
        val organization =
            organizationRepository.save(
                Organization.create(
                    slug = "org-a",
                    name = "Org A",
                    description = null,
                    publicDescription = null,
                    contactEmail = null,
                    contactPhone = null,
                    websiteUrl = null,
                    businessName = null,
                    businessRegistrationNumber = null,
                    timezone = "Asia/Seoul",
                    now = clock.instant(),
                ),
            )
        membershipRepository.save(
            OrganizationMembership.active(
                organizationId = requireNotNull(organization.id),
                userId = requireNotNull(owner.id),
                role = OrganizationRole.OWNER,
                now = clock.instant(),
            ),
        )
    }

    @Test
    fun `invite sends delivery and accept consumes token`() {
        val invited =
            service.invite(
                InviteOrganizationMemberCommand(
                    actorUserId = 1L,
                    organizationId = 1L,
                    userId = 2L,
                    role = OrganizationRole.MEMBER,
                ),
            )

        assertEquals("INVITED", invited.status.name)
        assertEquals(1, deliveryRepository.findAll().size)

        val accepted =
            service.acceptInvitation(
                AcceptOrganizationInvitationCommand(
                    actorUserId = 2L,
                    organizationId = 1L,
                    token = "org-invite-token",
                ),
            )

        assertEquals("ACTIVE", accepted.status.name)
    }

    @Test
    fun `accept rejects token for different actor`() {
        service.invite(
            InviteOrganizationMemberCommand(
                actorUserId = 1L,
                organizationId = 1L,
                userId = 2L,
                role = OrganizationRole.MEMBER,
            ),
        )

        val ex =
            assertFailsWith<com.demo.tourwave.domain.common.DomainException> {
                service.acceptInvitation(
                    AcceptOrganizationInvitationCommand(
                        actorUserId = 1L,
                        organizationId = 1L,
                        token = "org-invite-token",
                    ),
                )
            }

        assertEquals(403, ex.status)
    }
}
