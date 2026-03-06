package com.demo.tourwave.adapter.`in`.web.inquiry

import com.demo.tourwave.application.inquiry.CreateInquiryCommand
import com.demo.tourwave.application.inquiry.InquiryCommandService
import com.demo.tourwave.application.inquiry.InquiryCreated
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class InquiryCommandController(
    private val inquiryCommandService: InquiryCommandService
) {
    @PostMapping("/occurrences/{occurrenceId}/inquiries")
    fun createInquiry(
        @PathVariable occurrenceId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
        @RequestBody request: InquiryCreateWebRequest
    ): ResponseEntity<InquiryWebResponse> {
        val result = inquiryCommandService.createInquiry(
            CreateInquiryCommand(
                occurrenceId = occurrenceId,
                actorUserId = actorUserId ?: 1L,
                idempotencyKey = idempotencyKey,
                bookingId = request.bookingId,
                subject = request.subject,
                message = request.message,
                requestId = requestId
            )
        )

        return ResponseEntity.status(result.status).body(result.inquiry.toWebResponse())
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
}

