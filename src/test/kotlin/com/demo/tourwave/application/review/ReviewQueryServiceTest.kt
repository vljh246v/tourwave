package com.demo.tourwave.application.review

import com.demo.tourwave.adapter.out.persistence.occurrence.InMemoryOccurrenceRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.review.InMemoryReviewRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.instructor.InMemoryInstructorProfileRepositoryAdapter
import com.demo.tourwave.domain.instructor.InstructorProfile
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.organization.Organization
import com.demo.tourwave.domain.organization.OrganizationMembership
import com.demo.tourwave.domain.organization.OrganizationRole
import com.demo.tourwave.domain.review.Review
import com.demo.tourwave.domain.review.ReviewType
import com.demo.tourwave.domain.tour.Tour
import com.demo.tourwave.support.FakeOrganizationMembershipRepository
import com.demo.tourwave.support.FakeOrganizationRepository
import com.demo.tourwave.support.FakeTourRepository
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class ReviewQueryServiceTest {
    private val reviewRepository = InMemoryReviewRepositoryAdapter()
    private val occurrenceRepository = InMemoryOccurrenceRepositoryAdapter()
    private val tourRepository = FakeTourRepository()
    private val instructorProfileRepository = InMemoryInstructorProfileRepositoryAdapter()
    private val organizationRepository = FakeOrganizationRepository()
    private val membershipRepository = FakeOrganizationMembershipRepository()
    private val service = ReviewQueryService(
        reviewRepository = reviewRepository,
        occurrenceRepository = occurrenceRepository,
        tourRepository = tourRepository,
        instructorProfileRepository = instructorProfileRepository,
        organizationAccessGuard = OrganizationAccessGuard(organizationRepository, membershipRepository)
    )

    @BeforeEach
    fun setUp() {
        reviewRepository.clear()
        occurrenceRepository.clear()
        tourRepository.clear()
        instructorProfileRepository.clear()
        organizationRepository.clear()
        membershipRepository.clear()
    }

    @Test
    fun `tour and instructor summaries aggregate only public trust surface`() {
        val publishedTour = tourRepository.save(
            Tour.create(organizationId = 11L, title = "Published", summary = null, now = now())
                .publish(now())
                .copy(id = 101L)
        )
        tourRepository.save(
            Tour.create(organizationId = 11L, title = "Draft", summary = null, now = now())
                .copy(id = 102L)
        )
        instructorProfileRepository.save(
            InstructorProfile.create(
                userId = 501L,
                organizationId = 11L,
                headline = "Guide",
                bio = null,
                languages = listOf("ko"),
                specialties = listOf("history"),
                certifications = emptyList(),
                yearsOfExperience = 3,
                internalNote = null,
                approvedAt = now(),
                now = now()
            ).copy(id = 301L)
        )
        occurrenceRepository.save(
            Occurrence(
                id = 1001L,
                organizationId = 11L,
                tourId = requireNotNull(publishedTour.id),
                instructorProfileId = 301L,
                capacity = 10,
                startsAtUtc = now(),
                endsAtUtc = now().plusSeconds(7200),
                createdAt = now(),
                updatedAt = now()
            )
        )
        occurrenceRepository.save(
            Occurrence(
                id = 1002L,
                organizationId = 11L,
                tourId = 102L,
                instructorProfileId = 301L,
                capacity = 10,
                startsAtUtc = now(),
                endsAtUtc = now().plusSeconds(7200),
                createdAt = now(),
                updatedAt = now()
            )
        )
        reviewRepository.save(review(1001L, 201L, ReviewType.TOUR, 5))
        reviewRepository.save(review(1001L, 202L, ReviewType.TOUR, 3))
        reviewRepository.save(review(1001L, 203L, ReviewType.INSTRUCTOR, 4))
        reviewRepository.save(review(1002L, 204L, ReviewType.INSTRUCTOR, 1))

        val tourSummary = service.getTourSummary(101L)
        val instructorSummary = service.getInstructorSummary(301L)

        assertEquals(2, tourSummary.summary.count)
        assertEquals(4.0, tourSummary.summary.averageRating)
        assertEquals(1, instructorSummary.summary.count)
        assertEquals(4.0, instructorSummary.summary.averageRating)
    }

    @Test
    fun `organization public summary excludes draft tours while operator summary includes all occurrences`() {
        organizationRepository.save(
            Organization.create(
                slug = "review-org",
                name = "Review Org",
                description = null,
                publicDescription = null,
                contactEmail = null,
                contactPhone = null,
                websiteUrl = null,
                businessName = null,
                businessRegistrationNumber = null,
                timezone = "Asia/Seoul",
                now = now()
            ).copy(id = 11L)
        )
        membershipRepository.save(
            OrganizationMembership.active(
                organizationId = 11L,
                userId = 900L,
                role = OrganizationRole.ADMIN,
                now = now()
            )
        )
        tourRepository.save(
            Tour.create(organizationId = 11L, title = "Published", summary = null, now = now())
                .publish(now())
                .copy(id = 101L)
        )
        tourRepository.save(
            Tour.create(organizationId = 11L, title = "Draft", summary = null, now = now())
                .copy(id = 102L)
        )
        occurrenceRepository.save(
            Occurrence(
                id = 1001L,
                organizationId = 11L,
                tourId = 101L,
                instructorProfileId = 301L,
                capacity = 10,
                startsAtUtc = now(),
                endsAtUtc = now().plusSeconds(7200),
                createdAt = now(),
                updatedAt = now()
            )
        )
        occurrenceRepository.save(
            Occurrence(
                id = 1002L,
                organizationId = 11L,
                tourId = 102L,
                instructorProfileId = 301L,
                capacity = 10,
                startsAtUtc = now(),
                endsAtUtc = now().plusSeconds(7200),
                createdAt = now(),
                updatedAt = now()
            )
        )
        reviewRepository.save(review(1001L, 201L, ReviewType.TOUR, 5))
        reviewRepository.save(review(1001L, 202L, ReviewType.INSTRUCTOR, 4))
        reviewRepository.save(review(1002L, 203L, ReviewType.TOUR, 1))
        reviewRepository.save(review(1002L, 204L, ReviewType.INSTRUCTOR, 2))

        val publicSummary = service.getPublicOrganizationSummary(11L)
        val operatorSummary = service.getOperatorOrganizationSummary(actorUserId = 900L, organizationId = 11L)

        assertEquals("PUBLIC", publicSummary.scope)
        assertEquals(1, publicSummary.tour.count)
        assertEquals(5.0, publicSummary.tour.averageRating)
        assertEquals(1, publicSummary.instructor.count)
        assertEquals("OPERATOR", operatorSummary.scope)
        assertEquals(2, operatorSummary.tour.count)
        assertEquals(3.0, operatorSummary.tour.averageRating)
        assertEquals(2, operatorSummary.instructor.count)
        assertEquals(3.0, operatorSummary.instructor.averageRating)
    }

    @Test
    fun `operator organization summary requires operator membership`() {
        organizationRepository.save(
            Organization.create(
                slug = "review-org",
                name = "Review Org",
                description = null,
                publicDescription = null,
                contactEmail = null,
                contactPhone = null,
                websiteUrl = null,
                businessName = null,
                businessRegistrationNumber = null,
                timezone = "Asia/Seoul",
                now = now()
            ).copy(id = 11L)
        )

        assertFailsWith<Exception> {
            service.getOperatorOrganizationSummary(actorUserId = 999L, organizationId = 11L)
        }
    }

    private fun review(occurrenceId: Long, reviewerUserId: Long, type: ReviewType, rating: Int): Review =
        Review(
            occurrenceId = occurrenceId,
            reviewerUserId = reviewerUserId,
            type = type,
            rating = rating,
            createdAt = now()
        )

    private fun now(): Instant = Instant.parse("2026-03-19T00:00:00Z")
}
