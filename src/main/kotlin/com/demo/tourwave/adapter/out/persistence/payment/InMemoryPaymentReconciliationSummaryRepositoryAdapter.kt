package com.demo.tourwave.adapter.out.persistence.payment

import com.demo.tourwave.application.payment.port.PaymentReconciliationSummaryRepository
import com.demo.tourwave.domain.payment.PaymentReconciliationDailySummary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

@Repository
@Profile("!mysql & !mysql-test")
class InMemoryPaymentReconciliationSummaryRepositoryAdapter : PaymentReconciliationSummaryRepository {
    private val summaries = ConcurrentHashMap<LocalDate, PaymentReconciliationDailySummary>()

    override fun save(summary: PaymentReconciliationDailySummary): PaymentReconciliationDailySummary {
        summaries[summary.summaryDate] = summary
        return summary
    }

    override fun findByDate(summaryDate: LocalDate): PaymentReconciliationDailySummary? = summaries[summaryDate]

    override fun findBetween(
        startInclusive: LocalDate,
        endInclusive: LocalDate,
    ): List<PaymentReconciliationDailySummary> {
        return summaries.values
            .filter { !it.summaryDate.isBefore(startInclusive) && !it.summaryDate.isAfter(endInclusive) }
            .sortedBy { it.summaryDate }
    }

    override fun clear() {
        summaries.clear()
    }
}
