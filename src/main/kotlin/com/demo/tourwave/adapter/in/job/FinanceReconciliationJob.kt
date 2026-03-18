package com.demo.tourwave.adapter.`in`.job

import com.demo.tourwave.application.payment.FinanceReconciliationJobResult
import com.demo.tourwave.application.payment.ReconciliationService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "tourwave.jobs.finance-reconciliation",
    name = ["enabled"],
    havingValue = "true"
)
class FinanceReconciliationJob(
    private val reconciliationService: ReconciliationService
) {
    @Scheduled(fixedDelayString = "\${tourwave.jobs.finance-reconciliation.fixed-delay-ms:86400000}")
    fun run(): FinanceReconciliationJobResult = reconciliationService.refreshYesterday()
}
