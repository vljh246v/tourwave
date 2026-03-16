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
import com.demo.tourwave.domain.inquiry.InquiryMessage
import com.demo.tourwave.domain.inquiry.InquiryStatus
import java.security.MessageDigest
import java.time.Clock

class InquiryCommandService(
    private val bookingRepository: BookingRepository,
    private val inquiryRepository: InquiryRepository,
    private val inquiryAccessPolicy: InquiryAccessPolicy,
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
        val messageBody = command.message.trim()
        if (messageBody.isEmpty()) {
            throw DomainException(
                errorCode = ErrorCode.REQUIRED_FIELD_MISSING,
                status = 422,
                message = "message is required",
                details = mapOf("field" to "message")
            )
        }

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

                if (booking.leaderUserId != command.actorUserId) {
                    throw DomainException(
                        errorCode = ErrorCode.FORBIDDEN,
                        status = 403,
                        message = "Only booking leader can create inquiry",
                        details = mapOf("bookingId" to bookingId, "actorUserId" to command.actorUserId)
                    )
                }

                val now = clock.instant()

                val existing = inquiryRepository.findByBookingId(bookingId)
                val saved = if (existing != null) {
                    existing
                } else {
                    val created = inquiryRepository.save(
                        Inquiry(
                            organizationId = booking.organizationId,
                            occurrenceId = command.occurrenceId,
                            bookingId = bookingId,
                            createdByUserId = command.actorUserId,
                            subject = command.subject,
                            createdAt = now
                        )
                    )
                    inquiryRepository.saveMessage(
                        InquiryMessage(
                            inquiryId = requireNotNull(created.id),
                            senderUserId = command.actorUserId,
                            body = messageBody,
                            createdAt = now
                        )
                    )
                    created
                }

                val response = InquiryCreated(
                    id = requireNotNull(saved.id),
                    organizationId = saved.organizationId,
                    occurrenceId = saved.occurrenceId,
                    bookingId = saved.bookingId,
                    createdByUserId = saved.createdByUserId,
                    subject = saved.subject,
                    status = saved.status,
                    createdAt = saved.createdAt
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

    fun postMessage(command: PostInquiryMessageCommand): PostInquiryMessageResult {
        val inquiry = loadInquiry(command.inquiryId)
        val accessType = inquiryAccessPolicy.authorize(inquiry, command.actor)

        val body = command.body?.trim()
        if (body.isNullOrEmpty()) {
            throw DomainException(
                errorCode = ErrorCode.REQUIRED_FIELD_MISSING,
                status = 422,
                message = "body is required",
                details = mapOf("field" to "body")
            )
        }

        val pathTemplate = "/inquiries/{inquiryId}/messages"
        val requestHash = hashForMessage(command.inquiryId, body, command.attachmentAssetIds)

        return when (
            val decision = idempotencyStore.reserveOrReplay(
                actorUserId = command.actor.actorUserId,
                method = "POST",
                pathTemplate = pathTemplate,
                idempotencyKey = command.idempotencyKey,
                requestHash = requestHash
            )
        ) {
            is IdempotencyDecision.Replay -> PostInquiryMessageResult(
                status = decision.status,
                message = decision.body as InquiryMessageView
            )

            IdempotencyDecision.Reserved -> {
                if (inquiry.status == InquiryStatus.CLOSED) {
                    throw DomainException(
                        errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                        status = 409,
                        message = "Inquiry is already closed",
                        details = mapOf("inquiryId" to command.inquiryId, "status" to inquiry.status)
                    )
                }

                val saved = inquiryRepository.saveMessage(
                    InquiryMessage(
                        inquiryId = command.inquiryId,
                        senderUserId = command.actor.actorUserId,
                        body = body,
                        attachmentAssetIds = command.attachmentAssetIds,
                        createdAt = clock.instant()
                    )
                )

                val response = InquiryMessageView(
                    id = requireNotNull(saved.id),
                    inquiryId = saved.inquiryId,
                    senderUserId = saved.senderUserId,
                    body = saved.body,
                    attachmentAssetIds = saved.attachmentAssetIds,
                    createdAt = saved.createdAt
                )

                idempotencyStore.complete(
                    actorUserId = command.actor.actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    status = 201,
                    body = response
                )

                auditEventPort.append(
                    AuditEventCommand(
                        actor = actorLabel(command.actor, accessType),
                        action = "INQUIRY_MESSAGE_POSTED",
                        resourceType = "INQUIRY_MESSAGE",
                        resourceId = requireNotNull(saved.id),
                        occurredAtUtc = clock.instant(),
                        requestId = command.actor.requestId
                    )
                )

                PostInquiryMessageResult(status = 201, message = response)
            }
        }
    }

    fun closeInquiry(command: CloseInquiryCommand): CloseInquiryResult {
        val inquiry = loadInquiry(command.inquiryId)
        val accessType = inquiryAccessPolicy.authorize(inquiry, command.actor)

        val pathTemplate = "/inquiries/{inquiryId}/close"
        val requestHash = hash("${command.inquiryId}|CLOSE")

        return when (
            val decision = idempotencyStore.reserveOrReplay(
                actorUserId = command.actor.actorUserId,
                method = "POST",
                pathTemplate = pathTemplate,
                idempotencyKey = command.idempotencyKey,
                requestHash = requestHash
            )
        ) {
            is IdempotencyDecision.Replay -> CloseInquiryResult(status = decision.status)

            IdempotencyDecision.Reserved -> {
                if (inquiry.status == InquiryStatus.CLOSED) {
                    throw DomainException(
                        errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                        status = 409,
                        message = "Inquiry is already closed",
                        details = mapOf("inquiryId" to command.inquiryId, "status" to inquiry.status)
                    )
                }

                val closed = inquiry.close()
                inquiryRepository.save(closed)

                idempotencyStore.complete(
                    actorUserId = command.actor.actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    status = 204,
                    body = mapOf("ok" to true)
                )

                auditEventPort.append(
                    AuditEventCommand(
                        actor = actorLabel(command.actor, accessType),
                        action = "INQUIRY_CLOSED",
                        resourceType = "INQUIRY",
                        resourceId = command.inquiryId,
                        occurredAtUtc = clock.instant(),
                        requestId = command.actor.requestId
                    )
                )

                CloseInquiryResult(status = 204)
            }
        }
    }

    private fun hashForCreate(command: CreateInquiryCommand, bookingId: Long): String {
        val raw = "${command.occurrenceId}|$bookingId|${command.subject ?: ""}|${command.message}"
        return hash(raw)
    }

    private fun hashForMessage(inquiryId: Long, body: String, attachmentAssetIds: List<Long>?): String {
        val attachments = attachmentAssetIds?.joinToString(",") ?: ""
        return hash("$inquiryId|$body|$attachments")
    }

    private fun hash(raw: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun loadInquiry(inquiryId: Long): Inquiry {
        return inquiryRepository.findById(inquiryId)
            ?: throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 404,
                message = "Inquiry not found",
                details = mapOf("inquiryId" to inquiryId)
            )
    }

    private fun actorLabel(actor: InquiryActorContext, accessType: InquiryAccessType): String {
        return if (accessType == InquiryAccessType.ORG_OPERATOR) {
            "OPERATOR:${actor.actorUserId}"
        } else {
            "USER:${actor.actorUserId}"
        }
    }
}
