package com.demo.tourwave.adapter.`in`.job

import com.demo.tourwave.application.common.ScheduledJobCoordinator
import com.demo.tourwave.application.payment.FinanceReconciliationJobResult
import com.demo.tourwave.application.payment.ReconciliationService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals

class FinanceReconciliationJobTest {
    private val reconciliationService = mock<ReconciliationService>()
    private val scheduledJobCoordinator = mock<ScheduledJobCoordinator>()
    private val clock = Clock.fixed(Instant.parse("2026-03-18T00:00:00Z"), ZoneOffset.UTC)
    private val job = FinanceReconciliationJob(reconciliationService, scheduledJobCoordinator, clock)

    @Test
    fun `job delegates to reconciliation service through coordinator`() {
        val expected =
            FinanceReconciliationJobResult(
                refreshedDate = LocalDate.parse("2026-03-17"),
                refreshedAtUtc = Instant.parse("2026-03-18T00:00:00Z"),
            )
        whenever(scheduledJobCoordinator.run(eq("finance-reconciliation"), any(), any<() -> FinanceReconciliationJobResult>()))
            .thenAnswer { invocation -> invocation.getArgument<() -> FinanceReconciliationJobResult>(2).invoke() }
        whenever(reconciliationService.refreshYesterday()).thenReturn(expected)

        val result = job.run()

        verify(reconciliationService).refreshYesterday()
        assertEquals(expected, result)
    }
}
