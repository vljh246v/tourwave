package com.demo.tourwave.adapter.out.persistence.jpa.payment

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface PaymentReconciliationDailySummaryJpaRepository :
    JpaRepository<PaymentReconciliationDailySummaryJpaEntity, LocalDate> {
    fun findBySummaryDateBetweenOrderBySummaryDateAsc(
        startInclusive: LocalDate,
        endInclusive: LocalDate,
    ): List<PaymentReconciliationDailySummaryJpaEntity>
}
