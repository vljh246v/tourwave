package com.demo.tourwave.adapter.out.payment

import com.demo.tourwave.application.booking.port.RefundExecutionRequest
import com.demo.tourwave.application.booking.port.RefundExecutionResult
import com.demo.tourwave.application.payment.PaymentAuthorizationRequest
import com.demo.tourwave.application.payment.PaymentCaptureRequest
import com.demo.tourwave.domain.booking.RefundReasonCode
import com.demo.tourwave.domain.common.DomainException
import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class HttpPaymentProviderAdapterTest {
    private val server = HttpServer.create(InetSocketAddress(0), 0)
    private val objectMapper = ObjectMapper()

    @AfterEach
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun `authorize capture and refund map provider responses`() {
        server.createContext("/payments/authorize") { exchange ->
            respond(exchange, 200, """{"providerName":"tourwave-pay","paymentKey":"pay-1","authorizationId":"auth-1"}""")
        }
        server.createContext("/payments/pay-1/capture") { exchange ->
            respond(exchange, 200, """{"providerName":"tourwave-pay","providerReference":"capture-ref-1","captureId":"cap-1"}""")
        }
        server.createContext("/refunds") { exchange ->
            respond(exchange, 200, """{"refundReference":"refund-ref-1"}""")
        }
        server.start()
        val adapter = adapter()

        val authorization = adapter.authorize(PaymentAuthorizationRequest(bookingId = 1L, actorUserId = 2L, amount = 10000, currency = "KRW"))
        val capture = adapter.capture(PaymentCaptureRequest(bookingId = 1L, actorUserId = 2L, providerPaymentKey = "pay-1"))
        val refund =
            adapter.executeRefund(
                RefundExecutionRequest(
                    bookingId = 1L,
                    actorUserId = 2L,
                    refundRequestId = "refund-1",
                    reasonCode = RefundReasonCode.BOOKING_REJECTED,
                ),
            )

        assertEquals("pay-1", authorization.providerPaymentKey)
        assertEquals("cap-1", capture.captureId)
        assertEquals(RefundExecutionResult.Success("refund-ref-1"), refund)
    }

    @Test
    fun `provider failures map to domain safe results`() {
        server.createContext("/payments/authorize") { exchange ->
            respond(exchange, 422, """{"errorCode":"AMOUNT_INVALID","message":"amount invalid"}""")
        }
        server.createContext("/refunds") { exchange ->
            respond(exchange, 429, """{"errorCode":"RATE_LIMITED"}""")
        }
        server.start()
        val adapter = adapter()

        val exception =
            assertFailsWith<DomainException> {
                adapter.authorize(PaymentAuthorizationRequest(bookingId = 1L, actorUserId = 2L, amount = 10000, currency = "KRW"))
            }
        val refund =
            adapter.executeRefund(
                RefundExecutionRequest(
                    bookingId = 1L,
                    actorUserId = 2L,
                    refundRequestId = "refund-1",
                    reasonCode = RefundReasonCode.BOOKING_REJECTED,
                ),
            )

        assertEquals(422, exception.status)
        assertIs<RefundExecutionResult.RetryableFailure>(refund)
        assertEquals("RATE_LIMITED", refund.errorCode)
    }

    private fun adapter(): HttpPaymentProviderAdapter {
        return HttpPaymentProviderAdapter(
            baseUrl = "http://localhost:${server.address.port}",
            apiKey = "test-key",
            providerName = "tourwave-pay",
            objectMapper = objectMapper,
        )
    }

    private fun respond(
        exchange: HttpExchange,
        status: Int,
        body: String,
    ) {
        exchange.sendResponseHeaders(status, body.toByteArray().size.toLong())
        exchange.responseBody.use { it.write(body.toByteArray()) }
    }
}
