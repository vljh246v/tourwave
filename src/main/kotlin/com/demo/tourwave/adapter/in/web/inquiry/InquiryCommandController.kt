package com.demo.tourwave.adapter.`in`.web.inquiry

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.inquiry.CloseInquiryCommand
import com.demo.tourwave.application.inquiry.CreateInquiryCommand
import com.demo.tourwave.application.inquiry.InquiryCommandService
import com.demo.tourwave.application.inquiry.InquiryCreated
import com.demo.tourwave.application.inquiry.InquiryMessageView
import com.demo.tourwave.application.inquiry.PostInquiryMessageCommand
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class InquiryCommandController(
    private val inquiryCommandService: InquiryCommandService,
    private val authzGuardPort: AuthzGuardPort,
) {
    @PostMapping("/occurrences/{occurrenceId}/inquiries")
    fun createInquiry(
        @PathVariable occurrenceId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
        @RequestBody request: InquiryCreateWebRequest,
    ): ResponseEntity<InquiryWebResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val result =
            inquiryCommandService.createInquiry(
                CreateInquiryCommand(
                    occurrenceId = occurrenceId,
                    actorUserId = requiredActorUserId,
                    idempotencyKey = idempotencyKey,
                    bookingId = request.bookingId,
                    subject = request.subject,
                    message = request.message,
                    requestId = requestId,
                ),
            )

        return ResponseEntity.status(result.status).body(result.inquiry.toWebResponse())
    }

    @PostMapping("/inquiries/{inquiryId}/messages")
    fun postInquiryMessage(
        @PathVariable inquiryId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Actor-Role", required = false) actorRole: String?,
        @RequestHeader("X-Actor-Org-Role", required = false) actorOrgRole: String?,
        @RequestHeader("X-Actor-Org-Id", required = false) actorOrgId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
        @RequestBody request: InquiryMessageCreateWebRequest,
    ): ResponseEntity<InquiryMessageWebResponse> {
        val actorAuthContext =
            authzGuardPort.requireActorContext(
                actorUserId = actorUserId,
                actorRole = actorRole,
                actorOrgRole = actorOrgRole,
                actorOrgId = actorOrgId,
                requestId = requestId,
            )
        val result =
            inquiryCommandService.postMessage(
                PostInquiryMessageCommand(
                    inquiryId = inquiryId,
                    actor = actorAuthContext,
                    idempotencyKey = idempotencyKey,
                    body = request.body,
                    attachmentAssetIds = request.attachmentAssetIds,
                ),
            )

        return ResponseEntity.status(result.status).body(result.message.toWebResponse())
    }

    @PostMapping("/inquiries/{inquiryId}/close")
    fun closeInquiry(
        @PathVariable inquiryId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Actor-Role", required = false) actorRole: String?,
        @RequestHeader("X-Actor-Org-Role", required = false) actorOrgRole: String?,
        @RequestHeader("X-Actor-Org-Id", required = false) actorOrgId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
    ): ResponseEntity<Void> {
        val actorAuthContext =
            authzGuardPort.requireActorContext(
                actorUserId = actorUserId,
                actorRole = actorRole,
                actorOrgRole = actorOrgRole,
                actorOrgId = actorOrgId,
                requestId = requestId,
            )
        val result =
            inquiryCommandService.closeInquiry(
                CloseInquiryCommand(
                    inquiryId = inquiryId,
                    actor = actorAuthContext,
                    idempotencyKey = idempotencyKey,
                ),
            )
        return ResponseEntity.status(result.status).build()
    }

    private fun InquiryCreated.toWebResponse(): InquiryWebResponse {
        return InquiryWebResponse(
            id = id,
            organizationId = organizationId,
            occurrenceId = occurrenceId,
            bookingId = bookingId,
            createdByUserId = createdByUserId,
            subject = subject,
            status = status,
            createdAt = createdAt,
        )
    }

    private fun InquiryMessageView.toWebResponse(): InquiryMessageWebResponse {
        return InquiryMessageWebResponse(
            id = id,
            inquiryId = inquiryId,
            senderUserId = senderUserId,
            body = body,
            attachmentAssetIds = attachmentAssetIds,
            createdAt = createdAt,
        )
    }
}
