package com.demo.tourwave.adapter.`in`.job

import com.demo.tourwave.application.booking.RefundRetryJobResult
import com.demo.tourwave.application.booking.RefundRetryService
import com.demo.tourwave.application.common.ScheduledJobCoordinator
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class RefundRetryJobTest {
    private val refundRetryService = mock<RefundRetryService>()
    private val scheduledJobCoordinator = mock<ScheduledJobCoordinator>()
    private val job = RefundRetryJob(refundRetryService, scheduledJobCoordinator)

    @Test
    fun `job delegates to refund retry service through coordinator`() {
        val expected = RefundRetryJobResult(1, 1, 1, 0, 0)
        whenever(scheduledJobCoordinator.run(eq("refund-retry"), any(), any<() -> RefundRetryJobResult>()))
            .thenAnswer { invocation -> invocation.getArgument<() -> RefundRetryJobResult>(2).invoke() }
        whenever(refundRetryService.retryPendingRefunds()).thenReturn(expected)

        val result = job.run()

        verify(refundRetryService).retryPendingRefunds()
        assertEquals(expected, result)
    }
}
