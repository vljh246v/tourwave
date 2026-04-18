package com.demo.tourwave.adapter.out.persistence.jpa

import com.demo.tourwave.TourwaveApplication
import com.demo.tourwave.application.customer.port.NotificationDeliveryRepository
import com.demo.tourwave.application.operations.port.OperatorFailureRecordRepository
import com.demo.tourwave.application.payment.port.PaymentProviderEventRepository
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.customer.NotificationChannel
import com.demo.tourwave.domain.customer.NotificationDelivery
import com.demo.tourwave.domain.customer.NotificationDeliveryStatus
import com.demo.tourwave.domain.operations.OperatorFailureAction
import com.demo.tourwave.domain.operations.OperatorFailureRecord
import com.demo.tourwave.domain.operations.OperatorFailureRecordStatus
import com.demo.tourwave.domain.operations.OperatorFailureSourceType
import com.demo.tourwave.domain.payment.PaymentProviderEvent
import com.demo.tourwave.domain.payment.PaymentProviderEventStatus
import com.demo.tourwave.domain.payment.PaymentProviderEventType
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.support.MysqlTestContainerSupport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest(classes = [TourwaveApplication::class])
@ActiveProfiles("mysql")
class RealMysqlContainerRegressionTest : MysqlTestContainerSupport() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var notificationDeliveryRepository: NotificationDeliveryRepository

    @Autowired
    private lateinit var paymentProviderEventRepository: PaymentProviderEventRepository

    @Autowired
    private lateinit var operatorFailureRecordRepository: OperatorFailureRecordRepository

    @BeforeEach
    fun setUp() {
        operatorFailureRecordRepository.clear()
        paymentProviderEventRepository.clear()
        notificationDeliveryRepository.clear()
        userRepository.clear()
    }

    @Test
    fun `real mysql container persists customer payment and remediation ops views`() {
        val user = userRepository.save(User.create(displayName = "Ops", email = "ops@example.com", passwordHash = "hashed"))
        val delivery =
            notificationDeliveryRepository.save(
                NotificationDelivery(
                    channel = NotificationChannel.EMAIL,
                    templateCode = "ops-alert",
                    recipient = "ops@example.com",
                    subject = "Alert",
                    body = "Notification failed",
                    resourceType = "BOOKING",
                    resourceId = 101L,
                    status = NotificationDeliveryStatus.FAILED_RETRYABLE,
                    attemptCount = 1,
                    lastError = "provider-timeout",
                    createdAt = Instant.parse("2026-03-19T01:00:00Z"),
                    updatedAt = Instant.parse("2026-03-19T01:01:00Z"),
                ),
            )
        val event =
            paymentProviderEventRepository.save(
                PaymentProviderEvent(
                    providerName = "stub-pay",
                    providerEventId = "evt-mysql-regression-1",
                    eventType = PaymentProviderEventType.REFUND_FAILED,
                    bookingId = 101L,
                    payloadJson = """{"providerName":"stub-pay"}""",
                    payloadSha256 = "mysql-regression-hash",
                    status = PaymentProviderEventStatus.POISONED,
                    note = "processing-failed",
                    receivedAtUtc = Instant.parse("2026-03-19T01:02:00Z"),
                    processedAtUtc = Instant.parse("2026-03-19T01:03:00Z"),
                ),
            )
        val remediation =
            operatorFailureRecordRepository.save(
                OperatorFailureRecord(
                    sourceType = OperatorFailureSourceType.PAYMENT_WEBHOOK,
                    sourceKey = "evt-mysql-regression-1",
                    status = OperatorFailureRecordStatus.OPEN,
                    lastAction = OperatorFailureAction.RETRY,
                    note = "initial replay attempted",
                    lastActionByUserId = requireNotNull(user.id),
                    lastActionAtUtc = Instant.parse("2026-03-19T01:04:00Z"),
                    retryCount = 1,
                    createdAtUtc = Instant.parse("2026-03-19T01:04:00Z"),
                    updatedAtUtc = Instant.parse("2026-03-19T01:04:00Z"),
                ),
            )

        assertNotNull(delivery.id)
        assertNotNull(event.id)
        assertNotNull(remediation.id)
        assertEquals(NotificationDeliveryStatus.FAILED_RETRYABLE, notificationDeliveryRepository.findById(requireNotNull(delivery.id))?.status)
        assertEquals(PaymentProviderEventStatus.POISONED, paymentProviderEventRepository.findByProviderEventId("evt-mysql-regression-1")?.status)
        assertEquals(1, operatorFailureRecordRepository.findAll().single().retryCount)
    }
}
