package com.demo.tourwave.application.organization

import com.demo.tourwave.adapter.out.persistence.auth.InMemoryUserActionTokenRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.customer.FakeEmailNotificationChannelAdapter
import com.demo.tourwave.adapter.out.persistence.customer.InMemoryNotificationDeliveryRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.idempotency.InMemoryIdempotencyStoreAdapter
import com.demo.tourwave.application.auth.UserActionTokenService
import com.demo.tourwave.application.customer.NotificationDeliveryService
import com.demo.tourwave.application.customer.NotificationTemplateFactory
import com.demo.tourwave.domain.organization.Organization
import com.demo.tourwave.domain.organization.OrganizationMembership
import com.demo.tourwave.domain.organization.OrganizationRole
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.support.FakeAuditEventPort
import com.demo.tourwave.support.FakeOrganizationMembershipRepository
import com.demo.tourwave.support.FakeOrganizationRepository
import com.demo.tourwave.support.FakeUserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class OrganizationMembershipAuditTest {
    private val clock = Clock.fixed(Instant.parse("2026-03-19T10:00:00Z"), ZoneOffset.UTC)
    private val membershipRepository = FakeOrganizationMembershipRepository()
    private val userRepository = FakeUserRepository()
    private val organizationRepository = FakeOrganizationRepository()
    private val auditEventPort = FakeAuditEventPort()
    private val idempotencyStore = InMemoryIdempotencyStoreAdapter()
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
                            actionTokenGenerator = { "invite-token" },
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
                    invitationTokenTtl = Duration.ofDays(7),
                    clock = clock,
                ),
            auditEventPort = auditEventPort,
            idempotencyStore = idempotencyStore,
            clock = clock,
        )

    private lateinit var owner: User
    private lateinit var invitee: User
    private var organizationId: Long = 0L

    @BeforeEach
    fun setUp() {
        idempotencyStore.clear()
        membershipRepository.clear()
        userRepository.clear()
        organizationRepository.clear()
        auditEventPort.clear()
        deliveryRepository.clear()
        actionTokenRepository.clear()

        owner = userRepository.save(User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = clock.instant()))
        invitee = userRepository.save(User.create(displayName = "Invitee", email = "invitee@test.com", passwordHash = "hash", now = clock.instant()))
        val org =
            organizationRepository.save(
                Organization.create(
                    slug = "org-audit",
                    name = "Org Audit",
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
        organizationId = requireNotNull(org.id)
        membershipRepository.save(
            OrganizationMembership.active(
                organizationId = organizationId,
                userId = requireNotNull(owner.id),
                role = OrganizationRole.OWNER,
                now = clock.instant(),
            ),
        )
    }

    @Test
    fun `invite appends ORGANIZATION_MEMBER_INVITED audit event`() {
        service.invite(
            InviteOrganizationMemberCommand(
                actorUserId = requireNotNull(owner.id),
                organizationId = organizationId,
                userId = requireNotNull(invitee.id),
                role = OrganizationRole.MEMBER,
                idempotencyKey = "audit-invite-001",
            ),
        )

        val events = auditEventPort.events.filter { it.action == "ORGANIZATION_MEMBER_INVITED" }
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("ORGANIZATION_MEMBERSHIP", event.resourceType)
        assertEquals("OPERATOR:${owner.id}", event.actor)
    }

    @Test
    fun `changeRole appends ORGANIZATION_MEMBER_ROLE_CHANGED audit event`() {
        membershipRepository.save(
            OrganizationMembership.active(
                organizationId = organizationId,
                userId = requireNotNull(invitee.id),
                role = OrganizationRole.MEMBER,
                now = clock.instant(),
            ),
        )
        auditEventPort.clear()

        service.changeRole(
            ChangeOrganizationMemberRoleCommand(
                actorUserId = requireNotNull(owner.id),
                organizationId = organizationId,
                memberUserId = requireNotNull(invitee.id),
                role = OrganizationRole.ADMIN,
                idempotencyKey = "audit-change-role-001",
            ),
        )

        val events = auditEventPort.events.filter { it.action == "ORGANIZATION_MEMBER_ROLE_CHANGED" }
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("ORGANIZATION_MEMBERSHIP", event.resourceType)
        assertEquals("OPERATOR:${owner.id}", event.actor)
    }

    @Test
    fun `deactivate appends ORGANIZATION_MEMBER_DEACTIVATED audit event`() {
        membershipRepository.save(
            OrganizationMembership.active(
                organizationId = organizationId,
                userId = requireNotNull(invitee.id),
                role = OrganizationRole.MEMBER,
                now = clock.instant(),
            ),
        )
        auditEventPort.clear()

        service.deactivate(
            DeactivateOrganizationMemberCommand(
                actorUserId = requireNotNull(owner.id),
                organizationId = organizationId,
                memberUserId = requireNotNull(invitee.id),
                idempotencyKey = "audit-deactivate-001",
            ),
        )

        val events = auditEventPort.events.filter { it.action == "ORGANIZATION_MEMBER_DEACTIVATED" }
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("ORGANIZATION_MEMBERSHIP", event.resourceType)
        assertEquals("OPERATOR:${owner.id}", event.actor)
    }
}
