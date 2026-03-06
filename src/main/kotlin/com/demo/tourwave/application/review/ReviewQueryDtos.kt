package com.demo.tourwave.application.review

data class ReviewSummaryItem(
    val count: Int,
    val averageRating: Double? = null
)

data class OccurrenceReviewSummary(
    val occurrenceId: Long,
    val tour: ReviewSummaryItem,
    val instructor: ReviewSummaryItem
)

