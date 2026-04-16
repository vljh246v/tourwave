package com.demo.tourwave.application.payment

import com.demo.tourwave.application.booking.PaymentLedgerService
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.payment.port.PaymentProviderEventRepository
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.payment.PaymentProviderEvent
import com.demo.tourwave.domain.payment.PaymentProviderEventStatus
import com.demo.tourwave.domain.payment.PaymentProviderEventType
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Clock
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
    val signature: String?,
)

data class PaymentWebhookResult(
    val duplicate: Boolean,
    val eventStatus: PaymentProviderEventStatus,
    val paymentRecordStatus: PaymentRecordStatus? = null,
)

class PaymentWebhookService(
    private val paymentProviderEventRepository: PaymentProviderEventRepository,
    private val bookingRepository: BookingRepository,
    private val paymentLedgerService: PaymentLedgerService,
    webhookSecrets: List<String>,
    private val clock: Clock,
) {
    private val resolvedSecrets =
        webhookSecrets
            .mapNotNull { parseSecret(it) }
            .ifEmpty { listOf(WebhookSecret("default", "tourwave-webhook-secret")) }

    fun receive(command: PaymentWebhookCommand): PaymentWebhookResult {
        paymentProviderEventRepository.findByProviderEventId(command.providerEventId)?.let {
            return PaymentWebhookResult(
                duplicate = true,
                eventStatus = PaymentProviderEventStatus.IGNORED_DUPLICATE,
            )
        }

        val now = clock.instant()
        val signatureVerification = verifySignature(command.rawPayload, command.signature)
        if (!signatureVerification.valid) {
            paymentProviderEventRepository.save(
                PaymentProviderEvent(
                    providerName = command.providerName,
                    providerEventId = command.providerEventId,
                    eventType = command.eventType,
                    bookingId = command.bookingId,
                    payloadJson = command.rawPayload,
                    signature = command.signature,
                    signatureKeyId = signatureVerification.keyId,
                    payloadSha256 = sha256(command.rawPayload),
                    status = PaymentProviderEventStatus.REJECTED_SIGNATURE,
                    note = "invalid-signature",
                    receivedAtUtc = now,
                    processedAtUtc = now,
                ),
            )
            throw unauthorized("Invalid webhook signature")
        }

        var persisted =
            paymentProviderEventRepository.save(
                PaymentProviderEvent(
                    providerName = command.providerName,
                    providerEventId = command.providerEventId,
                    eventType = command.eventType,
                    bookingId = command.bookingId,
                    payloadJson = command.rawPayload,
                    signature = command.signature,
                    signatureKeyId = signatureVerification.keyId,
                    payloadSha256 = sha256(command.rawPayload),
                    status = PaymentProviderEventStatus.RECEIVED,
                    receivedAtUtc = now,
                ),
            )

        if (command.bookingId == null) {
            persisted =
                paymentProviderEventRepository.save(
                    persisted.copy(
                        status = PaymentProviderEventStatus.IGNORED,
                        note = "missing-booking-id",
                        processedAtUtc = clock.instant(),
                    ),
                )
            return PaymentWebhookResult(
                duplicate = false,
                eventStatus = persisted.status,
            )
        }

        val booking = bookingRepository.findById(command.bookingId)
        if (booking == null) {
            persisted =
                paymentProviderEventRepository.save(
                    persisted.copy(
                        status = PaymentProviderEventStatus.IGNORED,
                        note = "booking-not-found",
                        processedAtUtc = clock.instant(),
                    ),
                )
            return PaymentWebhookResult(
                duplicate = false,
                eventStatus = persisted.status,
            )
        }

        try {
            val record = applyEvent(command, booking)

            persisted =
                paymentProviderEventRepository.save(
                    persisted.copy(
                        status = PaymentProviderEventStatus.PROCESSED,
                        note = record.status.name,
                        processedAtUtc = clock.instant(),
                    ),
                )

            return PaymentWebhookResult(
                duplicate = false,
                eventStatus = persisted.status,
                paymentRecordStatus = record.status,
            )
        } catch (exception: Exception) {
            paymentProviderEventRepository.save(
                persisted.copy(
                    status = PaymentProviderEventStatus.POISONED,
                    note = exception.message?.take(255) ?: exception::class.simpleName,
                    processedAtUtc = clock.instant(),
                ),
            )
            throw exception
        }
    }

    fun recordMalformedPayload(
        rawPayload: String,
        signature: String?,
        reason: String? = null,
    ) {
        val now = clock.instant()
        val signatureVerification = verifySignature(rawPayload, signature)
        val payloadHash = sha256(rawPayload)
        paymentProviderEventRepository.findByProviderEventId("malformed-$payloadHash")?.let { return }
        paymentProviderEventRepository.save(
            PaymentProviderEvent(
                providerName = "unknown",
                providerEventId = "malformed-$payloadHash",
                eventType = PaymentProviderEventType.CAPTURE_FAILED,
                bookingId = null,
                payloadJson = rawPayload,
                signature = signature,
                signatureKeyId = signatureVerification.keyId,
                payloadSha256 = payloadHash,
                status =
                    if (signatureVerification.valid) {
                        PaymentProviderEventStatus.MALFORMED_PAYLOAD
                    } else {
                        PaymentProviderEventStatus.REJECTED_SIGNATURE
                    },
                note = reason?.take(255) ?: "malformed-payload",
                receivedAtUtc = now,
                processedAtUtc = now,
            ),
        )
    }

    fun reprocessPoisonedEvent(providerEventId: String): PaymentWebhookResult {
        val persisted =
            paymentProviderEventRepository.findByProviderEventId(providerEventId)
                ?: throw IllegalArgumentException("Webhook event not found: $providerEventId")
        require(persisted.status == PaymentProviderEventStatus.POISONED) {
            "Only poisoned webhook events can be retried"
        }
        val bookingId = requireNotNull(persisted.bookingId) { "Poisoned webhook event is missing booking id" }
        val booking =
            bookingRepository.findById(bookingId)
                ?: throw IllegalArgumentException("Booking not found for webhook event: $providerEventId")
        return try {
            val record =
                applyEvent(
                    PaymentWebhookCommand(
                        providerName = persisted.providerName,
                        providerEventId = persisted.providerEventId,
                        eventType = persisted.eventType,
                        bookingId = persisted.bookingId,
                        rawPayload = persisted.payloadJson,
                        signature = persisted.signature,
                    ),
                    booking,
                )
            paymentProviderEventRepository.save(
                persisted.copy(
                    status = PaymentProviderEventStatus.PROCESSED,
                    note = record.status.name,
                    processedAtUtc = clock.instant(),
                ),
            )
            PaymentWebhookResult(
                duplicate = false,
                eventStatus = PaymentProviderEventStatus.PROCESSED,
                paymentRecordStatus = record.status,
            )
        } catch (exception: Exception) {
            paymentProviderEventRepository.save(
                persisted.copy(
                    status = PaymentProviderEventStatus.POISONED,
                    note = exception.message?.take(255) ?: exception::class.simpleName,
                    processedAtUtc = clock.instant(),
                ),
            )
            throw exception
        }
    }

    private fun applyEvent(
        command: PaymentWebhookCommand,
        booking: com.demo.tourwave.domain.booking.Booking,
    ): com.demo.tourwave.domain.payment.PaymentRecord {
        return when (command.eventType) {
            PaymentProviderEventType.AUTHORIZED -> {
                paymentLedgerService.applyWebhookStatus(
                    bookingId = requireNotNull(command.bookingId),
                    status = PaymentRecordStatus.AUTHORIZED,
                    providerName = command.providerName,
                    providerAuthorizationId = command.providerAuthorizationId,
                    providerReference = command.providerReference,
                    webhookEventId = command.providerEventId,
                )
            }

            PaymentProviderEventType.CAPTURED -> {
                bookingRepository.save(booking.copy(paymentStatus = PaymentStatus.PAID))
                paymentLedgerService.applyWebhookStatus(
                    bookingId = requireNotNull(command.bookingId),
                    status = PaymentRecordStatus.CAPTURED,
                    providerName = command.providerName,
                    providerCaptureId = command.providerCaptureId,
                    providerReference = command.providerReference,
                    webhookEventId = command.providerEventId,
                )
            }

            PaymentProviderEventType.CAPTURE_FAILED -> {
                paymentLedgerService.applyWebhookStatus(
                    bookingId = requireNotNull(command.bookingId),
                    status = PaymentRecordStatus.AUTHORIZED,
                    providerName = command.providerName,
                    errorCode = command.errorCode,
                    webhookEventId = command.providerEventId,
                )
            }

            PaymentProviderEventType.AUTHORIZATION_CANCELED -> {
                paymentLedgerService.applyWebhookStatus(
                    bookingId = requireNotNull(command.bookingId),
                    status = PaymentRecordStatus.NO_REFUND,
                    providerName = command.providerName,
                    providerReference = command.providerReference,
                    webhookEventId = command.providerEventId,
                )
            }

            PaymentProviderEventType.REFUNDED -> {
                bookingRepository.save(booking.copy(paymentStatus = PaymentStatus.REFUNDED))
                paymentLedgerService.applyWebhookStatus(
                    bookingId = requireNotNull(command.bookingId),
                    status = PaymentRecordStatus.REFUNDED,
                    providerName = command.providerName,
                    providerReference = command.providerReference,
                    webhookEventId = command.providerEventId,
                )
            }

            PaymentProviderEventType.REFUND_FAILED -> {
                val status =
                    if (command.retryable) {
                        PaymentRecordStatus.REFUND_FAILED_RETRYABLE
                    } else {
                        PaymentRecordStatus.REFUND_REVIEW_REQUIRED
                    }
                paymentLedgerService.applyWebhookStatus(
                    bookingId = requireNotNull(command.bookingId),
                    status = status,
                    providerName = command.providerName,
                    errorCode = command.errorCode,
                    providerReference = command.providerReference,
                    webhookEventId = command.providerEventId,
                )
            }
        }
    }

    fun expectedSignature(
        rawPayload: String,
        keyId: String? = null,
    ): String {
        val secret = keyId?.let { target -> resolvedSecrets.firstOrNull { it.keyId == target } } ?: resolvedSecrets.first()
        return sign(secret.secret, rawPayload)
    }

    private fun verifySignature(
        rawPayload: String,
        signature: String?,
    ): SignatureVerification {
        if (signature.isNullOrBlank()) {
            return SignatureVerification(valid = false, keyId = null)
        }
        val parsed = parseSignatureHeader(signature.trim())
        val candidates = parsed.keyId?.let { keyId -> resolvedSecrets.filter { it.keyId == keyId } } ?: resolvedSecrets
        if (candidates.isEmpty()) {
            return SignatureVerification(valid = false, keyId = parsed.keyId)
        }
        candidates.forEach { secret ->
            val expected = sign(secret.secret, rawPayload)
            if (MessageDigest.isEqual(expected.toByteArray(StandardCharsets.UTF_8), parsed.signature.toByteArray(StandardCharsets.UTF_8))) {
                return SignatureVerification(valid = true, keyId = secret.keyId)
            }
        }
        return SignatureVerification(valid = false, keyId = parsed.keyId)
    }

    private fun sign(
        secret: String,
        rawPayload: String,
    ): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val bytes = mac.doFinal(rawPayload.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun parseSecret(raw: String): WebhookSecret? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) {
            return null
        }
        val parts = trimmed.split(":", limit = 2)
        return if (parts.size == 2) {
            WebhookSecret(parts[0].trim(), parts[1].trim())
        } else {
            WebhookSecret("default", trimmed)
        }
    }

    private fun parseSignatureHeader(rawSignature: String): ParsedSignature =
        when {
            rawSignature.contains(',') -> {
                val pairs =
                    rawSignature
                        .split(',')
                        .mapNotNull { token ->
                            val parts = token.split('=', limit = 2)
                            if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
                        }.toMap()
                ParsedSignature(
                    keyId = pairs["kid"],
                    signature = pairs["signature"] ?: rawSignature,
                )
            }

            rawSignature.contains(':') -> {
                val parts = rawSignature.split(':', limit = 2)
                ParsedSignature(keyId = parts[0].trim(), signature = parts[1].trim())
            }

            else -> {
                ParsedSignature(keyId = null, signature = rawSignature)
            }
        }

    private fun sha256(rawPayload: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest
            .digest(rawPayload.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun unauthorized(message: String) =
        DomainException(
            errorCode = ErrorCode.UNAUTHORIZED,
            status = 401,
            message = message,
        )

    private data class WebhookSecret(
        val keyId: String,
        val secret: String,
    )

    private data class ParsedSignature(
        val keyId: String?,
        val signature: String,
    )

    private data class SignatureVerification(
        val valid: Boolean,
        val keyId: String?,
    )
}
