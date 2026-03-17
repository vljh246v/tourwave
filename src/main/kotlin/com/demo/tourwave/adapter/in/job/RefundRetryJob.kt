package com.demo.tourwave.adapter.`in`.job

import com.demo.tourwave.application.booking.RefundRetryJobResult
import com.demo.tourwave.application.booking.RefundRetryService
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
    private val refundRetryService: RefundRetryService
) {
    @Scheduled(fixedDelayString = "\${tourwave.jobs.refund-retry.fixed-delay-ms:60000}")
    fun run(): RefundRetryJobResult = refundRetryService.retryPendingRefunds()
}
