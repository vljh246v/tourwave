package com.demo.tourwave.adapter.out.payment

import com.demo.tourwave.application.booking.port.RefundExecutionPort
import com.demo.tourwave.application.booking.port.RefundExecutionRequest
import com.demo.tourwave.application.booking.port.RefundExecutionResult
import com.demo.tourwave.application.payment.PaymentAuthorizationCancelRequest
import com.demo.tourwave.application.payment.PaymentAuthorizationCancelResult
import com.demo.tourwave.application.payment.PaymentAuthorizationRequest
import com.demo.tourwave.application.payment.PaymentAuthorizationResult
import com.demo.tourwave.application.payment.PaymentCaptureRequest
import com.demo.tourwave.application.payment.PaymentCaptureResult
import com.demo.tourwave.application.payment.PaymentProviderPort
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Component
@Profile("alpha", "beta", "real")
class HttpPaymentProviderAdapter(
    @Value("\${integration.payment.base-url}") private val baseUrl: String,
    @Value("\${integration.payment.api-key}") private val apiKey: String,
    @Value("\${integration.payment.provider-name:tourwave-pay}") private val providerName: String,
    private val objectMapper: ObjectMapper
) : PaymentProviderPort, RefundExecutionPort {
    private val httpClient = HttpClient.newBuilder().build()

    override fun authorize(request: PaymentAuthorizationRequest): PaymentAuthorizationResult {
        val response = send("/payments/authorize", request)
        if (response.statusCode() !in 200..299) {
            throw providerFailure(response)
        }
        val body = objectMapper.readTree(response.body())
        return PaymentAuthorizationResult(
            providerName = body.text("providerName") ?: providerName,
            providerPaymentKey = requireField(body, "paymentKey"),
            authorizationId = requireField(body, "authorizationId")
        )
    }

    override fun capture(request: PaymentCaptureRequest): PaymentCaptureResult {
        val response = send("/payments/${request.providerPaymentKey}/capture", request)
        if (response.statusCode() !in 200..299) {
            throw providerFailure(response)
        }
        val body = objectMapper.readTree(response.body())
        return PaymentCaptureResult(
            providerName = body.text("providerName") ?: providerName,
            providerReference = requireField(body, "providerReference"),
            captureId = requireField(body, "captureId")
        )
    }

    override fun cancelAuthorization(request: PaymentAuthorizationCancelRequest): PaymentAuthorizationCancelResult {
        val response = send("/payments/${request.providerPaymentKey}/cancel", request)
        if (response.statusCode() !in 200..299) {
            throw providerFailure(response)
        }
        val body = objectMapper.readTree(response.body())
        return PaymentAuthorizationCancelResult(
            providerName = body.text("providerName") ?: providerName,
            providerReference = requireField(body, "providerReference")
        )
    }

    override fun executeRefund(request: RefundExecutionRequest): RefundExecutionResult {
        val response = send("/refunds", request)
        val body = if (response.body().isBlank()) objectMapper.createObjectNode() else objectMapper.readTree(response.body())
        return when (response.statusCode()) {
            in 200..299 -> RefundExecutionResult.Success(requireField(body, "refundReference"))
            429 -> RefundExecutionResult.RetryableFailure(body.text("errorCode") ?: "RATE_LIMITED")
            in 500..599 -> RefundExecutionResult.RetryableFailure(body.text("errorCode") ?: "PROVIDER_UNAVAILABLE")
            409, 422 -> RefundExecutionResult.ReviewRequired(body.text("errorCode") ?: "REFUND_REVIEW_REQUIRED")
            else -> RefundExecutionResult.ReviewRequired(body.text("errorCode") ?: "REFUND_REJECTED")
        }
    }

    private fun send(path: String, payload: Any): HttpResponse<String> {
        return httpClient.send(
            HttpRequest.newBuilder(URI.create("${baseUrl.trimEnd('/')}$path"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $apiKey")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
    }

    private fun providerFailure(response: HttpResponse<String>): DomainException {
        val body = response.body().takeIf { it.isNotBlank() }?.let(objectMapper::readTree)
        val code = body?.text("errorCode")
        val message = body?.text("message") ?: "payment provider rejected request"
        return DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = if (response.statusCode() == 429 || response.statusCode() >= 500) 503 else 422,
            message = message,
            details = mapOf(
                "provider" to providerName,
                "providerStatus" to response.statusCode(),
                "providerErrorCode" to code
            )
        )
    }

    private fun JsonNode.text(fieldName: String): String? {
        val node = path(fieldName)
        return if (node.isMissingNode || node.isNull) null else node.asText()
    }

    private fun requireField(body: JsonNode, fieldName: String): String {
        return body.text(fieldName)
            ?: throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 503,
                message = "payment provider response is missing $fieldName",
                details = mapOf("provider" to providerName)
            )
    }
}
