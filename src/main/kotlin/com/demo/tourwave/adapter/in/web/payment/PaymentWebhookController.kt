package com.demo.tourwave.adapter.`in`.web.payment

import com.demo.tourwave.application.payment.PaymentWebhookCommand
import com.demo.tourwave.application.payment.PaymentWebhookService
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.payment.PaymentProviderEventType
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class PaymentWebhookController(
    private val paymentWebhookService: PaymentWebhookService,
    private val objectMapper: ObjectMapper
) {
    @PostMapping("/payments/webhooks/provider")
    fun receiveProviderWebhook(
        @RequestHeader("X-Payment-Signature", required = false) signature: String?,
        @RequestBody rawBody: String
    ): ResponseEntity<PaymentWebhookResponse> {
        val request = try {
            objectMapper.readValue(rawBody, PaymentWebhookRequest::class.java)
        } catch (exception: Exception) {
            paymentWebhookService.recordMalformedPayload(rawBody, signature, exception.message)
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 422,
                message = "Malformed payment webhook payload"
            )
        }
        val result = try {
            paymentWebhookService.receive(
                PaymentWebhookCommand(
                    providerName = request.providerName,
                    providerEventId = request.providerEventId,
                    eventType = request.eventType,
                    bookingId = request.bookingId,
                    providerAuthorizationId = request.providerAuthorizationId,
                    providerCaptureId = request.providerCaptureId,
                    providerReference = request.providerReference,
                    errorCode = request.errorCode,
                    retryable = request.retryable,
                    rawPayload = rawBody,
                    signature = signature
                )
            )
        } catch (exception: DomainException) {
            throw exception
        }

        return ResponseEntity.status(if (result.duplicate) HttpStatus.OK else HttpStatus.ACCEPTED)
            .body(
                PaymentWebhookResponse(
                    duplicate = result.duplicate,
                    eventStatus = result.eventStatus.name,
                    paymentRecordStatus = result.paymentRecordStatus?.name
                )
            )
    }
}

data class PaymentWebhookRequest(
    @field:NotBlank
    val providerName: String,
    @field:NotBlank
    val providerEventId: String,
    @field:NotNull
    val eventType: PaymentProviderEventType,
    val bookingId: Long? = null,
    val providerAuthorizationId: String? = null,
    val providerCaptureId: String? = null,
    val providerReference: String? = null,
    val errorCode: String? = null,
    val retryable: Boolean = true
)

data class PaymentWebhookResponse(
    val duplicate: Boolean,
    val eventStatus: String,
    val paymentRecordStatus: String?
)
