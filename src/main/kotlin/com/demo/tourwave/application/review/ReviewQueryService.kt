package com.demo.tourwave.application.review

import com.demo.tourwave.application.review.port.ReviewRepository
import com.demo.tourwave.domain.review.ReviewType

class ReviewQueryService(
    private val reviewRepository: ReviewRepository
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

