package com.demo.tourwave.adapter.`in`.web.booking

import com.demo.tourwave.application.booking.BookingRefundPreviewService
import com.demo.tourwave.application.booking.BookingRefundPreviewView
import com.demo.tourwave.application.booking.GetBookingRefundPreviewQuery
import com.demo.tourwave.application.common.port.AuthzGuardPort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class BookingRefundPreviewController(
    private val bookingRefundPreviewService: BookingRefundPreviewService,
    private val authzGuardPort: AuthzGuardPort,
) {
    @GetMapping("/bookings/{bookingId}/refund-preview")
    fun getRefundPreview(
        @PathVariable bookingId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Actor-Org-Role", required = false) actorOrgRole: String?,
        @RequestHeader("X-Actor-Org-Id", required = false) actorOrgId: Long?,
    ): ResponseEntity<BookingRefundPreviewWebResponse> {
        val actor =
            authzGuardPort.requireActorContext(
                actorUserId = actorUserId,
                actorOrgRole = actorOrgRole,
                actorOrgId = actorOrgId,
            )
        val result =
            bookingRefundPreviewService.getPreview(
                GetBookingRefundPreviewQuery(
                    bookingId = bookingId,
                    actor = actor,
                ),
            )
        return ResponseEntity.ok(result.toWebResponse())
    }

    private fun BookingRefundPreviewView.toWebResponse(): BookingRefundPreviewWebResponse {
        return BookingRefundPreviewWebResponse(
            bookingId = bookingId,
            cancelable = cancelable,
            status = status,
            paymentStatus = paymentStatus,
            refundDecisionType = refundDecisionType,
            refundReasonCode = refundReasonCode,
            refundable = refundable,
            occurrenceStartsAtUtc = occurrenceStartsAtUtc,
            evaluatedAtUtc = evaluatedAtUtc,
        )
    }
}
