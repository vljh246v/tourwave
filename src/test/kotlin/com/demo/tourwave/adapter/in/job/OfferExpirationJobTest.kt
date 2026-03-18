package com.demo.tourwave.adapter.`in`.job

import com.demo.tourwave.application.booking.OfferExpirationJobResult
import com.demo.tourwave.application.booking.OfferExpirationService
import com.demo.tourwave.application.common.ScheduledJobCoordinator
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class OfferExpirationJobTest {
    private val offerExpirationService = mock<OfferExpirationService>()
    private val scheduledJobCoordinator = mock<ScheduledJobCoordinator>()
    private val job = OfferExpirationJob(offerExpirationService, scheduledJobCoordinator)

    @Test
    fun `job delegates to offer expiration service`() {
        whenever(scheduledJobCoordinator.run(eq("offer-expiration"), any(), any<() -> OfferExpirationJobResult>()))
            .thenAnswer { invocation -> invocation.getArgument<() -> OfferExpirationJobResult>(2).invoke() }
        whenever(offerExpirationService.expireOffers()).thenReturn(
            OfferExpirationJobResult(expiredBookingIds = listOf(1L, 2L))
        )

        val result = job.run()

        verify(offerExpirationService).expireOffers()
        assertEquals(listOf(1L, 2L), result.expiredBookingIds)
    }
}
