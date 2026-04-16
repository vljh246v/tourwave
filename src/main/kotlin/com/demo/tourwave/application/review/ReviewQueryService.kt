package com.demo.tourwave.application.review

import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.review.port.ReviewRepository
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.instructor.port.InstructorProfileRepository
import com.demo.tourwave.application.tour.port.TourRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.instructor.InstructorProfileStatus
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.review.ReviewType
import com.demo.tourwave.domain.tour.TourStatus

class ReviewQueryService(
    private val reviewRepository: ReviewRepository,
    private val occurrenceRepository: OccurrenceRepository,
    private val tourRepository: TourRepository,
    private val instructorProfileRepository: InstructorProfileRepository,
    private val organizationAccessGuard: OrganizationAccessGuard
) {
    fun getOccurrenceSummary(occurrenceId: Long): OccurrenceReviewSummary {
        val tourReviews = reviewRepository.findByOccurrenceAndType(
            occurrenceId = occurrenceId,
            type = ReviewType.TOUR
        )
        val instructorReviews = reviewRepository.findByOccurrenceAndType(
            occurrenceId = occurrenceId,
            type = ReviewType.INSTRUCTOR
        )

        return OccurrenceReviewSummary(
            occurrenceId = occurrenceId,
            tour = tourReviews.toSummaryItem(),
            instructor = instructorReviews.toSummaryItem()
        )
    }

    fun getTourSummary(tourId: Long): TourReviewSummary {
        val tour = tourRepository.findById(tourId) ?: throw notFound("tour $tourId not found")
        if (tour.status != TourStatus.PUBLISHED) {
            throw notFound("tour $tourId not found")
        }
        val occurrenceIds = occurrenceRepository.findByTourId(tourId).map { it.id }.toSet()
        return TourReviewSummary(
            tourId = tourId,
            summary = summarize(
                reviews = reviewRepository.findAll(),
                occurrenceIds = occurrenceIds,
                type = ReviewType.TOUR
            )
        )
    }

    fun getInstructorSummary(instructorProfileId: Long): InstructorReviewSummary {
        val instructorProfile = instructorProfileRepository.findById(instructorProfileId)
            ?: throw notFound("instructor profile $instructorProfileId not found")
        if (instructorProfile.status != InstructorProfileStatus.ACTIVE) {
            throw notFound("instructor profile $instructorProfileId not found")
        }

        val publishedTourIds = tourRepository.findAllPublished().mapNotNull { it.id }.toSet()
        val occurrenceIds = occurrenceRepository.findAll()
            .filter { it.instructorProfileId == instructorProfileId && it.tourId in publishedTourIds }
            .map { it.id }
            .toSet()

        return InstructorReviewSummary(
            instructorProfileId = instructorProfileId,
            summary = summarize(
                reviews = reviewRepository.findAll(),
                occurrenceIds = occurrenceIds,
                type = ReviewType.INSTRUCTOR
            )
        )
    }

    fun getPublicOrganizationSummary(organizationId: Long): OrganizationReviewSummary {
        organizationAccessGuard.requireOrganization(organizationId)
        val publishedTourIds = tourRepository.findAllPublished()
            .filter { it.organizationId == organizationId }
            .mapNotNull { it.id }
            .toSet()
        val occurrences = occurrenceRepository.findAll()
            .filter { it.organizationId == organizationId && it.tourId in publishedTourIds }
        return toOrganizationSummary(
            organizationId = organizationId,
            scope = "PUBLIC",
            occurrences = occurrences
        )
    }

    fun getOperatorOrganizationSummary(actorUserId: Long, organizationId: Long): OrganizationReviewSummary {
        organizationAccessGuard.requireOperator(actorUserId, organizationId)
        val occurrences = occurrenceRepository.findAll().filter { it.organizationId == organizationId }
        return toOrganizationSummary(
            organizationId = organizationId,
            scope = "OPERATOR",
            occurrences = occurrences
        )
    }

    private fun toOrganizationSummary(
        organizationId: Long,
        scope: String,
        occurrences: List<Occurrence>
    ): OrganizationReviewSummary {
        val occurrenceIds = occurrences.map { it.id }.toSet()
        val reviews = reviewRepository.findAll()
        return OrganizationReviewSummary(
            organizationId = organizationId,
            scope = scope,
            tour = summarize(reviews, occurrenceIds, ReviewType.TOUR),
            instructor = summarize(reviews, occurrenceIds, ReviewType.INSTRUCTOR)
        )
    }

    private fun summarize(
        reviews: List<com.demo.tourwave.domain.review.Review>,
        occurrenceIds: Set<Long>,
        type: ReviewType
    ): ReviewSummaryItem {
        return reviews
            .asSequence()
            .filter { it.type == type && it.occurrenceId in occurrenceIds }
            .toList()
            .toSummaryItem()
    }

    private fun notFound(message: String) = DomainException(
        errorCode = ErrorCode.VALIDATION_ERROR,
        status = 404,
        message = message
    )

    private fun List<com.demo.tourwave.domain.review.Review>.toSummaryItem(): ReviewSummaryItem {
        if (isEmpty()) {
            return ReviewSummaryItem(count = 0, averageRating = null)
        }
        return ReviewSummaryItem(
            count = size,
            averageRating = map { it.rating }.average()
        )
    }
}
