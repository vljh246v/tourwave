package com.demo.tourwave.adapter.`in`.job

import com.demo.tourwave.application.common.IdempotencyPurgeJobResult
import com.demo.tourwave.application.common.IdempotencyPurgeService
import com.demo.tourwave.application.common.ScheduledJobCoordinator
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class IdempotencyPurgeJobTest {
    private val idempotencyPurgeService = mock<IdempotencyPurgeService>()
    private val scheduledJobCoordinator = mock<ScheduledJobCoordinator>()
    private val job = IdempotencyPurgeJob(idempotencyPurgeService, scheduledJobCoordinator)

    @Test
    fun `job delegates to idempotency purge service through coordinator`() {
        whenever(scheduledJobCoordinator.run(eq("idempotency-purge"), any(), any<() -> IdempotencyPurgeJobResult>()))
            .thenAnswer { invocation -> invocation.getArgument<() -> IdempotencyPurgeJobResult>(2).invoke() }
        whenever(idempotencyPurgeService.purgeExpired()).thenReturn(IdempotencyPurgeJobResult(purgedCount = 3))

        val result = job.run()

        verify(idempotencyPurgeService).purgeExpired()
        assertEquals(3, result.purgedCount)
    }
}
