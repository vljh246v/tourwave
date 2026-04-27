package com.demo.tourwave.application.operations

import com.demo.tourwave.adapter.out.payment.InMemoryRefundExecutionAdapter
import com.demo.tourwave.adapter.out.persistence.audit.InMemoryAuditEventAdapter
import com.demo.tourwave.adapter.out.persistence.booking.InMemoryBookingRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.customer.InMemoryNotificationDeliveryRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.idempotency.InMemoryIdempotencyStoreAdapter
import com.demo.tourwave.adapter.out.persistence.operations.InMemoryOperatorFailureRecordRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.payment.InMemoryPaymentProviderEventRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.payment.InMemoryPaymentRecordRepositoryAdapter
import com.demo.tourwave.application.booking.PaymentLedgerService
import com.demo.tourwave.application.booking.RefundRetryService
import com.demo.tourwave.application.customer.DeliverNotificationCommand
import com.demo.tourwave.application.customer.NotificationDeliveryService
import com.demo.tourwave.application.customer.port.NotificationChannelException
import com.demo.tourwave.application.customer.port.NotificationChannelPort
import com.demo.tourwave.application.customer.port.NotificationChannelSendResult
import com.demo.tourwave.application.customer.port.SendNotificationMessage
import com.demo.tourwave.application.payment.PaymentWebhookService
import com.demo.tourwave.application.payment.RefundOperationsService
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.customer.NotificationChannel
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.operations.OperatorFailureSourceType
import com.demo.tourwave.domain.payment.PaymentProviderEvent
import com.demo.tourwave.domain.payment.PaymentProviderEventStatus
import com.demo.tourwave.domain.payment.PaymentProviderEventType
import com.demo.tourwave.domain.payment.PaymentRecord
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OperatorRemediationQueueServiceTest {
    private val bookingRepository = InMemoryBookingRepositoryAdapter()
    private val paymentRecordRepository = InMemoryPaymentRecordRepositoryAdapter()
    private val paymentProviderEventRepository = InMemoryPaymentProviderEventRepositoryAdapter()
    private val notificationDeliveryRepository = InMemoryNotificationDeliveryRepositoryAdapter()
    private val operatorFailureRecordRepository = InMemoryOperatorFailureRecordRepositoryAdapter()
    private val auditEventAdapter = InMemoryAuditEventAdapter()
    private val idempotencyStore = InMemoryIdempotencyStoreAdapter()
    private val refundExecutionAdapter = InMemoryRefundExecutionAdapter()
    private val notificationChannelPort = ScriptedNotificationChannelPort()
    private val clock = Clock.fixed(Instant.parse("2026-03-19T01:00:00Z"), ZoneOffset.UTC)
    private val paymentLedgerService =
        PaymentLedgerService(
            paymentRecordRepository = paymentRecordRepository,
            paymentProviderPort = refundExecutionAdapter,
            refundExecutionPort = refundExecutionAdapter,
            clock = clock,
        )
    private val refundRetryService =
        RefundRetryService(
            paymentRecordRepository = paymentRecordRepository,
            bookingRepository = bookingRepository,
            refundExecutionPort = refundExecutionAdapter,
            auditEventPort = auditEventAdapter,
            maxRetryAttempts = 5,
            retryCooldown = Duration.ZERO,
            clock = clock,
        )
    private val refundOperationsService =
        RefundOperationsService(
            paymentRecordRepository = paymentRecordRepository,
            bookingRepository = bookingRepository,
            refundRetryService = refundRetryService,
            auditEventPort = auditEventAdapter,
            maxRetryAttempts = 5,
            retryCooldown = Duration.ZERO,
            clock = clock,
        )
    private val notificationDeliveryService =
        NotificationDeliveryService(
            notificationDeliveryRepository = notificationDeliveryRepository,
            notificationChannelPort = notificationChannelPort,
            clock = clock,
        )
    private val paymentWebhookService =
        PaymentWebhookService(
            paymentProviderEventRepository = paymentProviderEventRepository,
            bookingRepository = bookingRepository,
            paymentLedgerService = paymentLedgerService,
            webhookSecrets = listOf("current:webhook-secret"),
            clock = clock,
        )
    private val service =
        OperatorRemediationQueueService(
            paymentRecordRepository = paymentRecordRepository,
            refundOperationsService = refundOperationsService,
            notificationDeliveryRepository = notificationDeliveryRepository,
            notificationDeliveryService = notificationDeliveryService,
            paymentProviderEventRepository = paymentProviderEventRepository,
            paymentWebhookService = paymentWebhookService,
            operatorFailureRecordRepository = operatorFailureRecordRepository,
            auditEventPort = auditEventAdapter,
            idempotencyStore = idempotencyStore,
            clock = clock,
        )

    @Test
    fun `queue lists refund notification and webhook failures and hides resolved items`() {
        val booking = createBooking(501L)
        paymentRecordRepository.save(
            PaymentRecord(
                bookingId = requireNotNull(booking.id),
                status = PaymentRecordStatus.REFUND_REVIEW_REQUIRED,
                lastErrorCode = "manual-review",
                createdAtUtc = Instant.parse("2026-03-19T00:00:00Z"),
                updatedAtUtc = Instant.parse("2026-03-19T00:10:00Z"),
            ),
        )
        val failedNotification =
            notificationDeliveryRepository.save(
                com.demo.tourwave.domain.customer.NotificationDelivery(
                    channel = NotificationChannel.EMAIL,
                    templateCode = "booking-refund",
                    recipient = "ops@example.com",
                    subject = "Refund failed",
                    body = "retry later",
                    resourceType = "BOOKING",
                    resourceId = requireNotNull(booking.id),
                    status = com.demo.tourwave.domain.customer.NotificationDeliveryStatus.FAILED_PERMANENT,
                    attemptCount = 1,
                    lastError = "smtp-bounce",
                    createdAt = Instant.parse("2026-03-19T00:05:00Z"),
                    updatedAt = Instant.parse("2026-03-19T00:15:00Z"),
                ),
            )
        paymentProviderEventRepository.save(
            PaymentProviderEvent(
                providerName = "stub-pay",
                providerEventId = "evt-poison",
                eventType = PaymentProviderEventType.CAPTURED,
                bookingId = requireNotNull(booking.id),
                payloadJson = """{"providerName":"stub-pay","providerEventId":"evt-poison"}""",
                payloadSha256 = "sha",
                status = PaymentProviderEventStatus.POISONED,
                note = "database-timeout",
                receivedAtUtc = Instant.parse("2026-03-19T00:20:00Z"),
                processedAtUtc = Instant.parse("2026-03-19T00:21:00Z"),
            ),
        )

        service.remediate(
            sourceType = OperatorFailureSourceType.NOTIFICATION_DELIVERY,
            sourceKey = requireNotNull(failedNotification.id).toString(),
            command =
                OperatorRemediationCommand(
                    actorUserId = 7L,
                    action = OperatorRemediationAction.RESOLVE,
                    note = "acknowledged",
                    idempotencyKey = "rem-resolve-001",
                ),
        )

        val items = service.listOpenItems()

        assertEquals(2, items.size)
        assertTrue(items.any { it.sourceType == OperatorFailureSourceType.REFUND })
        assertTrue(items.any { it.sourceType == OperatorFailureSourceType.PAYMENT_WEBHOOK })
        assertFalse(items.any { it.sourceType == OperatorFailureSourceType.NOTIFICATION_DELIVERY })
    }

    @Test
    fun `retry on notification failure redelivers and closes queue item`() {
        val delivery =
            notificationDeliveryService.deliver(
                DeliverNotificationCommand(
                    channel = NotificationChannel.EMAIL,
                    templateCode = "booking-created",
                    recipient = "user@example.com",
                    subject = "Booked",
                    body = "Booked",
                    resourceType = "BOOKING",
                    resourceId = 900L,
                    idempotencyKey = "notify-900",
                ),
            )
        assertEquals(com.demo.tourwave.domain.customer.NotificationDeliveryStatus.FAILED_RETRYABLE, delivery.status)

        notificationChannelPort.failuresBeforeSuccess.set(0)
        service.remediate(
            sourceType = OperatorFailureSourceType.NOTIFICATION_DELIVERY,
            sourceKey = requireNotNull(delivery.id).toString(),
            command =
                OperatorRemediationCommand(
                    actorUserId = 11L,
                    action = OperatorRemediationAction.RETRY,
                    note = "replay email",
                    idempotencyKey = "rem-retry-notification-001",
                ),
        )

        val saved = notificationDeliveryRepository.findById(requireNotNull(delivery.id))
        assertEquals(com.demo.tourwave.domain.customer.NotificationDeliveryStatus.SENT, saved?.status)
        assertTrue(service.listOpenItems().none { it.sourceKey == requireNotNull(delivery.id).toString() })
        assertEquals("OPERATOR_FAILURE_RETRY", auditEventAdapter.all().last().action)
    }

    @Test
    fun `retry on poisoned webhook reprocesses payment record`() {
        val booking = createBooking(777L)
        paymentLedgerService.initialize(
            booking = booking,
            occurrence = Occurrence(id = 777L, organizationId = 1L, capacity = 10, unitPrice = 10000, currency = "KRW"),
            actorUserId = 1L,
        )
        paymentProviderEventRepository.save(
            PaymentProviderEvent(
                providerName = "stub-pay",
                providerEventId = "evt-poison-retry",
                eventType = PaymentProviderEventType.CAPTURED,
                bookingId = requireNotNull(booking.id),
                payloadJson = """{"providerName":"stub-pay","providerEventId":"evt-poison-retry"}""",
                payloadSha256 = "sha-2",
                status = PaymentProviderEventStatus.POISONED,
                note = "transient-write-failure",
                receivedAtUtc = Instant.parse("2026-03-19T00:40:00Z"),
                processedAtUtc = Instant.parse("2026-03-19T00:41:00Z"),
            ),
        )

        service.remediate(
            sourceType = OperatorFailureSourceType.PAYMENT_WEBHOOK,
            sourceKey = "evt-poison-retry",
            command =
                OperatorRemediationCommand(
                    actorUserId = 15L,
                    action = OperatorRemediationAction.RETRY,
                    note = "reprocess",
                    idempotencyKey = "rem-retry-webhook-001",
                ),
        )

        assertEquals(PaymentProviderEventStatus.PROCESSED, paymentProviderEventRepository.findByProviderEventId("evt-poison-retry")?.status)
        assertEquals(PaymentRecordStatus.CAPTURED, paymentRecordRepository.findByBookingId(requireNotNull(booking.id))?.status)
    }

    @Test
    fun `remediate with same idempotency key and same payload replays response`() {
        val delivery =
            notificationDeliveryService.deliver(
                DeliverNotificationCommand(
                    channel = NotificationChannel.EMAIL,
                    templateCode = "booking-created",
                    recipient = "user@example.com",
                    subject = "Booked",
                    body = "Booked",
                    resourceType = "BOOKING",
                    resourceId = 999L,
                    idempotencyKey = "notify-999",
                ),
            )
        notificationChannelPort.failuresBeforeSuccess.set(0)
        val cmd =
            OperatorRemediationCommand(
                actorUserId = 20L,
                action = OperatorRemediationAction.RETRY,
                note = "first-try",
                idempotencyKey = "rem-idem-replay-001",
            )

        val result1 =
            service.remediate(
                sourceType = OperatorFailureSourceType.NOTIFICATION_DELIVERY,
                sourceKey = requireNotNull(delivery.id).toString(),
                command = cmd,
            )
        // Second call with same key — item no longer in open queue, so retry would fail if not replayed
        val result2 =
            service.remediate(
                sourceType = OperatorFailureSourceType.NOTIFICATION_DELIVERY,
                sourceKey = requireNotNull(delivery.id).toString(),
                command = cmd,
            )
        assertEquals(result1, result2)
    }

    @Test
    fun `remediate with same idempotency key but different payload returns 422`() {
        val delivery =
            notificationDeliveryService.deliver(
                DeliverNotificationCommand(
                    channel = NotificationChannel.EMAIL,
                    templateCode = "booking-created",
                    recipient = "user2@example.com",
                    subject = "Booked",
                    body = "Booked",
                    resourceType = "BOOKING",
                    resourceId = 998L,
                    idempotencyKey = "notify-998",
                ),
            )
        notificationChannelPort.failuresBeforeSuccess.set(0)
        val key = "rem-idem-conflict-001"

        service.remediate(
            sourceType = OperatorFailureSourceType.NOTIFICATION_DELIVERY,
            sourceKey = requireNotNull(delivery.id).toString(),
            command = OperatorRemediationCommand(actorUserId = 21L, action = OperatorRemediationAction.RETRY, note = "first", idempotencyKey = key),
        )

        try {
            service.remediate(
                sourceType = OperatorFailureSourceType.NOTIFICATION_DELIVERY,
                sourceKey = requireNotNull(delivery.id).toString(),
                command =
                    OperatorRemediationCommand(
                        actorUserId = 21L,
                        action = OperatorRemediationAction.RETRY,
                        note = "different-note",
                        idempotencyKey = key,
                    ),
            )
            kotlin.test.fail("Expected DomainException for IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD")
        } catch (e: com.demo.tourwave.domain.common.DomainException) {
            assertEquals(com.demo.tourwave.domain.common.ErrorCode.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD, e.errorCode)
        }
    }

    private fun createBooking(occurrenceId: Long): Booking =
        bookingRepository.save(
            Booking(
                occurrenceId = occurrenceId,
                organizationId = 1L,
                leaderUserId = 1L,
                partySize = 1,
                status = BookingStatus.CANCELED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-18T00:00:00Z"),
            ),
        )

    private class ScriptedNotificationChannelPort : NotificationChannelPort {
        val failuresBeforeSuccess = AtomicInteger(1)

        override fun send(message: SendNotificationMessage): NotificationChannelSendResult {
            if (failuresBeforeSuccess.getAndUpdate { current -> if (current > 0) current - 1 else current } > 0) {
                throw NotificationChannelException("provider-timeout", retryable = true)
            }
            return NotificationChannelSendResult(providerMessageId = "msg-${message.idempotencyKey}")
        }
    }
}
