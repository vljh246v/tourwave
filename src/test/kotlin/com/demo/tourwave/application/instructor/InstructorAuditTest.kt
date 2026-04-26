package com.demo.tourwave.application.instructor

import com.demo.tourwave.adapter.out.persistence.instructor.InMemoryInstructorProfileRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.instructor.InMemoryInstructorRegistrationRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.organization.InMemoryOrganizationMembershipRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.organization.InMemoryOrganizationRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.user.UserQueryAdapter
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.domain.instructor.InstructorRegistration
import com.demo.tourwave.domain.instructor.InstructorRegistrationStatus
import com.demo.tourwave.domain.organization.Organization
import com.demo.tourwave.domain.organization.OrganizationMembership
import com.demo.tourwave.domain.organization.OrganizationRole
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.support.FakeAuditEventPort
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class InstructorAuditTest {
    private val clock = Clock.fixed(Instant.parse("2026-03-19T10:00:00Z"), ZoneOffset.UTC)
    private val registrationRepository = InMemoryInstructorRegistrationRepositoryAdapter()
    private val profileRepository = InMemoryInstructorProfileRepositoryAdapter()
    private val organizationRepository = InMemoryOrganizationRepositoryAdapter()
    private val membershipRepository = InMemoryOrganizationMembershipRepositoryAdapter()
    private val userRepository = UserQueryAdapter()
    private val auditEventPort = FakeAuditEventPort()
    private val accessGuard = OrganizationAccessGuard(organizationRepository, membershipRepository)
    private val registrationService =
        InstructorRegistrationService(
            registrationRepository = registrationRepository,
            instructorProfileRepository = profileRepository,
            organizationRepository = organizationRepository,
            organizationAccessGuard = accessGuard,
            userRepository = userRepository,
            auditEventPort = auditEventPort,
            clock = clock,
        )
    private val profileService =
        InstructorProfileService(
            instructorProfileRepository = profileRepository,
            instructorRegistrationRepository = registrationRepository,
            userRepository = userRepository,
            auditEventPort = auditEventPort,
            clock = clock,
        )

    private lateinit var owner: User
    private lateinit var instructor: User
    private var organizationId: Long = 0L

    @BeforeEach
    fun setUp() {
        registrationRepository.clear()
        profileRepository.clear()
        organizationRepository.clear()
        membershipRepository.clear()
        userRepository.clear()
        auditEventPort.clear()

        owner = userRepository.save(User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = clock.instant()))
        instructor = userRepository.save(User.create(displayName = "Guide", email = "guide@test.com", passwordHash = "hash", now = clock.instant()))
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
    fun `apply registration appends INSTRUCTOR_REGISTRATION_SUBMITTED audit event`() {
        registrationService.apply(
            ApplyInstructorRegistrationCommand(
                actorUserId = requireNotNull(instructor.id),
                organizationId = organizationId,
                headline = "City guide",
                languages = listOf("ko"),
                specialties = listOf("history"),
            ),
        )

        val events = auditEventPort.events.filter { it.action == "INSTRUCTOR_REGISTRATION_SUBMITTED" }
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("INSTRUCTOR_REGISTRATION", event.resourceType)
        assertEquals("USER:${instructor.id}", event.actor)
        assertNotNull(event.afterJson)
    }

    @Test
    fun `approve registration appends INSTRUCTOR_REGISTRATION_APPROVED audit event`() {
        val registration =
            registrationService.apply(
                ApplyInstructorRegistrationCommand(
                    actorUserId = requireNotNull(instructor.id),
                    organizationId = organizationId,
                    headline = "City guide",
                    languages = listOf("ko"),
                    specialties = listOf("history"),
                ),
            )
        auditEventPort.clear()

        registrationService.approve(
            ReviewInstructorRegistrationCommand(
                actorUserId = requireNotNull(owner.id),
                registrationId = requireNotNull(registration.id),
            ),
        )

        val events = auditEventPort.events.filter { it.action == "INSTRUCTOR_REGISTRATION_APPROVED" }
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("INSTRUCTOR_REGISTRATION", event.resourceType)
        assertEquals("OPERATOR:${owner.id}", event.actor)
        assertNotNull(event.beforeJson)
        assertNotNull(event.afterJson)
    }

    @Test
    fun `reject registration appends INSTRUCTOR_REGISTRATION_REJECTED audit event`() {
        val registration =
            registrationService.apply(
                ApplyInstructorRegistrationCommand(
                    actorUserId = requireNotNull(instructor.id),
                    organizationId = organizationId,
                    headline = "City guide",
                ),
            )
        auditEventPort.clear()

        registrationService.reject(
            ReviewInstructorRegistrationCommand(
                actorUserId = requireNotNull(owner.id),
                registrationId = requireNotNull(registration.id),
                rejectionReason = "Not enough experience",
            ),
        )

        val events = auditEventPort.events.filter { it.action == "INSTRUCTOR_REGISTRATION_REJECTED" }
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("INSTRUCTOR_REGISTRATION", event.resourceType)
        assertEquals("OPERATOR:${owner.id}", event.actor)
        assertNotNull(event.beforeJson)
        assertNotNull(event.afterJson)
    }

    @Test
    fun `approve registration for new instructor also appends INSTRUCTOR_PROFILE_CREATED audit event`() {
        val registration =
            registrationService.apply(
                ApplyInstructorRegistrationCommand(
                    actorUserId = requireNotNull(instructor.id),
                    organizationId = organizationId,
                    headline = "City guide",
                    languages = listOf("ko"),
                    specialties = listOf("history"),
                ),
            )
        auditEventPort.clear()

        registrationService.approve(
            ReviewInstructorRegistrationCommand(
                actorUserId = requireNotNull(owner.id),
                registrationId = requireNotNull(registration.id),
            ),
        )

        val approvedEvents = auditEventPort.events.filter { it.action == "INSTRUCTOR_REGISTRATION_APPROVED" }
        assertEquals(1, approvedEvents.size, "should emit INSTRUCTOR_REGISTRATION_APPROVED")

        val profileCreatedEvents = auditEventPort.events.filter { it.action == "INSTRUCTOR_PROFILE_CREATED" }
        assertEquals(1, profileCreatedEvents.size, "should emit INSTRUCTOR_PROFILE_CREATED when no existing profile")
        val profileEvent = profileCreatedEvents.first()
        assertEquals("INSTRUCTOR_PROFILE", profileEvent.resourceType)
        assertEquals("OPERATOR:${owner.id}", profileEvent.actor)
        assertNotNull(profileEvent.afterJson)
    }

    @Test
    fun `approve registration for existing instructor does not append INSTRUCTOR_PROFILE_CREATED`() {
        val registration =
            registrationService.apply(
                ApplyInstructorRegistrationCommand(
                    actorUserId = requireNotNull(instructor.id),
                    organizationId = organizationId,
                    headline = "City guide",
                    languages = listOf("ko"),
                    specialties = listOf("history"),
                ),
            )
        // First approval creates profile
        registrationService.approve(
            ReviewInstructorRegistrationCommand(
                actorUserId = requireNotNull(owner.id),
                registrationId = requireNotNull(registration.id),
            ),
        )
        // Re-apply (as REJECTED logic flow is needed — skip this edge case,
        // instead test directly by saving a second registration in PENDING state)
        val secondRegistration =
            registrationRepository.save(
                com.demo.tourwave.domain.instructor.InstructorRegistration(
                    organizationId = organizationId,
                    userId = requireNotNull(instructor.id),
                    headline = "Updated guide",
                    languages = listOf("ko", "en"),
                    specialties = listOf("history"),
                    status = com.demo.tourwave.domain.instructor.InstructorRegistrationStatus.PENDING,
                    createdAt = clock.instant(),
                    updatedAt = clock.instant(),
                ),
            )
        auditEventPort.clear()

        registrationService.approve(
            ReviewInstructorRegistrationCommand(
                actorUserId = requireNotNull(owner.id),
                registrationId = requireNotNull(secondRegistration.id),
            ),
        )

        val approvedEvents = auditEventPort.events.filter { it.action == "INSTRUCTOR_REGISTRATION_APPROVED" }
        assertEquals(1, approvedEvents.size, "should still emit INSTRUCTOR_REGISTRATION_APPROVED")

        val profileCreatedEvents = auditEventPort.events.filter { it.action == "INSTRUCTOR_PROFILE_CREATED" }
        assertEquals(0, profileCreatedEvents.size, "should NOT emit INSTRUCTOR_PROFILE_CREATED when profile already exists")
    }

    @Test
    fun `createMyProfile appends INSTRUCTOR_PROFILE_CREATED audit event`() {
        // Save an approved registration directly (bypassing approve() which would auto-create the profile)
        registrationRepository.save(
            InstructorRegistration(
                organizationId = organizationId,
                userId = requireNotNull(instructor.id),
                headline = "City guide",
                languages = listOf("ko"),
                specialties = listOf("history"),
                status = InstructorRegistrationStatus.APPROVED,
                reviewedByUserId = requireNotNull(owner.id),
                createdAt = clock.instant(),
                updatedAt = clock.instant(),
            ),
        )
        auditEventPort.clear()

        profileService.createMyProfile(
            UpsertInstructorProfileCommand(
                actorUserId = requireNotNull(instructor.id),
                organizationId = organizationId,
                headline = "City guide",
                languages = listOf("ko"),
                specialties = listOf("history"),
            ),
        )

        val events = auditEventPort.events.filter { it.action == "INSTRUCTOR_PROFILE_CREATED" }
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("INSTRUCTOR_PROFILE", event.resourceType)
        assertEquals("USER:${instructor.id}", event.actor)
        assertNotNull(event.afterJson)
    }

    @Test
    fun `updateMyProfile appends INSTRUCTOR_PROFILE_UPDATED audit event`() {
        // Save an approved registration and create profile (bypassing approve() side effect chain)
        registrationRepository.save(
            InstructorRegistration(
                organizationId = organizationId,
                userId = requireNotNull(instructor.id),
                headline = "City guide",
                languages = listOf("ko"),
                specialties = listOf("history"),
                status = InstructorRegistrationStatus.APPROVED,
                reviewedByUserId = requireNotNull(owner.id),
                createdAt = clock.instant(),
                updatedAt = clock.instant(),
            ),
        )
        profileService.createMyProfile(
            UpsertInstructorProfileCommand(
                actorUserId = requireNotNull(instructor.id),
                organizationId = organizationId,
                headline = "City guide",
                languages = listOf("ko"),
                specialties = listOf("history"),
            ),
        )
        auditEventPort.clear()

        profileService.updateMyProfile(
            UpsertInstructorProfileCommand(
                actorUserId = requireNotNull(instructor.id),
                organizationId = organizationId,
                headline = "Updated headline",
                languages = listOf("ko", "en"),
                specialties = listOf("history", "food"),
            ),
        )

        val events = auditEventPort.events.filter { it.action == "INSTRUCTOR_PROFILE_UPDATED" }
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("INSTRUCTOR_PROFILE", event.resourceType)
        assertEquals("USER:${instructor.id}", event.actor)
        assertNotNull(event.beforeJson)
        assertNotNull(event.afterJson)
    }
}
