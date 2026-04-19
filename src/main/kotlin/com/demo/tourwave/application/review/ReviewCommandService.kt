package com.demo.tourwave.application.review

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.common.port.IdempotencyDecision
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.application.review.port.ReviewRepository
import com.demo.tourwave.domain.booking.AttendanceStatus
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.review.Review
import com.demo.tourwave.domain.review.ReviewType
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Clock

@Transactional
class ReviewCommandService(
    private val bookingRepository: BookingRepository,
    private val bookingParticipantRepository: BookingParticipantRepository,
    private val reviewRepository: ReviewRepository,
    private val idempotencyStore: IdempotencyStore,
    private val auditEventPort: AuditEventPort,
    private val clock: Clock,
) {
    fun createTourReview(command: CreateReviewCommand): CreateReviewResult {
        return createReview(command, ReviewType.TOUR, "/occurrences/{occurrenceId}/reviews/tour")
    }

    fun createInstructorReview(command: CreateReviewCommand): CreateReviewResult {
        return createReview(command, ReviewType.INSTRUCTOR, "/occurrences/{occurrenceId}/reviews/instructor")
    }

    private fun createReview(
        command: CreateReviewCommand,
        type: ReviewType,
        pathTemplate: String,
    ): CreateReviewResult {
        if (command.rating !in 1..5) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 422,
                message = "rating must be between 1 and 5",
                details = mapOf("field" to "rating"),
            )
        }

        val requestHash =
            hash(
                "${command.occurrenceId}|$type|${command.rating}|${command.comment.orEmpty()}",
            )

        return when (
            val decision =
                idempotencyStore.reserveOrReplay(
                    actorUserId = command.actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    requestHash = requestHash,
                )
        ) {
            is IdempotencyDecision.Replay ->
                CreateReviewResult(
                    status = decision.status,
                    review = decision.body as ReviewCreated,
                )

            IdempotencyDecision.Reserved -> {
                ensureAttendanceEligible(command)
                ensureNotDuplicated(command, type)

                val created =
                    reviewRepository.save(
                        Review(
                            occurrenceId = command.occurrenceId,
                            reviewerUserId = command.actorUserId,
                            type = type,
                            rating = command.rating,
                            comment = command.comment,
                            createdAt = clock.instant(),
                        ),
                    )

                val response =
                    ReviewCreated(
                        id = requireNotNull(created.id),
                        occurrenceId = created.occurrenceId,
                        reviewerUserId = created.reviewerUserId,
                        type = created.type,
                        rating = created.rating,
                        comment = created.comment,
                        createdAt = created.createdAt,
                    )

                idempotencyStore.complete(
                    actorUserId = command.actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    status = 201,
                    body = response,
                )

                auditEventPort.append(
                    AuditEventCommand(
                        actor = "USER:${command.actorUserId}",
                        action = "REVIEW_CREATED_${type.name}",
                        resourceType = "REVIEW",
                        resourceId = response.id,
                        occurredAtUtc = clock.instant(),
                        requestId = command.requestId,
                    ),
                )

                CreateReviewResult(status = 201, review = response)
            }
        }
    }

    private fun ensureAttendanceEligible(command: CreateReviewCommand) {
        val completedBookings =
            bookingRepository.findByOccurrenceAndStatuses(
                occurrenceId = command.occurrenceId,
                statuses = setOf(BookingStatus.COMPLETED),
            )
        val eligible =
            completedBookings.any { booking ->
                val bookingId = booking.id ?: return@any false
                bookingParticipantRepository.findByBookingIdAndUserId(bookingId, command.actorUserId)
                    ?.attendanceStatus == AttendanceStatus.ATTENDED
            }

        if (!eligible) {
            throw DomainException(
                errorCode = ErrorCode.ATTENDANCE_NOT_ELIGIBLE,
                status = 422,
                message = "Review can be created only by attended user",
                details =
                    mapOf(
                        "occurrenceId" to command.occurrenceId,
                        "actorUserId" to command.actorUserId,
                    ),
            )
        }
    }

    private fun ensureNotDuplicated(
        command: CreateReviewCommand,
        type: ReviewType,
    ) {
        val duplicated =
            reviewRepository.findByOccurrenceAndReviewerAndType(
                occurrenceId = command.occurrenceId,
                reviewerUserId = command.actorUserId,
                type = type,
            )
        if (duplicated != null) {
            throw DomainException(
                errorCode = ErrorCode.DUPLICATE_REVIEW,
                status = 409,
                message = "Review already exists for this occurrence and type",
                details =
                    mapOf(
                        "occurrenceId" to command.occurrenceId,
                        "actorUserId" to command.actorUserId,
                        "type" to type.name,
                    ),
            )
        }
    }

    private fun hash(raw: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
