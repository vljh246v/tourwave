package com.demo.tourwave.adapter.out.persistence.topology

import com.demo.tourwave.domain.instructor.InstructorProfile
import com.demo.tourwave.domain.organization.OrganizationMembership
import com.demo.tourwave.domain.organization.OrganizationRole
import com.demo.tourwave.domain.organization.Organization
import com.demo.tourwave.domain.tour.Tour
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

    @BeforeEach
    fun setUp() {
        organizationRepository.clear()
        membershipRepository.clear()
        tourRepository.clear()
        instructorProfileRepository.clear()
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
        val tour = tourRepository.save(Tour(organizationId = requireNotNull(organization.id), title = "Night Walk"))
        val instructorProfile = instructorProfileRepository.save(
            InstructorProfile(
                userId = 42L,
                organizationId = requireNotNull(organization.id),
                bio = "Guide"
            )
        )

        assertNotNull(organization.id)
        assertNotNull(membership.id)
        assertNotNull(tour.id)
        assertNotNull(instructorProfile.id)
        assertEquals(organization, organizationRepository.findById(requireNotNull(organization.id)))
        assertEquals(organization, organizationRepository.findBySlug("tourwave-org"))
        assertEquals(membership, membershipRepository.findByOrganizationIdAndUserId(requireNotNull(organization.id), 7L))
        assertEquals(tour, tourRepository.findById(requireNotNull(tour.id)))
        assertEquals(instructorProfile, instructorProfileRepository.findById(requireNotNull(instructorProfile.id)))
    }
}
