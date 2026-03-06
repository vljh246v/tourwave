package com.demo.tourwave.application.review

import com.demo.tourwave.domain.review.ReviewType
import java.time.Instant

data class CreateReviewCommand(
    val occurrenceId: Long,
    val actorUserId: Long,
    val idempotencyKey: String,
    val rating: Int,
    val comment: String? = null,
    val requestId: String? = null
)

data class ReviewCreated(
    val id: Long,
    val occurrenceId: Long,
    val reviewerUserId: Long,
    val type: ReviewType,
    val rating: Int,
    val comment: String? = null,
    val createdAt: Instant
)

data class CreateReviewResult(
    val status: Int,
    val review: ReviewCreated
)

