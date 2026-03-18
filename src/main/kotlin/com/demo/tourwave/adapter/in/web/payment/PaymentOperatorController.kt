package com.demo.tourwave.adapter.`in`.web.payment

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.payment.ReconciliationService
import com.demo.tourwave.application.payment.RefundOperationsService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class PaymentOperatorController(
    private val refundOperationsService: RefundOperationsService,
    private val reconciliationService: ReconciliationService,
    private val authzGuardPort: AuthzGuardPort
) {
    @GetMapping("/operator/payments/refunds/ops")
    fun listRefundOpsQueue(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): List<RefundOpsQueueItemResponse> {
        authzGuardPort.requireActorUserId(actorUserId)
        return refundOperationsService.listRefundOpsQueue().map {
            RefundOpsQueueItemResponse(
                bookingId = it.bookingId,
                bookingStatus = it.bookingStatus?.name,
                paymentStatus = it.paymentStatus?.name,
                recordStatus = it.recordStatus.name,
                refundRetryCount = it.refundRetryCount,
                lastRefundRequestId = it.lastRefundRequestId,
                lastRefundReasonCode = it.lastRefundReasonCode,
                lastErrorCode = it.lastErrorCode,
                lastProviderReference = it.lastProviderReference,
                updatedAtUtc = it.updatedAtUtc.toString()
            )
        }
    }

    @PostMapping("/operator/payments/bookings/{bookingId}/refund-retry")
    fun retryBookingRefund(
        @PathVariable bookingId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): RefundOpsQueueItemResponse {
        authzGuardPort.requireActorUserId(actorUserId)
        val item = refundOperationsService.retryBookingRefund(bookingId)
        return RefundOpsQueueItemResponse(
            bookingId = item.bookingId,
            bookingStatus = item.bookingStatus?.name,
            paymentStatus = item.paymentStatus?.name,
            recordStatus = item.recordStatus.name,
            refundRetryCount = item.refundRetryCount,
            lastRefundRequestId = item.lastRefundRequestId,
            lastRefundReasonCode = item.lastRefundReasonCode,
            lastErrorCode = item.lastErrorCode,
            lastProviderReference = item.lastProviderReference,
            updatedAtUtc = item.updatedAtUtc.toString()
        )
    }

    @GetMapping("/operator/finance/reconciliation/daily")
    fun listDailySummaries(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): List<FinanceDailySummaryResponse> {
        authzGuardPort.requireActorUserId(actorUserId)
        return reconciliationService.listDailySummaries(startDate, endDate).map { it.toResponse() }
    }

    @PostMapping("/operator/finance/reconciliation/daily/{summaryDate}/refresh")
    fun refreshDailySummary(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) summaryDate: LocalDate,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): FinanceDailySummaryResponse {
        authzGuardPort.requireActorUserId(actorUserId)
        return reconciliationService.refreshDailySummary(summaryDate).toResponse()
    }

    @GetMapping("/operator/finance/reconciliation/daily/export", produces = ["text/csv"])
    fun exportDailySummariesCsv(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<String> {
        authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(reconciliationService.exportDailySummariesCsv(startDate, endDate))
    }
}

data class RefundOpsQueueItemResponse(
    val bookingId: Long,
    val bookingStatus: String?,
    val paymentStatus: String?,
    val recordStatus: String,
    val refundRetryCount: Int,
    val lastRefundRequestId: String?,
    val lastRefundReasonCode: String?,
    val lastErrorCode: String?,
    val lastProviderReference: String?,
    val updatedAtUtc: String
)

data class FinanceDailySummaryResponse(
    val summaryDate: String,
    val bookingCreatedCount: Int,
    val authorizedCount: Int,
    val capturedCount: Int,
    val refundPendingCount: Int,
    val refundedCount: Int,
    val noRefundCount: Int,
    val refundFailedRetryableCount: Int,
    val refundReviewRequiredCount: Int,
    val refreshedAtUtc: String
)

private fun com.demo.tourwave.domain.payment.PaymentReconciliationDailySummary.toResponse(): FinanceDailySummaryResponse =
    FinanceDailySummaryResponse(
        summaryDate = summaryDate.toString(),
        bookingCreatedCount = bookingCreatedCount,
        authorizedCount = authorizedCount,
        capturedCount = capturedCount,
        refundPendingCount = refundPendingCount,
        refundedCount = refundedCount,
        noRefundCount = noRefundCount,
        refundFailedRetryableCount = refundFailedRetryableCount,
        refundReviewRequiredCount = refundReviewRequiredCount,
        refreshedAtUtc = refreshedAtUtc.toString()
    )
