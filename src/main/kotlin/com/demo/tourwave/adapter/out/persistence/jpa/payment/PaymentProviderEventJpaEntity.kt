package com.demo.tourwave.adapter.out.persistence.jpa.payment

import com.demo.tourwave.domain.payment.PaymentProviderEventStatus
import com.demo.tourwave.domain.payment.PaymentProviderEventType
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
    name = "payment_provider_events",
    indexes = [
        Index(name = "idx_payment_provider_events_received", columnList = "received_at_utc"),
        Index(name = "idx_payment_provider_events_booking", columnList = "booking_id"),
        Index(name = "uk_payment_provider_events_provider_event", columnList = "provider_event_id", unique = true)
    ]
)
data class PaymentProviderEventJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "provider_name", nullable = false, length = 64)
    val providerName: String,
    @Column(name = "provider_event_id", nullable = false, length = 128)
    val providerEventId: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    val eventType: PaymentProviderEventType,
    @Column(name = "booking_id")
    val bookingId: Long? = null,
    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    val payloadJson: String,
    @Column(name = "signature", length = 255)
    val signature: String? = null,
    @Column(name = "signature_key_id", length = 64)
    val signatureKeyId: String? = null,
    @Column(name = "payload_sha256", nullable = false, length = 64)
    val payloadSha256: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 64)
    val status: PaymentProviderEventStatus,
    @Column(name = "note", length = 255)
    val note: String? = null,
    @Column(name = "received_at_utc", nullable = false)
    val receivedAtUtc: Instant,
    @Column(name = "processed_at_utc")
    val processedAtUtc: Instant? = null
)
