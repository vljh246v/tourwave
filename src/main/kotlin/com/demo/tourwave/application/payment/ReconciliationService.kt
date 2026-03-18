package com.demo.tourwave.application.payment

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.payment.port.PaymentReconciliationSummaryRepository
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import com.demo.tourwave.domain.payment.PaymentReconciliationDailySummary
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

data class FinanceReconciliationJobResult(
    val refreshedDate: LocalDate,
    val refreshedAtUtc: java.time.Instant
)

class ReconciliationService(
    private val bookingRepository: BookingRepository,
    private val paymentRecordRepository: PaymentRecordRepository,
    private val paymentReconciliationSummaryRepository: PaymentReconciliationSummaryRepository,
    private val clock: Clock
) {
    fun refreshDailySummary(summaryDate: LocalDate): PaymentReconciliationDailySummary {
        val start = summaryDate.atStartOfDay().toInstant(ZoneOffset.UTC)
        val end = summaryDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        val bookingCreatedCount = bookingRepository.findAll().count { !it.createdAt.isBefore(start) && it.createdAt.isBefore(end) }
        val paymentRecords = paymentRecordRepository.findUpdatedBetween(start, end)
        val statusCounts = paymentRecords.groupingBy { it.status }.eachCount()

        return paymentReconciliationSummaryRepository.save(
            PaymentReconciliationDailySummary(
                summaryDate = summaryDate,
                bookingCreatedCount = bookingCreatedCount,
                authorizedCount = statusCounts[PaymentRecordStatus.AUTHORIZED] ?: 0,
                capturedCount = statusCounts[PaymentRecordStatus.CAPTURED] ?: 0,
                refundPendingCount = statusCounts[PaymentRecordStatus.REFUND_PENDING] ?: 0,
                refundedCount = statusCounts[PaymentRecordStatus.REFUNDED] ?: 0,
                noRefundCount = statusCounts[PaymentRecordStatus.NO_REFUND] ?: 0,
                refundFailedRetryableCount = statusCounts[PaymentRecordStatus.REFUND_FAILED_RETRYABLE] ?: 0,
                refundReviewRequiredCount = statusCounts[PaymentRecordStatus.REFUND_REVIEW_REQUIRED] ?: 0,
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
            append("date,bookingCreatedCount,authorizedCount,capturedCount,refundPendingCount,refundedCount,noRefundCount,refundFailedRetryableCount,refundReviewRequiredCount,refreshedAtUtc\n")
            rows.forEach { row ->
                append(row.summaryDate).append(',')
                append(row.bookingCreatedCount).append(',')
                append(row.authorizedCount).append(',')
                append(row.capturedCount).append(',')
                append(row.refundPendingCount).append(',')
                append(row.refundedCount).append(',')
                append(row.noRefundCount).append(',')
                append(row.refundFailedRetryableCount).append(',')
                append(row.refundReviewRequiredCount).append(',')
                append(row.refreshedAtUtc).append('\n')
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
}
