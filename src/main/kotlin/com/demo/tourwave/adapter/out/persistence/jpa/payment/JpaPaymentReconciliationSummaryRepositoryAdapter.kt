package com.demo.tourwave.adapter.out.persistence.jpa.payment

import com.demo.tourwave.application.payment.port.PaymentReconciliationSummaryRepository
import com.demo.tourwave.domain.payment.PaymentReconciliationDailySummary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
@Profile("mysql", "mysql-test")
class JpaPaymentReconciliationSummaryRepositoryAdapter(
    private val paymentReconciliationDailySummaryJpaRepository: PaymentReconciliationDailySummaryJpaRepository
) : PaymentReconciliationSummaryRepository {
    override fun save(summary: PaymentReconciliationDailySummary): PaymentReconciliationDailySummary =
        paymentReconciliationDailySummaryJpaRepository.save(summary.toEntity()).toDomain()

    override fun findByDate(summaryDate: LocalDate): PaymentReconciliationDailySummary? =
        paymentReconciliationDailySummaryJpaRepository.findById(summaryDate).orElse(null)?.toDomain()

    override fun findBetween(
        startInclusive: LocalDate,
        endInclusive: LocalDate
    ): List<PaymentReconciliationDailySummary> =
        paymentReconciliationDailySummaryJpaRepository
            .findBySummaryDateBetweenOrderBySummaryDateAsc(startInclusive, endInclusive)
            .map { it.toDomain() }

    override fun clear() {
        paymentReconciliationDailySummaryJpaRepository.deleteAllInBatch()
    }
}

private fun PaymentReconciliationDailySummary.toEntity(): PaymentReconciliationDailySummaryJpaEntity =
    PaymentReconciliationDailySummaryJpaEntity(
        summaryDate = summaryDate,
        bookingCreatedCount = bookingCreatedCount,
        authorizedCount = authorizedCount,
        capturedCount = capturedCount,
        refundPendingCount = refundPendingCount,
        refundedCount = refundedCount,
        noRefundCount = noRefundCount,
        refundFailedRetryableCount = refundFailedRetryableCount,
        refundReviewRequiredCount = refundReviewRequiredCount,
        refreshedAtUtc = refreshedAtUtc
    )

private fun PaymentReconciliationDailySummaryJpaEntity.toDomain(): PaymentReconciliationDailySummary =
    PaymentReconciliationDailySummary(
        summaryDate = summaryDate,
        bookingCreatedCount = bookingCreatedCount,
        authorizedCount = authorizedCount,
        capturedCount = capturedCount,
        refundPendingCount = refundPendingCount,
        refundedCount = refundedCount,
        noRefundCount = noRefundCount,
        refundFailedRetryableCount = refundFailedRetryableCount,
        refundReviewRequiredCount = refundReviewRequiredCount,
        refreshedAtUtc = refreshedAtUtc
    )
