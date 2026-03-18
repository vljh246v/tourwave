package com.demo.tourwave.adapter.`in`.job

import com.demo.tourwave.application.booking.RefundRetryJobResult
import com.demo.tourwave.application.booking.RefundRetryService
import com.demo.tourwave.application.common.ScheduledJobCoordinator
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "tourwave.jobs.refund-retry",
    name = ["enabled"],
    havingValue = "true"
)
class RefundRetryJob(
    private val refundRetryService: RefundRetryService,
    private val scheduledJobCoordinator: ScheduledJobCoordinator
) {
    @Scheduled(fixedDelayString = "\${tourwave.jobs.refund-retry.fixed-delay-ms:60000}")
    fun run(): RefundRetryJobResult =
        scheduledJobCoordinator.run(
            jobName = "refund-retry",
            onSkipped = {
                RefundRetryJobResult(
                    scannedCount = 0,
                    eligibleCount = 0,
                    refundedCount = 0,
                    reviewRequiredCount = 0,
                    stillRetryableCount = 0
                )
            }
        ) {
            refundRetryService.retryPendingRefunds()
        }
}
