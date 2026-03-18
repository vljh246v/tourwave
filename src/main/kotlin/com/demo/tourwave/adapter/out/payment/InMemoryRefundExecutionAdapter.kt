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
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
@Profile("!alpha & !beta & !real")
class InMemoryRefundExecutionAdapter : RefundExecutionPort, PaymentProviderPort {
    private val scriptedResults = ConcurrentHashMap<Long, RefundExecutionResult>()

    override fun authorize(request: PaymentAuthorizationRequest): PaymentAuthorizationResult {
        return PaymentAuthorizationResult(
            providerName = "stub-pay",
            providerPaymentKey = "pay-${request.bookingId}",
            authorizationId = "auth-${request.bookingId}"
        )
    }

    override fun capture(request: PaymentCaptureRequest): PaymentCaptureResult {
        return PaymentCaptureResult(
            providerName = "stub-pay",
            providerReference = "capture-${request.bookingId}",
            captureId = "cap-${request.bookingId}"
        )
    }

    override fun cancelAuthorization(request: PaymentAuthorizationCancelRequest): PaymentAuthorizationCancelResult {
        return PaymentAuthorizationCancelResult(
            providerName = "stub-pay",
            providerReference = "cancel-${request.bookingId}"
        )
    }

    override fun executeRefund(request: RefundExecutionRequest): RefundExecutionResult {
        return scriptedResults.remove(request.bookingId)
            ?: RefundExecutionResult.Success(externalReference = "refund-${request.bookingId}")
    }

    fun scriptRetryableFailure(bookingId: Long, errorCode: String) {
        scriptedResults[bookingId] = RefundExecutionResult.RetryableFailure(errorCode)
    }

    fun scriptReviewRequired(bookingId: Long, errorCode: String) {
        scriptedResults[bookingId] = RefundExecutionResult.ReviewRequired(errorCode)
    }

    fun clear() {
        scriptedResults.clear()
    }
}
