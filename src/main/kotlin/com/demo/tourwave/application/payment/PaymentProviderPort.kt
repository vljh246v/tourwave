package com.demo.tourwave.application.payment

data class PaymentAuthorizationRequest(
    val bookingId: Long,
    val actorUserId: Long,
    val amount: Int,
    val currency: String
)

data class PaymentAuthorizationResult(
    val providerName: String,
    val providerPaymentKey: String,
    val authorizationId: String
)

data class PaymentCaptureRequest(
    val bookingId: Long,
    val actorUserId: Long,
    val providerPaymentKey: String
)

data class PaymentCaptureResult(
    val providerName: String,
    val providerReference: String,
    val captureId: String
)

data class PaymentAuthorizationCancelRequest(
    val bookingId: Long,
    val actorUserId: Long,
    val providerPaymentKey: String
)

data class PaymentAuthorizationCancelResult(
    val providerName: String,
    val providerReference: String
)

interface PaymentProviderPort {
    fun authorize(request: PaymentAuthorizationRequest): PaymentAuthorizationResult
    fun capture(request: PaymentCaptureRequest): PaymentCaptureResult
    fun cancelAuthorization(request: PaymentAuthorizationCancelRequest): PaymentAuthorizationCancelResult
}
