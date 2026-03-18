package com.demo.tourwave.domain.payment

import java.time.Instant
import java.time.LocalDate

data class PaymentReconciliationDailySummary(
    val summaryDate: LocalDate,
    val bookingCreatedCount: Int,
    val authorizedCount: Int,
    val capturedCount: Int,
    val providerCapturedCount: Int,
    val providerRefundedCount: Int,
    val refundPendingCount: Int,
    val refundedCount: Int,
    val noRefundCount: Int,
    val refundFailedRetryableCount: Int,
    val refundReviewRequiredCount: Int,
    val captureMismatchCount: Int,
    val refundMismatchCount: Int,
    val internalStatusMismatchCount: Int,
    val refreshedAtUtc: Instant
)
