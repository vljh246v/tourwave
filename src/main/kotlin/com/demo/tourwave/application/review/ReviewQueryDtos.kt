package com.demo.tourwave.application.review

data class ReviewSummaryItem(
    val count: Int,
    val averageRating: Double? = null,
)

data class OccurrenceReviewSummary(
    val occurrenceId: Long,
    val tour: ReviewSummaryItem,
    val instructor: ReviewSummaryItem,
)

enum class ReviewAggregationMode {
    QUERY_TIME,
}

data class TourReviewSummary(
    val tourId: Long,
    val summary: ReviewSummaryItem,
    val aggregationMode: ReviewAggregationMode = ReviewAggregationMode.QUERY_TIME,
)

data class InstructorReviewSummary(
    val instructorProfileId: Long,
    val summary: ReviewSummaryItem,
    val aggregationMode: ReviewAggregationMode = ReviewAggregationMode.QUERY_TIME,
)

data class OrganizationReviewSummary(
    val organizationId: Long,
    val scope: String,
    val tour: ReviewSummaryItem,
    val instructor: ReviewSummaryItem,
    val aggregationMode: ReviewAggregationMode = ReviewAggregationMode.QUERY_TIME,
)
