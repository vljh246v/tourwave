package com.demo.tourwave.adapter.`in`.web.review

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.review.CreateReviewCommand
import com.demo.tourwave.application.review.OccurrenceReviewSummary
import com.demo.tourwave.application.review.ReviewCommandService
import com.demo.tourwave.application.review.ReviewCreated
import com.demo.tourwave.application.review.ReviewQueryService
import com.demo.tourwave.application.review.ReviewSummaryItem
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class ReviewController(
    private val reviewCommandService: ReviewCommandService,
    private val reviewQueryService: ReviewQueryService,
    private val authzGuardPort: AuthzGuardPort
) {
    @PostMapping("/occurrences/{occurrenceId}/reviews/tour")
    fun createTourReview(
        @PathVariable occurrenceId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
        @RequestBody request: ReviewCreateWebRequest
    ): ResponseEntity<ReviewWebResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val result = reviewCommandService.createTourReview(
            CreateReviewCommand(
                occurrenceId = occurrenceId,
                actorUserId = requiredActorUserId,
                idempotencyKey = idempotencyKey,
                rating = request.rating,
                comment = request.comment,
                requestId = requestId
            )
        )
        return ResponseEntity.status(result.status).body(result.review.toWebResponse())
    }

    @PostMapping("/occurrences/{occurrenceId}/reviews/instructor")
    fun createInstructorReview(
        @PathVariable occurrenceId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
        @RequestBody request: ReviewCreateWebRequest
    ): ResponseEntity<ReviewWebResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val result = reviewCommandService.createInstructorReview(
            CreateReviewCommand(
                occurrenceId = occurrenceId,
                actorUserId = requiredActorUserId,
                idempotencyKey = idempotencyKey,
                rating = request.rating,
                comment = request.comment,
                requestId = requestId
            )
        )
        return ResponseEntity.status(result.status).body(result.review.toWebResponse())
    }

    @GetMapping("/occurrences/{occurrenceId}/reviews/summary")
    fun getSummary(
        @PathVariable occurrenceId: Long
    ): ResponseEntity<OccurrenceReviewSummaryWebResponse> {
        val summary = reviewQueryService.getOccurrenceSummary(occurrenceId)
        return ResponseEntity.ok(summary.toWebResponse())
    }

    private fun ReviewCreated.toWebResponse(): ReviewWebResponse {
        return ReviewWebResponse(
            id = id,
            occurrenceId = occurrenceId,
            reviewerUserId = reviewerUserId,
            type = type,
            rating = rating,
            comment = comment,
            createdAt = createdAt
        )
    }

    private fun OccurrenceReviewSummary.toWebResponse(): OccurrenceReviewSummaryWebResponse {
        return OccurrenceReviewSummaryWebResponse(
            occurrenceId = occurrenceId,
            tour = tour.toWebResponse(),
            instructor = instructor.toWebResponse()
        )
    }

    private fun ReviewSummaryItem.toWebResponse(): ReviewSummaryItemWebResponse {
        return ReviewSummaryItemWebResponse(
            count = count,
            averageRating = averageRating
        )
    }
}
