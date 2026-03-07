package com.demo.tourwave.adapter.`in`.web.inquiry

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.common.port.ActorAuthContext
import com.demo.tourwave.application.inquiry.CreateInquiryCommand
import com.demo.tourwave.application.inquiry.CloseInquiryCommand
import com.demo.tourwave.application.inquiry.InquiryActorContext
import com.demo.tourwave.application.inquiry.InquiryCommandService
import com.demo.tourwave.application.inquiry.InquiryCreated
import com.demo.tourwave.application.inquiry.InquiryMessageListResult
import com.demo.tourwave.application.inquiry.InquiryMessageView
import com.demo.tourwave.application.inquiry.InquiryQueryService
import com.demo.tourwave.application.inquiry.ListInquiryMessagesQuery
import com.demo.tourwave.application.inquiry.PostInquiryMessageCommand
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class InquiryCommandController(
    private val inquiryCommandService: InquiryCommandService,
    private val inquiryQueryService: InquiryQueryService,
    private val authzGuardPort: AuthzGuardPort
) {
    @PostMapping("/occurrences/{occurrenceId}/inquiries")
    fun createInquiry(
        @PathVariable occurrenceId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
        @RequestBody request: InquiryCreateWebRequest
    ): ResponseEntity<InquiryWebResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val result = inquiryCommandService.createInquiry(
            CreateInquiryCommand(
                occurrenceId = occurrenceId,
                actorUserId = requiredActorUserId,
                idempotencyKey = idempotencyKey,
                bookingId = request.bookingId,
                subject = request.subject,
                message = request.message,
                requestId = requestId
            )
        )

        return ResponseEntity.status(result.status).body(result.inquiry.toWebResponse())
    }

    @GetMapping("/inquiries/{inquiryId}/messages")
    fun listInquiryMessages(
        @PathVariable inquiryId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Actor-Org-Role", required = false) actorOrgRole: String?,
        @RequestHeader("X-Actor-Org-Id", required = false) actorOrgId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) limit: Int?
    ): ResponseEntity<InquiryMessageListWebResponse> {
        val actorAuthContext = authzGuardPort.requireActorContext(
            actorUserId = actorUserId,
            actorOrgRole = actorOrgRole,
            actorOrgId = actorOrgId
        )
        val result = inquiryQueryService.listMessages(
            ListInquiryMessagesQuery(
                inquiryId = inquiryId,
                actor = actorAuthContext.toInquiryActorContext(requestId),
                cursor = cursor,
                limit = limit
            )
        )

        return ResponseEntity.ok(result.toWebResponse())
    }

    @PostMapping("/inquiries/{inquiryId}/messages")
    fun postInquiryMessage(
        @PathVariable inquiryId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Actor-Org-Role", required = false) actorOrgRole: String?,
        @RequestHeader("X-Actor-Org-Id", required = false) actorOrgId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
        @RequestBody request: InquiryMessageCreateWebRequest
    ): ResponseEntity<InquiryMessageWebResponse> {
        val actorAuthContext = authzGuardPort.requireActorContext(
            actorUserId = actorUserId,
            actorOrgRole = actorOrgRole,
            actorOrgId = actorOrgId
        )
        val result = inquiryCommandService.postMessage(
            PostInquiryMessageCommand(
                inquiryId = inquiryId,
                actor = actorAuthContext.toInquiryActorContext(requestId),
                idempotencyKey = idempotencyKey,
                body = request.body,
                attachmentAssetIds = request.attachmentAssetIds
            )
        )

        return ResponseEntity.status(result.status).body(result.message.toWebResponse())
    }

    @PostMapping("/inquiries/{inquiryId}/close")
    fun closeInquiry(
        @PathVariable inquiryId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Actor-Org-Role", required = false) actorOrgRole: String?,
        @RequestHeader("X-Actor-Org-Id", required = false) actorOrgId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?
    ): ResponseEntity<Void> {
        val actorAuthContext = authzGuardPort.requireActorContext(
            actorUserId = actorUserId,
            actorOrgRole = actorOrgRole,
            actorOrgId = actorOrgId
        )
        val result = inquiryCommandService.closeInquiry(
            CloseInquiryCommand(
                inquiryId = inquiryId,
                actor = actorAuthContext.toInquiryActorContext(requestId),
                idempotencyKey = idempotencyKey
            )
        )
        return ResponseEntity.status(result.status).build()
    }

    private fun ActorAuthContext.toInquiryActorContext(requestId: String?): InquiryActorContext {
        return InquiryActorContext(
            actorUserId = actorUserId,
            actorOrgRole = actorOrgRole,
            actorOrgId = actorOrgId,
            requestId = requestId
        )
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
            createdAt = createdAt
        )
    }

    private fun InquiryMessageView.toWebResponse(): InquiryMessageWebResponse {
        return InquiryMessageWebResponse(
            id = id,
            inquiryId = inquiryId,
            senderUserId = senderUserId,
            body = body,
            attachmentAssetIds = attachmentAssetIds,
            createdAt = createdAt
        )
    }

    private fun InquiryMessageListResult.toWebResponse(): InquiryMessageListWebResponse {
        return InquiryMessageListWebResponse(
            items = items.map { it.toWebResponse() },
            nextCursor = nextCursor
        )
    }
}
