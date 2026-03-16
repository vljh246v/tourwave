package com.demo.tourwave.adapter.out.persistence.topology

import com.demo.tourwave.domain.instructor.InstructorProfile
import com.demo.tourwave.domain.organization.Organization
import com.demo.tourwave.domain.tour.Tour
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InMemoryTopologyRepositoryAdaptersTest {
    private val organizationRepository = InMemoryOrganizationRepositoryAdapter()
    private val tourRepository = InMemoryTourRepositoryAdapter()
    private val instructorProfileRepository = InMemoryInstructorProfileRepositoryAdapter()

    @BeforeEach
    fun setUp() {
        organizationRepository.clear()
        tourRepository.clear()
        instructorProfileRepository.clear()
    }

    @Test
    fun `repositories persist and load topology entities by id`() {
        val organization = organizationRepository.save(Organization(name = "Tourwave Org"))
        val tour = tourRepository.save(Tour(organizationId = requireNotNull(organization.id), title = "Night Walk"))
        val instructorProfile = instructorProfileRepository.save(
            InstructorProfile(
                userId = 42L,
                organizationId = requireNotNull(organization.id),
                bio = "Guide"
            )
        )

        assertNotNull(organization.id)
        assertNotNull(tour.id)
        assertNotNull(instructorProfile.id)
        assertEquals(organization, organizationRepository.findById(requireNotNull(organization.id)))
        assertEquals(tour, tourRepository.findById(requireNotNull(tour.id)))
        assertEquals(instructorProfile, instructorProfileRepository.findById(requireNotNull(instructorProfile.id)))
    }
}
