package com.demo.tourwave.domain.payment

import java.time.Instant

enum class PaymentRecordStatus {
    AUTHORIZED,
    CAPTURED,
    REFUND_PENDING,
    REFUNDED,
    NO_REFUND,
    REFUND_FAILED_RETRYABLE,
    REFUND_REVIEW_REQUIRED
}

data class PaymentRecord(
    val id: Long? = null,
    val bookingId: Long,
    val status: PaymentRecordStatus,
    val lastRefundRequestId: String? = null,
    val lastRefundReasonCode: String? = null,
    val lastErrorCode: String? = null,
    val createdAtUtc: Instant,
    val updatedAtUtc: Instant
)
