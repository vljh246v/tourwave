package com.demo.tourwave.application.payment

import com.demo.tourwave.application.booking.PaymentLedgerService
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.payment.port.PaymentProviderEventRepository
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.payment.PaymentProviderEvent
import com.demo.tourwave.domain.payment.PaymentProviderEventStatus
import com.demo.tourwave.domain.payment.PaymentProviderEventType
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class PaymentWebhookCommand(
    val providerName: String,
    val providerEventId: String,
    val eventType: PaymentProviderEventType,
    val bookingId: Long?,
    val providerAuthorizationId: String? = null,
    val providerCaptureId: String? = null,
    val providerReference: String? = null,
    val errorCode: String? = null,
    val retryable: Boolean = true,
    val rawPayload: String,
    val signature: String?
)

data class PaymentWebhookResult(
    val duplicate: Boolean,
    val eventStatus: PaymentProviderEventStatus,
    val paymentRecordStatus: PaymentRecordStatus? = null
)

class PaymentWebhookService(
    private val paymentProviderEventRepository: PaymentProviderEventRepository,
    private val bookingRepository: BookingRepository,
    private val paymentLedgerService: PaymentLedgerService,
    private val webhookSecret: String,
    private val clock: Clock
) {
    fun receive(command: PaymentWebhookCommand): PaymentWebhookResult {
        paymentProviderEventRepository.findByProviderEventId(command.providerEventId)?.let {
            return PaymentWebhookResult(
                duplicate = true,
                eventStatus = PaymentProviderEventStatus.IGNORED_DUPLICATE
            )
        }

        val now = clock.instant()
        if (!isValidSignature(command.rawPayload, command.signature)) {
            paymentProviderEventRepository.save(
                PaymentProviderEvent(
                    providerName = command.providerName,
                    providerEventId = command.providerEventId,
                    eventType = command.eventType,
                    bookingId = command.bookingId,
                    payloadJson = command.rawPayload,
                    signature = command.signature,
                    status = PaymentProviderEventStatus.REJECTED_SIGNATURE,
                    note = "invalid-signature",
                    receivedAtUtc = now,
                    processedAtUtc = now
                )
            )
            throw IllegalArgumentException("Invalid webhook signature")
        }

        var persisted = paymentProviderEventRepository.save(
            PaymentProviderEvent(
                providerName = command.providerName,
                providerEventId = command.providerEventId,
                eventType = command.eventType,
                bookingId = command.bookingId,
                payloadJson = command.rawPayload,
                signature = command.signature,
                status = PaymentProviderEventStatus.RECEIVED,
                receivedAtUtc = now
            )
        )

        if (command.bookingId == null) {
            persisted = paymentProviderEventRepository.save(
                persisted.copy(
                    status = PaymentProviderEventStatus.IGNORED,
                    note = "missing-booking-id",
                    processedAtUtc = clock.instant()
                )
            )
            return PaymentWebhookResult(
                duplicate = false,
                eventStatus = persisted.status
            )
        }

        val booking = bookingRepository.findById(command.bookingId)
        if (booking == null) {
            persisted = paymentProviderEventRepository.save(
                persisted.copy(
                    status = PaymentProviderEventStatus.IGNORED,
                    note = "booking-not-found",
                    processedAtUtc = clock.instant()
                )
            )
            return PaymentWebhookResult(
                duplicate = false,
                eventStatus = persisted.status
            )
        }

        val record = when (command.eventType) {
            PaymentProviderEventType.AUTHORIZED -> {
                paymentLedgerService.applyWebhookStatus(
                    bookingId = command.bookingId,
                    status = PaymentRecordStatus.AUTHORIZED,
                    providerName = command.providerName,
                    providerAuthorizationId = command.providerAuthorizationId,
                    providerReference = command.providerReference,
                    webhookEventId = command.providerEventId
                )
            }

            PaymentProviderEventType.CAPTURED -> {
                bookingRepository.save(booking.copy(paymentStatus = PaymentStatus.PAID))
                paymentLedgerService.applyWebhookStatus(
                    bookingId = command.bookingId,
                    status = PaymentRecordStatus.CAPTURED,
                    providerName = command.providerName,
                    providerCaptureId = command.providerCaptureId,
                    providerReference = command.providerReference,
                    webhookEventId = command.providerEventId
                )
            }

            PaymentProviderEventType.CAPTURE_FAILED -> {
                paymentLedgerService.applyWebhookStatus(
                    bookingId = command.bookingId,
                    status = PaymentRecordStatus.AUTHORIZED,
                    providerName = command.providerName,
                    errorCode = command.errorCode,
                    webhookEventId = command.providerEventId
                )
            }

            PaymentProviderEventType.AUTHORIZATION_CANCELED -> {
                paymentLedgerService.applyWebhookStatus(
                    bookingId = command.bookingId,
                    status = PaymentRecordStatus.NO_REFUND,
                    providerName = command.providerName,
                    providerReference = command.providerReference,
                    webhookEventId = command.providerEventId
                )
            }

            PaymentProviderEventType.REFUNDED -> {
                bookingRepository.save(booking.copy(paymentStatus = PaymentStatus.REFUNDED))
                paymentLedgerService.applyWebhookStatus(
                    bookingId = command.bookingId,
                    status = PaymentRecordStatus.REFUNDED,
                    providerName = command.providerName,
                    providerReference = command.providerReference,
                    webhookEventId = command.providerEventId
                )
            }

            PaymentProviderEventType.REFUND_FAILED -> {
                val status = if (command.retryable) {
                    PaymentRecordStatus.REFUND_FAILED_RETRYABLE
                } else {
                    PaymentRecordStatus.REFUND_REVIEW_REQUIRED
                }
                paymentLedgerService.applyWebhookStatus(
                    bookingId = command.bookingId,
                    status = status,
                    providerName = command.providerName,
                    errorCode = command.errorCode,
                    providerReference = command.providerReference,
                    webhookEventId = command.providerEventId
                )
            }
        }

        persisted = paymentProviderEventRepository.save(
            persisted.copy(
                status = PaymentProviderEventStatus.PROCESSED,
                note = record.status.name,
                processedAtUtc = clock.instant()
            )
        )

        return PaymentWebhookResult(
            duplicate = false,
            eventStatus = persisted.status,
            paymentRecordStatus = record.status
        )
    }

    fun expectedSignature(rawPayload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(webhookSecret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val bytes = mac.doFinal(rawPayload.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun isValidSignature(rawPayload: String, signature: String?): Boolean {
        if (signature.isNullOrBlank()) {
            return false
        }
        val expected = expectedSignature(rawPayload)
        return MessageDigest.isEqual(
            expected.toByteArray(StandardCharsets.UTF_8),
            signature.trim().toByteArray(StandardCharsets.UTF_8)
        )
    }
}
