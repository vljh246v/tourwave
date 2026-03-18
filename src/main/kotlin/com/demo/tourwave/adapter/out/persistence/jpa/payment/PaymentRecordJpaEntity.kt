package com.demo.tourwave.adapter.out.persistence.jpa.payment

import com.demo.tourwave.domain.payment.PaymentRecordStatus
import com.demo.tourwave.domain.payment.RefundRemediationAction
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "payment_records",
    indexes = [Index(name = "idx_payment_records_booking", columnList = "booking_id", unique = true)]
)
data class PaymentRecordJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "booking_id", nullable = false)
    val bookingId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    val status: PaymentRecordStatus,
    @Column(name = "provider_name", length = 64)
    val providerName: String? = null,
    @Column(name = "provider_payment_key", length = 128)
    val providerPaymentKey: String? = null,
    @Column(name = "provider_authorization_id", length = 128)
    val providerAuthorizationId: String? = null,
    @Column(name = "provider_capture_id", length = 128)
    val providerCaptureId: String? = null,
    @Column(name = "last_refund_request_id", length = 128)
    val lastRefundRequestId: String? = null,
    @Column(name = "last_provider_reference", length = 128)
    val lastProviderReference: String? = null,
    @Column(name = "last_refund_reason_code", length = 128)
    val lastRefundReasonCode: String? = null,
    @Column(name = "last_error_code", length = 128)
    val lastErrorCode: String? = null,
    @Column(name = "refund_retry_count", nullable = false)
    val refundRetryCount: Int = 0,
    @Column(name = "last_refund_attempted_at_utc")
    val lastRefundAttemptedAtUtc: Instant? = null,
    @Column(name = "next_retry_at_utc")
    val nextRetryAtUtc: Instant? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "last_remediation_action", length = 64)
    val lastRemediationAction: RefundRemediationAction? = null,
    @Column(name = "last_remediated_by_user_id")
    val lastRemediatedByUserId: Long? = null,
    @Column(name = "last_remediated_at_utc")
    val lastRemediatedAtUtc: Instant? = null,
    @Column(name = "last_webhook_event_id", length = 128)
    val lastWebhookEventId: String? = null,
    @Column(name = "created_at_utc", nullable = false)
    val createdAtUtc: Instant,
    @Column(name = "updated_at_utc", nullable = false)
    val updatedAtUtc: Instant
)
