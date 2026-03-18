package com.demo.tourwave.adapter.`in`.web.payment

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.payment.PaymentWebhookService
import com.demo.tourwave.application.payment.port.PaymentProviderEventRepository
import com.demo.tourwave.application.payment.port.PaymentReconciliationSummaryRepository
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
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

    @BeforeEach
    fun setUp() {
        paymentReconciliationSummaryRepository.clear()
        paymentProviderEventRepository.clear()
        paymentRecordRepository.clear()
        bookingRepository.clear()
    }

    @Test
    fun `payment webhook refund ops and reconciliation endpoints work together`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 200L,
                organizationId = 10L,
                leaderUserId = 1000L,
                partySize = 2,
                status = BookingStatus.CANCELED,
                paymentStatus = PaymentStatus.REFUND_PENDING,
                createdAt = Instant.parse("2026-03-17T02:00:00Z")
            )
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
                updatedAtUtc = Instant.parse("2026-03-17T03:00:00Z")
            )
        )

        mockMvc.perform(
            get("/operator/payments/refunds/ops")
                .header("X-Actor-User-Id", 999L)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].bookingId").value(requireNotNull(booking.id)))

        mockMvc.perform(
            post("/operator/payments/bookings/${booking.id}/refund-retry")
                .header("X-Actor-User-Id", 999L)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.recordStatus").value("REFUNDED"))

        mockMvc.perform(
            post("/operator/finance/reconciliation/daily/2026-03-17/refresh")
                .header("X-Actor-User-Id", 999L)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.summaryDate").value("2026-03-17"))

        mockMvc.perform(
            get("/operator/finance/reconciliation/daily")
                .header("X-Actor-User-Id", 999L)
                .param("startDate", "2026-03-17")
                .param("endDate", "2026-03-17")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].bookingCreatedCount").value(1))

        mockMvc.perform(
            get("/operator/finance/reconciliation/daily/export")
                .header("X-Actor-User-Id", 999L)
                .param("startDate", "2026-03-17")
                .param("endDate", "2026-03-17")
        )
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("date,bookingCreatedCount")))
    }

    @Test
    fun `payment webhook endpoint verifies signature and deduplicates provider events`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 201L,
                organizationId = 10L,
                leaderUserId = 1001L,
                partySize = 1,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-17T02:00:00Z")
            )
        )
        paymentRecordRepository.save(
            PaymentRecord(
                bookingId = requireNotNull(booking.id),
                status = PaymentRecordStatus.AUTHORIZED,
                providerName = "stub-pay",
                providerPaymentKey = "pay-${booking.id}",
                providerAuthorizationId = "auth-${booking.id}",
                createdAtUtc = Instant.parse("2026-03-17T02:00:00Z"),
                updatedAtUtc = Instant.parse("2026-03-17T02:00:00Z")
            )
        )

        val body = """{"providerName":"stub-pay","providerEventId":"evt-int-1","eventType":"CAPTURED","bookingId":${booking.id},"providerCaptureId":"cap-int-1","providerReference":"capture-int-1","retryable":true}"""
        val signature = paymentWebhookService.expectedSignature(body)

        mockMvc.perform(
            post("/payments/webhooks/provider")
                .header("X-Payment-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.duplicate").value(false))
            .andExpect(jsonPath("$.paymentRecordStatus").value("CAPTURED"))

        mockMvc.perform(
            post("/payments/webhooks/provider")
                .header("X-Payment-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.duplicate").value(true))
    }
}
