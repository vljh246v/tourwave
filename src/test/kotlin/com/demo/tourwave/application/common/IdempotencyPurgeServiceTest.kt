package com.demo.tourwave.application.common

import com.demo.tourwave.adapter.out.persistence.audit.InMemoryAuditEventAdapter
import com.demo.tourwave.application.common.port.IdempotencyMaintenancePort
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class IdempotencyPurgeServiceTest {
    @Test
    fun `purge service returns purged count and appends audit`() {
        val audit = InMemoryAuditEventAdapter()
        val service =
            IdempotencyPurgeService(
                idempotencyMaintenancePort =
                    object : IdempotencyMaintenancePort {
                        override fun purgeExpired(nowEpochMillis: Long): Long = 3
                    },
                auditEventPort = audit,
                clock = Clock.fixed(Instant.parse("2026-03-17T00:00:00Z"), ZoneOffset.UTC),
            )

        val result = service.purgeExpired()

        assertEquals(3, result.purgedCount)
        assertEquals(1, audit.all().size)
        assertEquals("IDEMPOTENCY_PURGED", audit.all().single().action)
    }
}
