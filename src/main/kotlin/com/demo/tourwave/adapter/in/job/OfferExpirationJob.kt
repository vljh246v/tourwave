package com.demo.tourwave.adapter.`in`.job

import com.demo.tourwave.application.booking.OfferExpirationJobResult
import com.demo.tourwave.application.booking.OfferExpirationService
import com.demo.tourwave.application.common.ScheduledJobCoordinator
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "tourwave.jobs.offer-expiration",
    name = ["enabled"],
    havingValue = "true"
)
class OfferExpirationJob(
    private val offerExpirationService: OfferExpirationService,
    private val scheduledJobCoordinator: ScheduledJobCoordinator
) {
    @Scheduled(fixedDelayString = "\${tourwave.jobs.offer-expiration.fixed-delay-ms:60000}")
    fun run(): OfferExpirationJobResult {
        return scheduledJobCoordinator.run(
            jobName = "offer-expiration",
            onSkipped = { OfferExpirationJobResult(expiredBookingIds = emptyList()) }
        ) {
            offerExpirationService.expireOffers()
        }
    }
}
