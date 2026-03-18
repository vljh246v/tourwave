package com.demo.tourwave.application.payment

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.payment.port.PaymentProviderEventRepository
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.payment.PaymentProviderEventType
import com.demo.tourwave.application.payment.port.PaymentReconciliationSummaryRepository
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import com.demo.tourwave.domain.payment.PaymentReconciliationDailySummary
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

enum class PaymentReconciliationMismatchType {
    PROVIDER_CAPTURE_NOT_APPLIED,
    PROVIDER_REFUND_NOT_APPLIED,
    INTERNAL_STATUS_DRIFT
}

data class PaymentReconciliationMismatch(
    val mismatchType: PaymentReconciliationMismatchType,
    val summaryDate: LocalDate,
    val bookingId: Long?,
    val providerEventId: String? = null,
    val bookingPaymentStatus: String? = null,
    val recordStatus: String? = null,
    val providerEventType: String? = null,
    val note: String? = null
)

data class FinanceReconciliationJobResult(
    val refreshedDate: LocalDate,
    val refreshedAtUtc: java.time.Instant
)

class ReconciliationService(
    private val bookingRepository: BookingRepository,
    private val paymentRecordRepository: PaymentRecordRepository,
    private val paymentProviderEventRepository: PaymentProviderEventRepository,
    private val paymentReconciliationSummaryRepository: PaymentReconciliationSummaryRepository,
    private val clock: Clock
) {
    fun refreshDailySummary(summaryDate: LocalDate): PaymentReconciliationDailySummary {
        val start = summaryDate.atStartOfDay().toInstant(ZoneOffset.UTC)
        val end = summaryDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        val bookingCreatedCount = bookingRepository.findAll().count { !it.createdAt.isBefore(start) && it.createdAt.isBefore(end) }
        val paymentRecords = paymentRecordRepository.findUpdatedBetween(start, end)
        val statusCounts = paymentRecords.groupingBy { it.status }.eachCount()
        val mismatches = listMismatches(summaryDate, summaryDate)
        val providerEvents = paymentProviderEventRepository.findReceivedBetween(start, end)
        val providerCapturedCount = providerEvents.count { it.eventType == PaymentProviderEventType.CAPTURED }
        val providerRefundedCount = providerEvents.count { it.eventType == PaymentProviderEventType.REFUNDED }

        return paymentReconciliationSummaryRepository.save(
            PaymentReconciliationDailySummary(
                summaryDate = summaryDate,
                bookingCreatedCount = bookingCreatedCount,
                authorizedCount = statusCounts[PaymentRecordStatus.AUTHORIZED] ?: 0,
                capturedCount = statusCounts[PaymentRecordStatus.CAPTURED] ?: 0,
                providerCapturedCount = providerCapturedCount,
                providerRefundedCount = providerRefundedCount,
                refundPendingCount = statusCounts[PaymentRecordStatus.REFUND_PENDING] ?: 0,
                refundedCount = statusCounts[PaymentRecordStatus.REFUNDED] ?: 0,
                noRefundCount = statusCounts[PaymentRecordStatus.NO_REFUND] ?: 0,
                refundFailedRetryableCount = statusCounts[PaymentRecordStatus.REFUND_FAILED_RETRYABLE] ?: 0,
                refundReviewRequiredCount = statusCounts[PaymentRecordStatus.REFUND_REVIEW_REQUIRED] ?: 0,
                captureMismatchCount = mismatches.count { it.mismatchType == PaymentReconciliationMismatchType.PROVIDER_CAPTURE_NOT_APPLIED },
                refundMismatchCount = mismatches.count { it.mismatchType == PaymentReconciliationMismatchType.PROVIDER_REFUND_NOT_APPLIED },
                internalStatusMismatchCount = mismatches.count { it.mismatchType == PaymentReconciliationMismatchType.INTERNAL_STATUS_DRIFT },
                refreshedAtUtc = clock.instant()
            )
        )
    }

    fun getDailySummary(summaryDate: LocalDate): PaymentReconciliationDailySummary {
        return paymentReconciliationSummaryRepository.findByDate(summaryDate) ?: refreshDailySummary(summaryDate)
    }

    fun listDailySummaries(startDate: LocalDate, endDate: LocalDate): List<PaymentReconciliationDailySummary> {
        return paymentReconciliationSummaryRepository.findBetween(startDate, endDate)
    }

    fun exportDailySummariesCsv(startDate: LocalDate, endDate: LocalDate): String {
        val rows = listDailySummaries(startDate, endDate)
        return buildString {
            append("date,bookingCreatedCount,authorizedCount,capturedCount,providerCapturedCount,providerRefundedCount,refundPendingCount,refundedCount,noRefundCount,refundFailedRetryableCount,refundReviewRequiredCount,captureMismatchCount,refundMismatchCount,internalStatusMismatchCount,refreshedAtUtc\n")
            rows.forEach { row ->
                append(row.summaryDate).append(',')
                append(row.bookingCreatedCount).append(',')
                append(row.authorizedCount).append(',')
                append(row.capturedCount).append(',')
                append(row.providerCapturedCount).append(',')
                append(row.providerRefundedCount).append(',')
                append(row.refundPendingCount).append(',')
                append(row.refundedCount).append(',')
                append(row.noRefundCount).append(',')
                append(row.refundFailedRetryableCount).append(',')
                append(row.refundReviewRequiredCount).append(',')
                append(row.captureMismatchCount).append(',')
                append(row.refundMismatchCount).append(',')
                append(row.internalStatusMismatchCount).append(',')
                append(row.refreshedAtUtc).append('\n')
            }
        }
    }

    fun listMismatches(startDate: LocalDate, endDate: LocalDate): List<PaymentReconciliationMismatch> {
        val start = startDate.atStartOfDay().toInstant(ZoneOffset.UTC)
        val end = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        val bookings = bookingRepository.findAll().associateBy { requireNotNull(it.id) }
        val paymentRecords = paymentRecordRepository.findAll().associateBy { it.bookingId }
        val mismatches = mutableListOf<PaymentReconciliationMismatch>()

        paymentProviderEventRepository.findReceivedBetween(start, end)
            .filter { it.status == com.demo.tourwave.domain.payment.PaymentProviderEventStatus.PROCESSED }
            .forEach { event ->
                val booking = event.bookingId?.let(bookings::get)
                val record = event.bookingId?.let(paymentRecords::get)
                when (event.eventType) {
                    PaymentProviderEventType.CAPTURED -> {
                        if (booking?.paymentStatus != PaymentStatus.PAID || record?.status != PaymentRecordStatus.CAPTURED) {
                            mismatches += PaymentReconciliationMismatch(
                                mismatchType = PaymentReconciliationMismatchType.PROVIDER_CAPTURE_NOT_APPLIED,
                                summaryDate = event.receivedAtUtc.atOffset(ZoneOffset.UTC).toLocalDate(),
                                bookingId = event.bookingId,
                                providerEventId = event.providerEventId,
                                bookingPaymentStatus = booking?.paymentStatus?.name,
                                recordStatus = record?.status?.name,
                                providerEventType = event.eventType.name,
                                note = event.note
                            )
                        }
                    }

                    PaymentProviderEventType.REFUNDED -> {
                        if (booking?.paymentStatus != PaymentStatus.REFUNDED || record?.status != PaymentRecordStatus.REFUNDED) {
                            mismatches += PaymentReconciliationMismatch(
                                mismatchType = PaymentReconciliationMismatchType.PROVIDER_REFUND_NOT_APPLIED,
                                summaryDate = event.receivedAtUtc.atOffset(ZoneOffset.UTC).toLocalDate(),
                                bookingId = event.bookingId,
                                providerEventId = event.providerEventId,
                                bookingPaymentStatus = booking?.paymentStatus?.name,
                                recordStatus = record?.status?.name,
                                providerEventType = event.eventType.name,
                                note = event.note
                            )
                        }
                    }

                    else -> Unit
                }
            }

        paymentRecords.values
            .filter { !it.updatedAtUtc.isBefore(start) && it.updatedAtUtc.isBefore(end) }
            .forEach { record ->
                val booking = bookings[record.bookingId] ?: return@forEach
                val drift = when {
                    booking.paymentStatus == PaymentStatus.PAID && record.status != PaymentRecordStatus.CAPTURED -> true
                    booking.paymentStatus == PaymentStatus.REFUNDED && record.status != PaymentRecordStatus.REFUNDED -> true
                    booking.paymentStatus == PaymentStatus.AUTHORIZED && record.status == PaymentRecordStatus.CAPTURED -> true
                    else -> false
                }
                if (drift) {
                    mismatches += PaymentReconciliationMismatch(
                        mismatchType = PaymentReconciliationMismatchType.INTERNAL_STATUS_DRIFT,
                        summaryDate = record.updatedAtUtc.atOffset(ZoneOffset.UTC).toLocalDate(),
                        bookingId = record.bookingId,
                        bookingPaymentStatus = booking.paymentStatus.name,
                        recordStatus = record.status.name,
                        note = "booking and payment ledger diverged"
                    )
                }
            }

        return mismatches.sortedWith(compareBy<PaymentReconciliationMismatch> { it.summaryDate }.thenBy { it.bookingId ?: Long.MAX_VALUE })
    }

    fun exportMismatchesCsv(startDate: LocalDate, endDate: LocalDate): String {
        val rows = listMismatches(startDate, endDate)
        return buildString {
            append("summaryDate,mismatchType,bookingId,providerEventId,bookingPaymentStatus,recordStatus,providerEventType,note\n")
            rows.forEach { row ->
                append(row.summaryDate).append(',')
                append(row.mismatchType.name).append(',')
                append(row.bookingId ?: "").append(',')
                append(row.providerEventId ?: "").append(',')
                append(row.bookingPaymentStatus ?: "").append(',')
                append(row.recordStatus ?: "").append(',')
                append(row.providerEventType ?: "").append(',')
                append(csv(row.note)).append('\n')
            }
        }
    }

    fun refreshYesterday(): FinanceReconciliationJobResult {
        val summaryDate = LocalDate.now(clock).minusDays(1)
        val summary = refreshDailySummary(summaryDate)
        return FinanceReconciliationJobResult(
            refreshedDate = summary.summaryDate,
            refreshedAtUtc = summary.refreshedAtUtc
        )
    }

    private fun csv(value: String?): String {
        val raw = value ?: return ""
        val escaped = raw.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
