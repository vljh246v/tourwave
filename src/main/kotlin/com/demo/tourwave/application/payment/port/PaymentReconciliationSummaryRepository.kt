package com.demo.tourwave.application.payment.port

import com.demo.tourwave.domain.payment.PaymentReconciliationDailySummary
import java.time.LocalDate

interface PaymentReconciliationSummaryRepository {
    fun save(summary: PaymentReconciliationDailySummary): PaymentReconciliationDailySummary

    fun findByDate(summaryDate: LocalDate): PaymentReconciliationDailySummary?

    fun findBetween(
        startInclusive: LocalDate,
        endInclusive: LocalDate,
    ): List<PaymentReconciliationDailySummary>

    fun clear()
}
