package com.demo.tourwave.application.review.port

import com.demo.tourwave.domain.review.Review
import com.demo.tourwave.domain.review.ReviewType

interface ReviewRepository {
    fun save(review: Review): Review
    fun findByOccurrenceAndReviewerAndType(occurrenceId: Long, reviewerUserId: Long, type: ReviewType): Review?
    fun findByOccurrenceAndType(occurrenceId: Long, type: ReviewType): List<Review>
    fun findAll(): List<Review>
    fun clear()
}
