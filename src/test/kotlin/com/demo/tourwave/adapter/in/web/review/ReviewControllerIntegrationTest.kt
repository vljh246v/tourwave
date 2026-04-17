package com.demo.tourwave.adapter.`in`.web.review

import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.instructor.port.InstructorProfileRepository
import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.review.port.ReviewRepository
import com.demo.tourwave.application.tour.port.TourRepository
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.instructor.InstructorProfile
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.organization.Organization
import com.demo.tourwave.domain.organization.OrganizationMembership
import com.demo.tourwave.domain.organization.OrganizationRole
import com.demo.tourwave.domain.review.Review
import com.demo.tourwave.domain.review.ReviewType
import com.demo.tourwave.domain.tour.Tour
import com.demo.tourwave.domain.user.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
class ReviewControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var membershipRepository: OrganizationMembershipRepository

    @Autowired
    private lateinit var tourRepository: TourRepository

    @Autowired
    private lateinit var occurrenceRepository: OccurrenceRepository

    @Autowired
    private lateinit var instructorProfileRepository: InstructorProfileRepository

    @Autowired
    private lateinit var reviewRepository: ReviewRepository

    @BeforeEach
    fun setUp() {
        reviewRepository.clear()
        occurrenceRepository.clear()
        tourRepository.clear()
        instructorProfileRepository.clear()
        membershipRepository.clear()
        organizationRepository.clear()
        userRepository.clear()
    }

    @Test
    fun `public and operator review summary endpoints expose expected trust scopes`() {
        val admin = userRepository.save(user(900L, "admin@test.com"))
        val member = userRepository.save(user(901L, "member@test.com"))
        val instructorUser = userRepository.save(user(902L, "instructor@test.com"))

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
                now = now(),
            ).copy(id = 11L),
        )
        membershipRepository.save(OrganizationMembership.active(11L, requireNotNull(admin.id), OrganizationRole.ADMIN, now()))
        membershipRepository.save(OrganizationMembership.active(11L, requireNotNull(member.id), OrganizationRole.MEMBER, now()))
        instructorProfileRepository.save(
            InstructorProfile.create(
                userId = requireNotNull(instructorUser.id),
                organizationId = 11L,
                headline = "Guide",
                bio = null,
                languages = listOf("ko"),
                specialties = listOf("history"),
                certifications = emptyList(),
                yearsOfExperience = 2,
                internalNote = null,
                approvedAt = now(),
                now = now(),
            ).copy(id = 301L),
        )
        tourRepository.save(
            Tour.create(organizationId = 11L, title = "Published Tour", summary = null, now = now())
                .publish(now())
                .copy(id = 101L),
        )
        tourRepository.save(
            Tour.create(organizationId = 11L, title = "Draft Tour", summary = null, now = now())
                .copy(id = 102L),
        )
        occurrenceRepository.save(occurrence(1001L, 11L, 101L, 301L))
        occurrenceRepository.save(occurrence(1002L, 11L, 102L, 301L))
        reviewRepository.save(review(1001L, 201L, ReviewType.TOUR, 5))
        reviewRepository.save(review(1001L, 202L, ReviewType.INSTRUCTOR, 4))
        reviewRepository.save(review(1002L, 203L, ReviewType.TOUR, 1))
        reviewRepository.save(review(1002L, 204L, ReviewType.INSTRUCTOR, 2))

        mockMvc.perform(get("/tours/101/reviews/summary"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tourId").value(101))
            .andExpect(jsonPath("$.summary.count").value(1))
            .andExpect(jsonPath("$.summary.averageRating").value(5.0))

        mockMvc.perform(get("/instructors/301/reviews/summary"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.instructorProfileId").value(301))
            .andExpect(jsonPath("$.summary.count").value(1))
            .andExpect(jsonPath("$.summary.averageRating").value(4.0))

        mockMvc.perform(get("/organizations/11/reviews/summary"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.scope").value("PUBLIC"))
            .andExpect(jsonPath("$.tour.count").value(1))
            .andExpect(jsonPath("$.instructor.count").value(1))

        mockMvc.perform(
            get("/operator/organizations/11/reviews/summary")
                .header("X-Actor-User-Id", requireNotNull(admin.id)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.scope").value("OPERATOR"))
            .andExpect(jsonPath("$.tour.count").value(2))
            .andExpect(jsonPath("$.instructor.count").value(2))

        mockMvc.perform(
            get("/operator/organizations/11/reviews/summary")
                .header("X-Actor-User-Id", requireNotNull(member.id)),
        )
            .andExpect(status().isForbidden)
    }

    private fun user(
        id: Long,
        email: String,
    ): User =
        User.create(
            displayName = email.substringBefore("@"),
            email = email,
            passwordHash = "hash",
            now = now(),
        ).copy(id = id)

    private fun occurrence(
        id: Long,
        organizationId: Long,
        tourId: Long,
        instructorProfileId: Long,
    ): Occurrence =
        Occurrence(
            id = id,
            organizationId = organizationId,
            tourId = tourId,
            instructorProfileId = instructorProfileId,
            capacity = 12,
            startsAtUtc = now(),
            endsAtUtc = now().plusSeconds(7200),
            timezone = "Asia/Seoul",
            createdAt = now(),
            updatedAt = now(),
        )

    private fun review(
        occurrenceId: Long,
        reviewerUserId: Long,
        type: ReviewType,
        rating: Int,
    ): Review =
        Review(
            occurrenceId = occurrenceId,
            reviewerUserId = reviewerUserId,
            type = type,
            rating = rating,
            createdAt = now(),
        )

    private fun now(): Instant = Instant.parse("2026-03-19T00:00:00Z")
}
