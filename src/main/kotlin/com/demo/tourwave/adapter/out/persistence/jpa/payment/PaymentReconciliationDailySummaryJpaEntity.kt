package com.demo.tourwave.adapter.out.persistence.jpa.payment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "payment_reconciliation_daily_summaries")
data class PaymentReconciliationDailySummaryJpaEntity(
    @Id
    @Column(name = "summary_date", nullable = false)
    val summaryDate: LocalDate,
    @Column(name = "booking_created_count", nullable = false)
    val bookingCreatedCount: Int,
    @Column(name = "authorized_count", nullable = false)
    val authorizedCount: Int,
    @Column(name = "captured_count", nullable = false)
    val capturedCount: Int,
    @Column(name = "refund_pending_count", nullable = false)
    val refundPendingCount: Int,
    @Column(name = "refunded_count", nullable = false)
    val refundedCount: Int,
    @Column(name = "no_refund_count", nullable = false)
    val noRefundCount: Int,
    @Column(name = "refund_failed_retryable_count", nullable = false)
    val refundFailedRetryableCount: Int,
    @Column(name = "refund_review_required_count", nullable = false)
    val refundReviewRequiredCount: Int,
    @Column(name = "refreshed_at_utc", nullable = false)
    val refreshedAtUtc: Instant
)
