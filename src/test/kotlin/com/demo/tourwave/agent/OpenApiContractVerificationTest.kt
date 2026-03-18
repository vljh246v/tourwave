package com.demo.tourwave.agent

import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenApiContractVerificationTest {
    @Test
    fun `target openapi contract contains sprint 14 payment and finance paths`() {
        val spec = Yaml().load<Map<String, Any>>(Files.readString(Path.of("agent/04_openapi.yaml")))
        val paths = spec["paths"] as? Map<*, *>
        assertNotNull(paths)

        assertTrue(paths.containsKey("/payments/webhooks/provider"))
        assertTrue(paths.containsKey("/operator/payments/refunds/ops"))
        assertTrue(paths.containsKey("/operator/payments/bookings/{bookingId}/refund-retry"))
        assertTrue(paths.containsKey("/operator/finance/reconciliation/daily"))
        assertTrue(paths.containsKey("/operator/finance/reconciliation/daily/{summaryDate}/refresh"))
        assertTrue(paths.containsKey("/operator/finance/reconciliation/daily/export"))
    }

    @Test
    fun `target openapi contract contains sprint 18 announcement and reporting paths`() {
        val spec = Yaml().load<Map<String, Any>>(Files.readString(Path.of("agent/04_openapi.yaml")))
        val paths = spec["paths"] as? Map<*, *>
        assertNotNull(paths)

        assertTrue(paths.containsKey("/public/announcements"))
        assertTrue(paths.containsKey("/operator/organizations/{orgId}/announcements"))
        assertTrue(paths.containsKey("/organizations/{orgId}/announcements"))
        assertTrue(paths.containsKey("/announcements/{announcementId}"))
        assertTrue(paths.containsKey("/organizations/{orgId}/reports/bookings"))
        assertTrue(paths.containsKey("/organizations/{orgId}/reports/bookings/export"))
        assertTrue(paths.containsKey("/organizations/{orgId}/reports/occurrences"))
        assertTrue(paths.containsKey("/organizations/{orgId}/reports/occurrences/export"))
    }

    @Test
    fun `target openapi contract keeps bearer auth as default security`() {
        val spec = Yaml().load<Map<String, Any>>(Files.readString(Path.of("agent/04_openapi.yaml")))
        val security = spec["security"] as? List<*>
        assertNotNull(security)
        val first = security.firstOrNull() as? Map<*, *>
        assertNotNull(first)
        assertEquals(listOf<Any>(), first["bearerAuth"])
    }
}
