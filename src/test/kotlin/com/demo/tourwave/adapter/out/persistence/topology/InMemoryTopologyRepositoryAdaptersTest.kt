package com.demo.tourwave.adapter.out.persistence.topology

import com.demo.tourwave.domain.instructor.InstructorProfile
import com.demo.tourwave.domain.instructor.InstructorProfileStatus
import com.demo.tourwave.domain.instructor.InstructorRegistration
import com.demo.tourwave.domain.organization.OrganizationMembership
import com.demo.tourwave.domain.organization.OrganizationRole
import com.demo.tourwave.domain.organization.Organization
import com.demo.tourwave.domain.tour.Tour
import com.demo.tourwave.domain.tour.TourContent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class InMemoryTopologyRepositoryAdaptersTest {
    private val organizationRepository = InMemoryOrganizationRepositoryAdapter()
    private val membershipRepository = InMemoryOrganizationMembershipRepositoryAdapter()
    private val tourRepository = InMemoryTourRepositoryAdapter()
    private val instructorProfileRepository = InMemoryInstructorProfileRepositoryAdapter()
    private val instructorRegistrationRepository = InMemoryInstructorRegistrationRepositoryAdapter()

    @BeforeEach
    fun setUp() {
        organizationRepository.clear()
        membershipRepository.clear()
        tourRepository.clear()
        instructorProfileRepository.clear()
        instructorRegistrationRepository.clear()
    }

    @Test
    fun `repositories persist and load topology entities by id`() {
        val organization = organizationRepository.save(
            Organization.create(
                slug = "tourwave-org",
                name = "Tourwave Org",
                description = "operator",
                publicDescription = "public",
                contactEmail = "ops@tourwave.test",
                contactPhone = "+82 10 0000 0000",
                websiteUrl = "https://tourwave.test",
                businessName = "Tourwave LLC",
                businessRegistrationNumber = "123-45-67890",
                timezone = "Asia/Seoul",
                now = Instant.parse("2026-03-17T00:00:00Z")
            )
        )
        val membership = membershipRepository.save(
            OrganizationMembership.active(
                organizationId = requireNotNull(organization.id),
                userId = 7L,
                role = OrganizationRole.OWNER,
                now = Instant.parse("2026-03-17T00:00:00Z")
            )
        )
        val registration = instructorRegistrationRepository.save(
            InstructorRegistration.create(
                organizationId = requireNotNull(organization.id),
                userId = 42L,
                headline = "Walking guide",
                bio = "Night route specialist",
                languages = listOf("ko", "en"),
                specialties = listOf("history"),
                now = Instant.parse("2026-03-17T00:00:00Z")
            )
        )
        val tour = tourRepository.save(
            Tour.create(
                organizationId = requireNotNull(organization.id),
                title = "Night Walk",
                summary = "after dark city walk",
                now = Instant.parse("2026-03-17T00:00:00Z")
            ).updateContent(
                TourContent(
                    description = "Tour content",
                    highlights = listOf("lantern market")
                ),
                now = Instant.parse("2026-03-17T00:30:00Z")
            )
        )
        val instructorProfile = instructorProfileRepository.save(
            InstructorProfile(
                userId = 42L,
                organizationId = requireNotNull(organization.id),
                headline = "Lead guide",
                bio = "Guide",
                languages = listOf("ko", "en"),
                specialties = listOf("history"),
                certifications = listOf("first aid"),
                yearsOfExperience = 5,
                internalNote = "internal",
                status = InstructorProfileStatus.ACTIVE,
                approvedAt = Instant.parse("2026-03-17T00:00:00Z"),
                createdAt = Instant.parse("2026-03-17T00:00:00Z"),
                updatedAt = Instant.parse("2026-03-17T00:00:00Z")
            )
        )

        assertNotNull(organization.id)
        assertNotNull(membership.id)
        assertNotNull(registration.id)
        assertNotNull(tour.id)
        assertNotNull(instructorProfile.id)
        assertEquals(organization, organizationRepository.findById(requireNotNull(organization.id)))
        assertEquals(organization, organizationRepository.findBySlug("tourwave-org"))
        assertEquals(membership, membershipRepository.findByOrganizationIdAndUserId(requireNotNull(organization.id), 7L))
        assertEquals(registration, instructorRegistrationRepository.findByOrganizationIdAndUserId(requireNotNull(organization.id), 42L))
        assertEquals(tour, tourRepository.findById(requireNotNull(tour.id)))
        assertEquals(listOf(tour), tourRepository.findByOrganizationId(requireNotNull(organization.id)))
        assertEquals(instructorProfile, instructorProfileRepository.findById(requireNotNull(instructorProfile.id)))
        assertEquals(instructorProfile, instructorProfileRepository.findByOrganizationIdAndUserId(requireNotNull(organization.id), 42L))
    }
}
