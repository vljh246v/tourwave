package com.demo.tourwave.application.payment

import com.demo.tourwave.application.booking.RefundRetryService
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import com.demo.tourwave.domain.payment.RefundRemediationAction
import java.time.Clock
import java.time.Duration
import java.time.Instant

data class RefundRemediationCommand(
    val actorUserId: Long,
    val action: RefundRemediationAction = RefundRemediationAction.RETRY,
    val reasonCode: String? = null,
    val note: String? = null,
)

data class RefundOpsQueueItem(
    val bookingId: Long,
    val bookingStatus: BookingStatus?,
    val paymentStatus: PaymentStatus?,
    val recordStatus: PaymentRecordStatus,
    val reviewRequired: Boolean,
    val refundRetryCount: Int,
    val nextRetryAtUtc: Instant?,
    val lastRefundRequestId: String?,
    val lastRefundReasonCode: String?,
    val lastErrorCode: String?,
    val lastProviderReference: String?,
    val lastRemediationAction: RefundRemediationAction?,
    val lastRemediatedByUserId: Long?,
    val lastRemediatedAtUtc: Instant?,
    val updatedAtUtc: Instant,
)

class RefundOperationsService(
    private val paymentRecordRepository: PaymentRecordRepository,
    private val bookingRepository: BookingRepository,
    private val refundRetryService: RefundRetryService,
    private val auditEventPort: AuditEventPort,
    private val maxRetryAttempts: Int,
    private val retryCooldown: Duration,
    private val clock: Clock,
) {
    fun listRefundOpsQueue(): List<RefundOpsQueueItem> {
        return paymentRecordRepository.findByStatuses(
            setOf(
                PaymentRecordStatus.REFUND_PENDING,
                PaymentRecordStatus.REFUND_FAILED_RETRYABLE,
                PaymentRecordStatus.REFUND_REVIEW_REQUIRED,
            ),
        ).map { record ->
            val booking = bookingRepository.findById(record.bookingId)
            RefundOpsQueueItem(
                bookingId = record.bookingId,
                bookingStatus = booking?.status,
                paymentStatus = booking?.paymentStatus,
                recordStatus = record.status,
                reviewRequired = record.status == PaymentRecordStatus.REFUND_REVIEW_REQUIRED,
                refundRetryCount = record.refundRetryCount,
                nextRetryAtUtc = record.nextRetryAtUtc ?: defaultNextRetryAt(record),
                lastRefundRequestId = record.lastRefundRequestId,
                lastRefundReasonCode = record.lastRefundReasonCode,
                lastErrorCode = record.lastErrorCode,
                lastProviderReference = record.lastProviderReference,
                lastRemediationAction = record.lastRemediationAction,
                lastRemediatedByUserId = record.lastRemediatedByUserId,
                lastRemediatedAtUtc = record.lastRemediatedAtUtc,
                updatedAtUtc = record.updatedAtUtc,
            )
        }.sortedByDescending { it.updatedAtUtc }
    }

    fun remediateBookingRefund(
        bookingId: Long,
        command: RefundRemediationCommand,
    ): RefundOpsQueueItem {
        val before =
            paymentRecordRepository.findByBookingId(bookingId)
                ?: throw IllegalArgumentException("Payment record not found for booking $bookingId")

        when (command.action) {
            RefundRemediationAction.RETRY -> refundRetryService.retryBookingRefund(bookingId)
            RefundRemediationAction.MARK_REVIEW_REQUIRED -> {
                paymentRecordRepository.save(
                    before.copy(
                        status = PaymentRecordStatus.REFUND_REVIEW_REQUIRED,
                        nextRetryAtUtc = null,
                        lastRemediationAction = RefundRemediationAction.MARK_REVIEW_REQUIRED,
                        lastRemediatedByUserId = command.actorUserId,
                        lastRemediatedAtUtc = clock.instant(),
                        updatedAtUtc = clock.instant(),
                    ),
                )
            }
        }
        val updated =
            paymentRecordRepository.findByBookingId(bookingId)
                ?: throw IllegalArgumentException("Payment record not found after remediation: $bookingId")
        if (command.action == RefundRemediationAction.RETRY) {
            paymentRecordRepository.save(
                updated.copy(
                    lastRemediationAction = RefundRemediationAction.RETRY,
                    lastRemediatedByUserId = command.actorUserId,
                    lastRemediatedAtUtc = clock.instant(),
                    updatedAtUtc = updated.updatedAtUtc,
                ),
            )
        }
        auditEventPort.append(
            AuditEventCommand(
                actor = "OPERATOR:${command.actorUserId}",
                action = "REFUND_REMEDIATION_${command.action.name}",
                resourceType = "PAYMENT_RECORD",
                resourceId = requireNotNull(updated.id ?: before.id),
                occurredAtUtc = clock.instant(),
                reasonCode = command.reasonCode,
                details =
                    mapOf(
                        "bookingId" to bookingId,
                        "note" to command.note,
                        "action" to command.action.name,
                    ),
                beforeJson =
                    mapOf(
                        "status" to before.status.name,
                        "retryCount" to before.refundRetryCount,
                        "nextRetryAtUtc" to before.nextRetryAtUtc?.toString(),
                    ),
                afterJson =
                    mapOf(
                        "status" to updated.status.name,
                        "retryCount" to updated.refundRetryCount,
                        "nextRetryAtUtc" to updated.nextRetryAtUtc?.toString(),
                    ),
            ),
        )
        return listRefundOpsQueue().firstOrNull { it.bookingId == bookingId }
            ?: run {
                val booking = bookingRepository.findById(bookingId)
                val record =
                    paymentRecordRepository.findByBookingId(bookingId)
                        ?: throw IllegalArgumentException("Payment record not found after remediation: $bookingId")
                RefundOpsQueueItem(
                    bookingId = bookingId,
                    bookingStatus = booking?.status,
                    paymentStatus = booking?.paymentStatus,
                    recordStatus = record.status,
                    reviewRequired = record.status == PaymentRecordStatus.REFUND_REVIEW_REQUIRED,
                    refundRetryCount = record.refundRetryCount,
                    nextRetryAtUtc = record.nextRetryAtUtc ?: defaultNextRetryAt(record),
                    lastRefundRequestId = record.lastRefundRequestId,
                    lastRefundReasonCode = record.lastRefundReasonCode,
                    lastErrorCode = record.lastErrorCode,
                    lastProviderReference = record.lastProviderReference,
                    lastRemediationAction = record.lastRemediationAction,
                    lastRemediatedByUserId = record.lastRemediatedByUserId,
                    lastRemediatedAtUtc = record.lastRemediatedAtUtc,
                    updatedAtUtc = record.updatedAtUtc,
                )
            }
    }

    private fun defaultNextRetryAt(record: com.demo.tourwave.domain.payment.PaymentRecord): Instant? {
        if (record.status != PaymentRecordStatus.REFUND_FAILED_RETRYABLE) {
            return null
        }
        if (record.refundRetryCount >= maxRetryAttempts) {
            return null
        }
        val attemptedAt = record.lastRefundAttemptedAtUtc ?: return null
        return attemptedAt.plus(retryCooldown)
    }
}
