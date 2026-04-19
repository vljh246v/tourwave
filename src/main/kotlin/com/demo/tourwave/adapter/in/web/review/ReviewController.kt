package com.demo.tourwave.adapter.`in`.web.review

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.review.CreateReviewCommand
import com.demo.tourwave.application.review.InstructorReviewSummary
import com.demo.tourwave.application.review.OccurrenceReviewSummary
import com.demo.tourwave.application.review.OrganizationReviewSummary
import com.demo.tourwave.application.review.ReviewCommandService
import com.demo.tourwave.application.review.ReviewCreated
import com.demo.tourwave.application.review.ReviewQueryService
import com.demo.tourwave.application.review.ReviewSummaryItem
import com.demo.tourwave.application.review.TourReviewSummary
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
    private val authzGuardPort: AuthzGuardPort,
) {
    @PostMapping("/occurrences/{occurrenceId}/reviews/tour")
    fun createTourReview(
        @PathVariable occurrenceId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
        @RequestBody request: ReviewCreateWebRequest,
    ): ResponseEntity<ReviewWebResponse> {
        val actorAuthContext = authzGuardPort.requireActorContext(actorUserId = actorUserId)
        val result =
            reviewCommandService.createTourReview(
                CreateReviewCommand(
                    occurrenceId = occurrenceId,
                    actorUserId = actorAuthContext.actorUserId,
                    idempotencyKey = idempotencyKey,
                    rating = request.rating,
                    comment = request.comment,
                    requestId = requestId,
                ),
            )
        return ResponseEntity.status(result.status).body(result.review.toWebResponse())
    }

    @PostMapping("/occurrences/{occurrenceId}/reviews/instructor")
    fun createInstructorReview(
        @PathVariable occurrenceId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
        @RequestBody request: ReviewCreateWebRequest,
    ): ResponseEntity<ReviewWebResponse> {
        val actorAuthContext = authzGuardPort.requireActorContext(actorUserId = actorUserId)
        val result =
            reviewCommandService.createInstructorReview(
                CreateReviewCommand(
                    occurrenceId = occurrenceId,
                    actorUserId = actorAuthContext.actorUserId,
                    idempotencyKey = idempotencyKey,
                    rating = request.rating,
                    comment = request.comment,
                    requestId = requestId,
                ),
            )
        return ResponseEntity.status(result.status).body(result.review.toWebResponse())
    }

    @GetMapping("/occurrences/{occurrenceId}/reviews/summary")
    fun getSummary(
        @PathVariable occurrenceId: Long,
    ): ResponseEntity<OccurrenceReviewSummaryWebResponse> {
        val summary = reviewQueryService.getOccurrenceSummary(occurrenceId)
        return ResponseEntity.ok(summary.toWebResponse())
    }

    @GetMapping("/tours/{tourId}/reviews/summary")
    fun getTourSummary(
        @PathVariable tourId: Long,
    ): ResponseEntity<TourReviewSummaryWebResponse> {
        return ResponseEntity.ok(reviewQueryService.getTourSummary(tourId).toWebResponse())
    }

    @GetMapping("/instructors/{instructorProfileId}/reviews/summary")
    fun getInstructorSummary(
        @PathVariable instructorProfileId: Long,
    ): ResponseEntity<InstructorReviewSummaryWebResponse> {
        return ResponseEntity.ok(reviewQueryService.getInstructorSummary(instructorProfileId).toWebResponse())
    }

    @GetMapping("/organizations/{organizationId}/reviews/summary")
    fun getPublicOrganizationSummary(
        @PathVariable organizationId: Long,
    ): ResponseEntity<OrganizationReviewSummaryWebResponse> {
        return ResponseEntity.ok(reviewQueryService.getPublicOrganizationSummary(organizationId).toWebResponse())
    }

    @GetMapping("/operator/organizations/{organizationId}/reviews/summary")
    fun getOperatorOrganizationSummary(
        @PathVariable organizationId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
    ): ResponseEntity<OrganizationReviewSummaryWebResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(
            reviewQueryService.getOperatorOrganizationSummary(requiredActorUserId, organizationId).toWebResponse(),
        )
    }

    private fun ReviewCreated.toWebResponse(): ReviewWebResponse {
        return ReviewWebResponse(
            id = id,
            occurrenceId = occurrenceId,
            reviewerUserId = reviewerUserId,
            type = type,
            rating = rating,
            comment = comment,
            createdAt = createdAt,
        )
    }

    private fun OccurrenceReviewSummary.toWebResponse(): OccurrenceReviewSummaryWebResponse {
        return OccurrenceReviewSummaryWebResponse(
            occurrenceId = occurrenceId,
            tour = tour.toWebResponse(),
            instructor = instructor.toWebResponse(),
        )
    }

    private fun ReviewSummaryItem.toWebResponse(): ReviewSummaryItemWebResponse {
        return ReviewSummaryItemWebResponse(
            count = count,
            averageRating = averageRating,
        )
    }

    private fun TourReviewSummary.toWebResponse(): TourReviewSummaryWebResponse {
        return TourReviewSummaryWebResponse(
            tourId = tourId,
            summary = summary.toWebResponse(),
            aggregationMode = aggregationMode.name,
        )
    }

    private fun InstructorReviewSummary.toWebResponse(): InstructorReviewSummaryWebResponse {
        return InstructorReviewSummaryWebResponse(
            instructorProfileId = instructorProfileId,
            summary = summary.toWebResponse(),
            aggregationMode = aggregationMode.name,
        )
    }

    private fun OrganizationReviewSummary.toWebResponse(): OrganizationReviewSummaryWebResponse {
        return OrganizationReviewSummaryWebResponse(
            organizationId = organizationId,
            scope = scope,
            tour = tour.toWebResponse(),
            instructor = instructor.toWebResponse(),
            aggregationMode = aggregationMode.name,
        )
    }
}
