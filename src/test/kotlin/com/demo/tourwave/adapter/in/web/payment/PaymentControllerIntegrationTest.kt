package com.demo.tourwave.adapter.`in`.web.payment

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.customer.port.NotificationDeliveryRepository
import com.demo.tourwave.application.operations.port.OperatorFailureRecordRepository
import com.demo.tourwave.application.payment.PaymentWebhookService
import com.demo.tourwave.application.payment.port.PaymentProviderEventRepository
import com.demo.tourwave.application.payment.port.PaymentReconciliationSummaryRepository
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.customer.NotificationChannel
import com.demo.tourwave.domain.customer.NotificationDelivery
import com.demo.tourwave.domain.customer.NotificationDeliveryStatus
import com.demo.tourwave.domain.payment.PaymentProviderEvent
import com.demo.tourwave.domain.payment.PaymentProviderEventStatus
import com.demo.tourwave.domain.payment.PaymentProviderEventType
import com.demo.tourwave.domain.payment.PaymentRecord
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    private lateinit var paymentRecordRepository: PaymentRecordRepository

    @Autowired
    private lateinit var paymentProviderEventRepository: PaymentProviderEventRepository

    @Autowired
    private lateinit var paymentReconciliationSummaryRepository: PaymentReconciliationSummaryRepository

    @Autowired
    private lateinit var paymentWebhookService: PaymentWebhookService

    @Autowired
    private lateinit var notificationDeliveryRepository: NotificationDeliveryRepository

    @Autowired
    private lateinit var operatorFailureRecordRepository: OperatorFailureRecordRepository

    @BeforeEach
    fun setUp() {
        operatorFailureRecordRepository.clear()
        notificationDeliveryRepository.clear()
        paymentReconciliationSummaryRepository.clear()
        paymentProviderEventRepository.clear()
        paymentRecordRepository.clear()
        bookingRepository.clear()
    }

    @Test
    fun `payment webhook refund ops and reconciliation endpoints work together`() {
        val booking =
            bookingRepository.save(
                Booking(
                    occurrenceId = 200L,
                    organizationId = 10L,
                    leaderUserId = 1000L,
                    partySize = 2,
                    status = BookingStatus.CANCELED,
                    paymentStatus = PaymentStatus.REFUND_PENDING,
                    createdAt = Instant.parse("2026-03-17T02:00:00Z"),
                ),
            )
        paymentRecordRepository.save(
            PaymentRecord(
                bookingId = requireNotNull(booking.id),
                status = PaymentRecordStatus.REFUND_FAILED_RETRYABLE,
                providerName = "stub-pay",
                providerPaymentKey = "pay-${booking.id}",
                lastRefundRequestId = "retry-${booking.id}",
                lastRefundReasonCode = "BOOKING_REJECTED",
                lastErrorCode = "TEMPORARY",
                refundRetryCount = 1,
                createdAtUtc = Instant.parse("2026-03-17T02:00:00Z"),
                updatedAtUtc = Instant.parse("2026-03-17T03:00:00Z"),
            ),
        )

        mockMvc.perform(
            get("/operator/payments/refunds/ops")
                .header("X-Actor-User-Id", 999L),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].bookingId").value(requireNotNull(booking.id)))
            .andExpect(jsonPath("$[0].reviewRequired").value(false))

        mockMvc.perform(
            post("/operator/payments/bookings/${booking.id}/refund-retry")
                .header("X-Actor-User-Id", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"action":"RETRY","reasonCode":"MANUAL_RETRY","note":"operator retry"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.recordStatus").value("REFUNDED"))
            .andExpect(jsonPath("$.lastRemediationAction").value("RETRY"))

        mockMvc.perform(
            post("/operator/finance/reconciliation/daily/2026-03-17/refresh")
                .header("X-Actor-User-Id", 999L),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.summaryDate").value("2026-03-17"))

        mockMvc.perform(
            get("/operator/finance/reconciliation/daily")
                .header("X-Actor-User-Id", 999L)
                .param("startDate", "2026-03-17")
                .param("endDate", "2026-03-17"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].bookingCreatedCount").value(1))

        mockMvc.perform(
            get("/operator/finance/reconciliation/daily/export")
                .header("X-Actor-User-Id", 999L)
                .param("startDate", "2026-03-17")
                .param("endDate", "2026-03-17"),
        )
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("providerCapturedCount")))

        mockMvc.perform(
            get("/operator/finance/reconciliation/mismatches")
                .header("X-Actor-User-Id", 999L)
                .param("startDate", "2026-03-17")
                .param("endDate", "2026-03-17"),
        )
            .andExpect(status().isOk)

        mockMvc.perform(
            get("/operator/finance/reconciliation/mismatches/export")
                .header("X-Actor-User-Id", 999L)
                .param("startDate", "2026-03-17")
                .param("endDate", "2026-03-17"),
        )
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("mismatchType")))
    }

    @Test
    fun `payment webhook endpoint verifies signature and deduplicates provider events`() {
        val booking =
            bookingRepository.save(
                Booking(
                    occurrenceId = 201L,
                    organizationId = 10L,
                    leaderUserId = 1001L,
                    partySize = 1,
                    status = BookingStatus.CONFIRMED,
                    paymentStatus = PaymentStatus.AUTHORIZED,
                    createdAt = Instant.parse("2026-03-17T02:00:00Z"),
                ),
            )
        paymentRecordRepository.save(
            PaymentRecord(
                bookingId = requireNotNull(booking.id),
                status = PaymentRecordStatus.AUTHORIZED,
                providerName = "stub-pay",
                providerPaymentKey = "pay-${booking.id}",
                providerAuthorizationId = "auth-${booking.id}",
                createdAtUtc = Instant.parse("2026-03-17T02:00:00Z"),
                updatedAtUtc = Instant.parse("2026-03-17T02:00:00Z"),
            ),
        )

        val body = """{"providerName":"stub-pay","providerEventId":"evt-int-1","eventType":"CAPTURED","bookingId":${booking.id},"providerCaptureId":"cap-int-1","providerReference":"capture-int-1","retryable":true}"""
        val signature = "current:${paymentWebhookService.expectedSignature(body, "current")}"

        mockMvc.perform(
            post("/payments/webhooks/provider")
                .header("X-Payment-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.duplicate").value(false))
            .andExpect(jsonPath("$.paymentRecordStatus").value("CAPTURED"))

        mockMvc.perform(
            post("/payments/webhooks/provider")
                .header("X-Payment-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.duplicate").value(true))
    }

    @Test
    fun `payment webhook endpoint records malformed payload`() {
        val body = """{"providerName":"stub-pay","providerEventId":"""

        mockMvc.perform(
            post("/payments/webhooks/provider")
                .header("X-Payment-Signature", "current:${paymentWebhookService.expectedSignature(body, "current")}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
    }

    @Test
    fun `operator remediation queue endpoint lists and resolves failures`() {
        val booking =
            bookingRepository.save(
                Booking(
                    occurrenceId = 202L,
                    organizationId = 10L,
                    leaderUserId = 1002L,
                    partySize = 1,
                    status = BookingStatus.CANCELED,
                    paymentStatus = PaymentStatus.REFUND_PENDING,
                    createdAt = Instant.parse("2026-03-18T00:00:00Z"),
                ),
            )
        paymentRecordRepository.save(
            PaymentRecord(
                bookingId = requireNotNull(booking.id),
                status = PaymentRecordStatus.REFUND_REVIEW_REQUIRED,
                lastErrorCode = "manual-review",
                createdAtUtc = Instant.parse("2026-03-18T00:00:00Z"),
                updatedAtUtc = Instant.parse("2026-03-18T00:05:00Z"),
            ),
        )
        val failedDelivery =
            notificationDeliveryRepository.save(
                NotificationDelivery(
                    channel = NotificationChannel.EMAIL,
                    templateCode = "booking-update",
                    recipient = "user@example.com",
                    subject = "Update",
                    body = "Delivery failed",
                    resourceType = "BOOKING",
                    resourceId = requireNotNull(booking.id),
                    status = NotificationDeliveryStatus.FAILED_PERMANENT,
                    attemptCount = 1,
                    lastError = "mailbox-unavailable",
                    createdAt = Instant.parse("2026-03-18T00:10:00Z"),
                    updatedAt = Instant.parse("2026-03-18T00:10:00Z"),
                ),
            )
        paymentProviderEventRepository.save(
            PaymentProviderEvent(
                providerName = "stub-pay",
                providerEventId = "evt-remediation-1",
                eventType = PaymentProviderEventType.CAPTURED,
                bookingId = requireNotNull(booking.id),
                payloadJson = """{"providerName":"stub-pay"}""",
                payloadSha256 = "queue-hash",
                status = PaymentProviderEventStatus.POISONED,
                note = "apply-failed",
                receivedAtUtc = Instant.parse("2026-03-18T00:20:00Z"),
                processedAtUtc = Instant.parse("2026-03-18T00:21:00Z"),
            ),
        )

        mockMvc.perform(
            get("/operator/operations/remediation-queue")
                .header("X-Actor-User-Id", 999L),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].sourceType").exists())
            .andExpect(jsonPath("$[?(@.sourceType=='NOTIFICATION_DELIVERY')]").isNotEmpty)

        mockMvc.perform(
            post("/operator/operations/remediation-queue/NOTIFICATION_DELIVERY/${requireNotNull(failedDelivery.id)}")
                .header("X-Actor-User-Id", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"action":"RESOLVE","note":"accepted as manual case"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.queueStatus").value("RESOLVED"))
            .andExpect(jsonPath("$.sourceType").value("NOTIFICATION_DELIVERY"))

        mockMvc.perform(
            get("/operator/operations/remediation-queue")
                .header("X-Actor-User-Id", 999L),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("NOTIFICATION_DELIVERY"))))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("REFUND")))
    }
}
