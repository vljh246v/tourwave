package com.demo.tourwave.domain.payment

import java.time.Instant

enum class PaymentRecordStatus {
    AUTHORIZED,
    CAPTURED,
    REFUND_PENDING,
    REFUNDED,
    NO_REFUND,
    REFUND_FAILED_RETRYABLE,
    REFUND_REVIEW_REQUIRED,
}

enum class RefundRemediationAction {
    RETRY,
    MARK_REVIEW_REQUIRED,
}

data class PaymentRecord(
    val id: Long? = null,
    val bookingId: Long,
    val status: PaymentRecordStatus,
    val providerName: String? = null,
    val providerPaymentKey: String? = null,
    val providerAuthorizationId: String? = null,
    val providerCaptureId: String? = null,
    val lastRefundRequestId: String? = null,
    val lastProviderReference: String? = null,
    val lastRefundReasonCode: String? = null,
    val lastErrorCode: String? = null,
    val refundRetryCount: Int = 0,
    val lastRefundAttemptedAtUtc: Instant? = null,
    val nextRetryAtUtc: Instant? = null,
    val lastRemediationAction: RefundRemediationAction? = null,
    val lastRemediatedByUserId: Long? = null,
    val lastRemediatedAtUtc: Instant? = null,
    val lastWebhookEventId: String? = null,
    val createdAtUtc: Instant,
    val updatedAtUtc: Instant,
)
