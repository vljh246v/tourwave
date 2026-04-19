package com.demo.tourwave.application.booking.port

import com.demo.tourwave.domain.booking.RefundReasonCode

data class RefundExecutionRequest(
    val bookingId: Long,
    val actorUserId: Long,
    val refundRequestId: String,
    val reasonCode: RefundReasonCode,
)

sealed interface RefundExecutionResult {
    data class Success(val externalReference: String) : RefundExecutionResult

    data class RetryableFailure(val errorCode: String) : RefundExecutionResult

    data class ReviewRequired(val errorCode: String) : RefundExecutionResult
}

interface RefundExecutionPort {
    fun executeRefund(request: RefundExecutionRequest): RefundExecutionResult
}
