package com.demo.tourwave.application.instructor

import com.demo.tourwave.adapter.out.persistence.auth.InMemoryUserActionTokenRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.customer.FakeEmailNotificationChannelAdapter
import com.demo.tourwave.adapter.out.persistence.customer.InMemoryNotificationDeliveryRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.instructor.InMemoryInstructorProfileRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.instructor.InMemoryInstructorRegistrationRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.organization.InMemoryOrganizationMembershipRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.organization.InMemoryOrganizationRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.user.UserQueryAdapter
import com.demo.tourwave.application.auth.UserActionTokenService
import com.demo.tourwave.application.customer.NotificationDeliveryService
import com.demo.tourwave.application.customer.NotificationTemplateFactory
import com.demo.tourwave.application.organization.AcceptOrganizationInvitationCommand
import com.demo.tourwave.application.organization.CreateOrganizationCommand
import com.demo.tourwave.application.organization.InviteOrganizationMemberCommand
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.organization.OrganizationCommandService
import com.demo.tourwave.application.organization.OrganizationInvitationDeliveryService
import com.demo.tourwave.application.organization.OrganizationMembershipService
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.instructor.InstructorRegistrationStatus
import com.demo.tourwave.domain.organization.OrganizationRole
import com.demo.tourwave.domain.user.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class InstructorRegistrationServiceTest {
    private val clock = Clock.fixed(Instant.parse("2026-03-18T00:00:00Z"), ZoneOffset.UTC)
    private val organizationRepository = InMemoryOrganizationRepositoryAdapter()
    private val membershipRepository = InMemoryOrganizationMembershipRepositoryAdapter()
    private val registrationRepository = InMemoryInstructorRegistrationRepositoryAdapter()
    private val instructorProfileRepository = InMemoryInstructorProfileRepositoryAdapter()
    private val userRepository = UserQueryAdapter()
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
            clock = clock,
        )
    private val membershipService =
        OrganizationMembershipService(
            membershipRepository = membershipRepository,
            userRepository = userRepository,
            organizationAccessGuard = accessGuard,
            organizationInvitationDeliveryService = invitationDeliveryService,
            clock = clock,
        )
    private val registrationService =
        InstructorRegistrationService(
            registrationRepository = registrationRepository,
            instructorProfileRepository = instructorProfileRepository,
            organizationRepository = organizationRepository,
            organizationAccessGuard = accessGuard,
            userRepository = userRepository,
            clock = clock,
        )
    private val profileService =
        InstructorProfileService(
            instructorProfileRepository = instructorProfileRepository,
            instructorRegistrationRepository = registrationRepository,
            userRepository = userRepository,
            clock = clock,
        )

    @BeforeEach
    fun setUp() {
        instructorProfileRepository.clear()
        registrationRepository.clear()
        membershipRepository.clear()
        organizationRepository.clear()
        userRepository.clear()
    }

    @Test
    fun `registration workflow supports apply approve and profile update`() {
        val owner =
            userRepository.save(
                User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = clock.instant()),
            )
        val instructor =
            userRepository.save(
                User.create(displayName = "Guide", email = "guide@test.com", passwordHash = "hash", now = clock.instant()),
            )
        val organization =
            organizationCommandService.createOrganization(
                CreateOrganizationCommand(
                    actorUserId = requireNotNull(owner.id),
                    slug = "seoul-guides",
                    name = "Seoul Guides",
                    timezone = "Asia/Seoul",
                ),
            )

        val registration =
            registrationService.apply(
                ApplyInstructorRegistrationCommand(
                    actorUserId = requireNotNull(instructor.id),
                    organizationId = requireNotNull(organization.id),
                    headline = "City storyteller",
                    languages = listOf("ko", "en"),
                    specialties = listOf("history", "food"),
                ),
            )
        assertEquals(InstructorRegistrationStatus.PENDING, registration.status)

        val approved =
            registrationService.approve(
                ReviewInstructorRegistrationCommand(
                    actorUserId = requireNotNull(owner.id),
                    registrationId = requireNotNull(registration.id),
                ),
            )
        assertEquals(InstructorRegistrationStatus.APPROVED, approved.status)

        val createdProfile = profileService.getMyProfile(requireNotNull(instructor.id), requireNotNull(organization.id))
        assertEquals(listOf("ko", "en"), createdProfile.languages)

        val updatedProfile =
            profileService.updateMyProfile(
                UpsertInstructorProfileCommand(
                    actorUserId = requireNotNull(instructor.id),
                    organizationId = requireNotNull(organization.id),
                    headline = "Lead storyteller",
                    bio = "Evening specialist",
                    languages = listOf("ko", "en"),
                    specialties = listOf("history"),
                    certifications = listOf("first aid"),
                    yearsOfExperience = 7,
                    internalNote = "operator note",
                ),
            )
        assertEquals("Lead storyteller", updatedProfile.headline)
        assertEquals(listOf("first aid"), updatedProfile.certifications)
    }

    @Test
    fun `rejected registration can be resubmitted and operator access is enforced`() {
        val owner =
            userRepository.save(
                User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = clock.instant()),
            )
        val member =
            userRepository.save(
                User.create(displayName = "Member", email = "member@test.com", passwordHash = "hash", now = clock.instant()),
            )
        val instructor =
            userRepository.save(
                User.create(displayName = "Guide", email = "guide@test.com", passwordHash = "hash", now = clock.instant()),
            )
        val organization =
            organizationCommandService.createOrganization(
                CreateOrganizationCommand(
                    actorUserId = requireNotNull(owner.id),
                    slug = "busan-guides",
                    name = "Busan Guides",
                    timezone = "Asia/Seoul",
                ),
            )
        membershipService.invite(
            InviteOrganizationMemberCommand(
                actorUserId = requireNotNull(owner.id),
                organizationId = requireNotNull(organization.id),
                userId = requireNotNull(member.id),
                role = OrganizationRole.MEMBER,
            ),
        )
        membershipService.acceptInvitation(
            AcceptOrganizationInvitationCommand(
                actorUserId = requireNotNull(member.id),
                organizationId = requireNotNull(organization.id),
            ),
        )
        val registration =
            registrationService.apply(
                ApplyInstructorRegistrationCommand(
                    actorUserId = requireNotNull(instructor.id),
                    organizationId = requireNotNull(organization.id),
                    headline = "Busan guide",
                ),
            )

        assertThrows(DomainException::class.java) {
            registrationService.approve(
                ReviewInstructorRegistrationCommand(
                    actorUserId = requireNotNull(member.id),
                    registrationId = requireNotNull(registration.id),
                ),
            )
        }

        val rejected =
            registrationService.reject(
                ReviewInstructorRegistrationCommand(
                    actorUserId = requireNotNull(owner.id),
                    registrationId = requireNotNull(registration.id),
                    rejectionReason = "Need more experience",
                ),
            )
        assertEquals(InstructorRegistrationStatus.REJECTED, rejected.status)

        val resubmitted =
            registrationService.apply(
                ApplyInstructorRegistrationCommand(
                    actorUserId = requireNotNull(instructor.id),
                    organizationId = requireNotNull(organization.id),
                    headline = "Busan guide updated",
                    specialties = listOf("harbor"),
                ),
            )
        assertEquals(InstructorRegistrationStatus.PENDING, resubmitted.status)
        assertEquals(listOf("harbor"), resubmitted.specialties)
    }
}
