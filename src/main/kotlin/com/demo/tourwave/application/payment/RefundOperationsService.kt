package com.demo.tourwave.application.payment

import com.demo.tourwave.application.booking.RefundRetryService
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import java.time.Instant

data class RefundOpsQueueItem(
    val bookingId: Long,
    val bookingStatus: BookingStatus?,
    val paymentStatus: PaymentStatus?,
    val recordStatus: PaymentRecordStatus,
    val refundRetryCount: Int,
    val lastRefundRequestId: String?,
    val lastRefundReasonCode: String?,
    val lastErrorCode: String?,
    val lastProviderReference: String?,
    val updatedAtUtc: Instant
)

class RefundOperationsService(
    private val paymentRecordRepository: PaymentRecordRepository,
    private val bookingRepository: BookingRepository,
    private val refundRetryService: RefundRetryService
) {
    fun listRefundOpsQueue(): List<RefundOpsQueueItem> {
        return paymentRecordRepository.findByStatuses(
            setOf(
                PaymentRecordStatus.REFUND_PENDING,
                PaymentRecordStatus.REFUND_FAILED_RETRYABLE,
                PaymentRecordStatus.REFUND_REVIEW_REQUIRED
            )
        ).map { record ->
            val booking = bookingRepository.findById(record.bookingId)
            RefundOpsQueueItem(
                bookingId = record.bookingId,
                bookingStatus = booking?.status,
                paymentStatus = booking?.paymentStatus,
                recordStatus = record.status,
                refundRetryCount = record.refundRetryCount,
                lastRefundRequestId = record.lastRefundRequestId,
                lastRefundReasonCode = record.lastRefundReasonCode,
                lastErrorCode = record.lastErrorCode,
                lastProviderReference = record.lastProviderReference,
                updatedAtUtc = record.updatedAtUtc
            )
        }.sortedByDescending { it.updatedAtUtc }
    }

    fun retryBookingRefund(bookingId: Long): RefundOpsQueueItem {
        refundRetryService.retryBookingRefund(bookingId)
        return listRefundOpsQueue().firstOrNull { it.bookingId == bookingId }
            ?: run {
                val booking = bookingRepository.findById(bookingId)
                val record = paymentRecordRepository.findByBookingId(bookingId)
                    ?: throw IllegalArgumentException("Payment record not found after retry: $bookingId")
                RefundOpsQueueItem(
                    bookingId = bookingId,
                    bookingStatus = booking?.status,
                    paymentStatus = booking?.paymentStatus,
                    recordStatus = record.status,
                    refundRetryCount = record.refundRetryCount,
                    lastRefundRequestId = record.lastRefundRequestId,
                    lastRefundReasonCode = record.lastRefundReasonCode,
                    lastErrorCode = record.lastErrorCode,
                    lastProviderReference = record.lastProviderReference,
                    updatedAtUtc = record.updatedAtUtc
                )
            }
    }
}
