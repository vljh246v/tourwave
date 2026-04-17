package com.demo.tourwave.adapter.`in`.job

import com.demo.tourwave.application.common.ScheduledJobCoordinator
import com.demo.tourwave.application.payment.FinanceReconciliationJobResult
import com.demo.tourwave.application.payment.ReconciliationService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate

@Component
@ConditionalOnProperty(
    prefix = "tourwave.jobs.finance-reconciliation",
    name = ["enabled"],
    havingValue = "true",
)
class FinanceReconciliationJob(
    private val reconciliationService: ReconciliationService,
    private val scheduledJobCoordinator: ScheduledJobCoordinator,
    private val clock: Clock,
) {
    @Scheduled(fixedDelayString = "\${tourwave.jobs.finance-reconciliation.fixed-delay-ms:86400000}")
    fun run(): FinanceReconciliationJobResult =
        scheduledJobCoordinator.run(
            jobName = "finance-reconciliation",
            onSkipped = {
                FinanceReconciliationJobResult(
                    refreshedDate = LocalDate.now(clock).minusDays(1),
                    refreshedAtUtc = clock.instant(),
                )
            },
        ) {
            reconciliationService.refreshYesterday()
        }
}
