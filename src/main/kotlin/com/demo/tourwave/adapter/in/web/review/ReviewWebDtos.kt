package com.demo.tourwave.adapter.`in`.web.review

import com.demo.tourwave.domain.review.ReviewType
import java.time.Instant

data class ReviewCreateWebRequest(
    val rating: Int,
    val comment: String? = null
)

data class ReviewWebResponse(
    val id: Long,
    val occurrenceId: Long,
    val reviewerUserId: Long,
    val type: ReviewType,
    val rating: Int,
    val comment: String? = null,
    val createdAt: Instant
)

data class ReviewSummaryItemWebResponse(
    val count: Int,
    val averageRating: Double? = null
)

data class OccurrenceReviewSummaryWebResponse(
    val occurrenceId: Long,
    val tour: ReviewSummaryItemWebResponse,
    val instructor: ReviewSummaryItemWebResponse
)

