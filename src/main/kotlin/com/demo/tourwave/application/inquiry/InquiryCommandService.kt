package com.demo.tourwave.application.inquiry

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.common.port.IdempotencyDecision
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.application.inquiry.port.InquiryRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.inquiry.Inquiry
import java.security.MessageDigest
import java.time.Clock

class InquiryCommandService(
    private val bookingRepository: BookingRepository,
    private val inquiryRepository: InquiryRepository,
    private val idempotencyStore: IdempotencyStore,
    private val auditEventPort: AuditEventPort,
    private val clock: Clock
) {
    fun createInquiry(command: CreateInquiryCommand): CreateInquiryResult {
        val bookingId = command.bookingId ?: throw DomainException(
            errorCode = ErrorCode.REQUIRED_FIELD_MISSING,
            status = 422,
            message = "bookingId is required",
            details = mapOf("field" to "bookingId")
        )

        val pathTemplate = "/occurrences/{occurrenceId}/inquiries"
        val requestHash = hashForCreate(command, bookingId)

        return when (
            val decision = idempotencyStore.reserveOrReplay(
                actorUserId = command.actorUserId,
                method = "POST",
                pathTemplate = pathTemplate,
                idempotencyKey = command.idempotencyKey,
                requestHash = requestHash
            )
        ) {
            is IdempotencyDecision.Replay -> CreateInquiryResult(
                status = decision.status,
                inquiry = decision.body as InquiryCreated
            )

            IdempotencyDecision.Reserved -> {
                val booking = bookingRepository.findById(bookingId)
                    ?: throw DomainException(
                        errorCode = ErrorCode.BOOKING_SCOPE_MISMATCH,
                        status = 422,
                        message = "bookingId does not match occurrence or organization scope",
                        details = mapOf("occurrenceId" to command.occurrenceId, "bookingId" to bookingId)
                    )

                if (booking.occurrenceId != command.occurrenceId) {
                    throw DomainException(
                        errorCode = ErrorCode.BOOKING_SCOPE_MISMATCH,
                        status = 422,
                        message = "bookingId does not match occurrence or organization scope",
                        details = mapOf("occurrenceId" to command.occurrenceId, "bookingId" to bookingId)
                    )
                }

                val existing = inquiryRepository.findByBookingId(bookingId)
                val saved = if (existing != null) {
                    existing
                } else {
                    inquiryRepository.save(
                        Inquiry(
                            organizationId = booking.organizationId,
                            occurrenceId = command.occurrenceId,
                            bookingId = bookingId,
                            createdByUserId = command.actorUserId,
                            subject = command.subject
                        )
                    )
                }

                val response = InquiryCreated(
                    id = requireNotNull(saved.id),
                    organizationId = saved.organizationId,
                    occurrenceId = saved.occurrenceId,
                    bookingId = saved.bookingId,
                    createdByUserId = saved.createdByUserId,
                    subject = saved.subject,
                    status = saved.status,
                    createdAt = clock.instant()
                )

                idempotencyStore.complete(
                    actorUserId = command.actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    status = 201,
                    body = response
                )

                auditEventPort.append(
                    AuditEventCommand(
                        actor = "USER:${command.actorUserId}",
                        action = "INQUIRY_CREATED",
                        resourceType = "INQUIRY",
                        resourceId = requireNotNull(saved.id),
                        occurredAtUtc = clock.instant(),
                        requestId = command.requestId
                    )
                )

                CreateInquiryResult(status = 201, inquiry = response)
            }
        }
    }

    private fun hashForCreate(command: CreateInquiryCommand, bookingId: Long): String {
        val raw = "${command.occurrenceId}|$bookingId|${command.subject ?: ""}|${command.message}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

