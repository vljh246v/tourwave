package com.demo.tourwave.domain.payment

import java.time.Instant

enum class PaymentProviderEventType {
    AUTHORIZED,
    CAPTURED,
    CAPTURE_FAILED,
    AUTHORIZATION_CANCELED,
    REFUNDED,
    REFUND_FAILED,
}

enum class PaymentProviderEventStatus {
    RECEIVED,
    PROCESSED,
    IGNORED_DUPLICATE,
    REJECTED_SIGNATURE,
    MALFORMED_PAYLOAD,
    POISONED,
    IGNORED,
}

data class PaymentProviderEvent(
    val id: Long? = null,
    val providerName: String,
    val providerEventId: String,
    val eventType: PaymentProviderEventType,
    val bookingId: Long?,
    val payloadJson: String,
    val signature: String? = null,
    val signatureKeyId: String? = null,
    val payloadSha256: String,
    val status: PaymentProviderEventStatus,
    val note: String? = null,
    val receivedAtUtc: Instant,
    val processedAtUtc: Instant? = null,
)
