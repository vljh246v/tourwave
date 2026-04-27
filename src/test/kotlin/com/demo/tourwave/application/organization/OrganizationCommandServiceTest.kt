package com.demo.tourwave.application.organization

import com.demo.tourwave.adapter.out.persistence.auth.InMemoryUserActionTokenRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.customer.FakeEmailNotificationChannelAdapter
import com.demo.tourwave.adapter.out.persistence.customer.InMemoryNotificationDeliveryRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.idempotency.InMemoryIdempotencyStoreAdapter
import com.demo.tourwave.adapter.out.persistence.organization.InMemoryOrganizationMembershipRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.organization.InMemoryOrganizationRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.user.UserQueryAdapter
import com.demo.tourwave.application.auth.UserActionTokenService
import com.demo.tourwave.application.customer.NotificationDeliveryService
import com.demo.tourwave.application.customer.NotificationTemplateFactory
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.organization.OrganizationMembershipStatus
import com.demo.tourwave.domain.organization.OrganizationRole
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.support.FakeAuditEventPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class OrganizationCommandServiceTest {
    private val clock = Clock.fixed(Instant.parse("2026-03-17T10:00:00Z"), ZoneOffset.UTC)
    private val organizationRepository = InMemoryOrganizationRepositoryAdapter()
    private val membershipRepository = InMemoryOrganizationMembershipRepositoryAdapter()
    private val userRepository = UserQueryAdapter()
    private val auditEventPort = FakeAuditEventPort()
    private val idempotencyStore = InMemoryIdempotencyStoreAdapter()
    private val accessGuard = OrganizationAccessGuard(organizationRepository, membershipRepository)
    private val invitationDeliveryService =
        OrganizationInvitationDeliveryService(
            userRepository = userRepository,
            organizationRepository = organizationRepository,
            userActionTokenService =
                UserActionTokenService(
                    userActionTokenRepository = InMemoryUserActionTokenRepositoryAdapter(),
                    actionTokenGenerator = { "org-invite-token" },
                    clock = clock,
                ),
            notificationDeliveryService =
                NotificationDeliveryService(
                    notificationDeliveryRepository = InMemoryNotificationDeliveryRepositoryAdapter(),
                    notificationChannelPort = FakeEmailNotificationChannelAdapter(),
                    clock = clock,
                ),
            notificationTemplateFactory = NotificationTemplateFactory(),
            appBaseUrl = "https://app.test",
            invitationTokenTtl = java.time.Duration.ofDays(7),
            clock = clock,
        )
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
    private val membershipService =
        OrganizationMembershipService(
            membershipRepository = membershipRepository,
            userRepository = userRepository,
            organizationAccessGuard = accessGuard,
            organizationInvitationDeliveryService = invitationDeliveryService,
            auditEventPort = auditEventPort,
            idempotencyStore = idempotencyStore,
            clock = clock,
        )
    private val queryService =
        OrganizationQueryService(
            organizationRepository = organizationRepository,
            membershipRepository = membershipRepository,
            organizationAccessGuard = accessGuard,
        )

    @BeforeEach
    fun setUp() {
        idempotencyStore.clear()
        organizationRepository.clear()
        membershipRepository.clear()
        userRepository.clear()
    }

    @Test
    fun `create organization creates owner membership and public profile`() {
        val owner =
            userRepository.save(
                User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = clock.instant()),
            )

        val created =
            organizationCommandService.createOrganization(
                CreateOrganizationCommand(
                    actorUserId = requireNotNull(owner.id),
                    slug = "seoul-walkers",
                    name = "Seoul Walkers",
                    publicDescription = "City walks",
                    timezone = "Asia/Seoul",
                    contactEmail = "ops@seoul.test",
                    idempotencyKey = "create-org-001",
                ),
            )

        val membership = membershipRepository.findByOrganizationIdAndUserId(requireNotNull(created.id), requireNotNull(owner.id))
        assertEquals(OrganizationRole.OWNER, membership?.role)
        assertEquals(OrganizationMembershipStatus.ACTIVE, membership?.status)
        assertEquals("City walks", queryService.getPublicOrganization(requireNotNull(created.id)).publicDescription)
    }

    @Test
    fun `membership lifecycle supports invite accept role change and deactivate`() {
        val owner =
            userRepository.save(
                User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = clock.instant()),
            )
        val member =
            userRepository.save(
                User.create(displayName = "Member", email = "member@test.com", passwordHash = "hash", now = clock.instant()),
            )
        val organization =
            organizationCommandService.createOrganization(
                CreateOrganizationCommand(
                    actorUserId = requireNotNull(owner.id),
                    slug = "busan-guides",
                    name = "Busan Guides",
                    timezone = "Asia/Seoul",
                    idempotencyKey = "create-org-busan-001",
                ),
            )
        val organizationId = requireNotNull(organization.id)

        val invited =
            membershipService.invite(
                InviteOrganizationMemberCommand(
                    actorUserId = requireNotNull(owner.id),
                    organizationId = organizationId,
                    userId = requireNotNull(member.id),
                    role = OrganizationRole.MEMBER,
                    idempotencyKey = "invite-member-001",
                ),
            )
        assertEquals(OrganizationMembershipStatus.INVITED, invited.status)

        val accepted =
            membershipService.acceptInvitation(
                AcceptOrganizationInvitationCommand(
                    actorUserId = requireNotNull(member.id),
                    organizationId = organizationId,
                ),
            )
        assertEquals(OrganizationMembershipStatus.ACTIVE, accepted.status)

        val promoted =
            membershipService.changeRole(
                ChangeOrganizationMemberRoleCommand(
                    actorUserId = requireNotNull(owner.id),
                    organizationId = organizationId,
                    memberUserId = requireNotNull(member.id),
                    role = OrganizationRole.ADMIN,
                    idempotencyKey = "change-role-001",
                ),
            )
        assertEquals(OrganizationRole.ADMIN, promoted.role)

        val deactivated =
            membershipService.deactivate(
                DeactivateOrganizationMemberCommand(
                    actorUserId = requireNotNull(owner.id),
                    organizationId = organizationId,
                    memberUserId = requireNotNull(member.id),
                    idempotencyKey = "deactivate-member-001",
                ),
            )
        assertEquals(OrganizationMembershipStatus.INACTIVE, deactivated.status)
    }

    @Test
    fun `admin cannot assign owner role or deactivate self`() {
        val owner =
            userRepository.save(
                User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = clock.instant()),
            )
        val admin =
            userRepository.save(
                User.create(displayName = "Admin", email = "admin@test.com", passwordHash = "hash", now = clock.instant()),
            )
        val organization =
            organizationCommandService.createOrganization(
                CreateOrganizationCommand(
                    actorUserId = requireNotNull(owner.id),
                    slug = "jeju-trails",
                    name = "Jeju Trails",
                    timezone = "Asia/Seoul",
                    idempotencyKey = "create-org-jeju-001",
                ),
            )
        val organizationId = requireNotNull(organization.id)

        membershipService.invite(
            InviteOrganizationMemberCommand(
                actorUserId = requireNotNull(owner.id),
                organizationId = organizationId,
                userId = requireNotNull(admin.id),
                role = OrganizationRole.ADMIN,
                idempotencyKey = "invite-admin-001",
            ),
        )
        membershipService.acceptInvitation(AcceptOrganizationInvitationCommand(requireNotNull(admin.id), organizationId))

        assertThrows(DomainException::class.java) {
            membershipService.invite(
                InviteOrganizationMemberCommand(
                    actorUserId = requireNotNull(admin.id),
                    organizationId = organizationId,
                    userId = requireNotNull(owner.id),
                    role = OrganizationRole.OWNER,
                    idempotencyKey = "invite-owner-forbidden-001",
                ),
            )
        }
        assertThrows(DomainException::class.java) {
            membershipService.deactivate(
                DeactivateOrganizationMemberCommand(
                    actorUserId = requireNotNull(admin.id),
                    organizationId = organizationId,
                    memberUserId = requireNotNull(admin.id),
                    idempotencyKey = "deactivate-self-forbidden-001",
                ),
            )
        }
    }
}
