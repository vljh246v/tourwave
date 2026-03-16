package com.demo.tourwave.adapter.out.payment

import com.demo.tourwave.application.booking.port.RefundExecutionPort
import com.demo.tourwave.application.booking.port.RefundExecutionRequest
import com.demo.tourwave.application.booking.port.RefundExecutionResult
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class InMemoryRefundExecutionAdapter : RefundExecutionPort {
    private val scriptedResults = ConcurrentHashMap<Long, RefundExecutionResult>()

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
