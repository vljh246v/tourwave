package com.demo.tourwave.application.customer

import com.demo.tourwave.adapter.out.persistence.booking.InMemoryBookingRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.customer.FakeEmailNotificationChannelAdapter
import com.demo.tourwave.adapter.out.persistence.customer.InMemoryNotificationDeliveryRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.customer.InMemoryNotificationRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.inquiry.InMemoryInquiryRepositoryAdapter
import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.customer.port.NotificationDeliveryRepository
import com.demo.tourwave.application.customer.port.NotificationRepository
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.inquiry.Inquiry
import com.demo.tourwave.support.FakeUserRepository
import com.demo.tourwave.domain.user.User
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NotificationServiceTest {
    private val notificationRepository: NotificationRepository = InMemoryNotificationRepositoryAdapter()
    private val notificationDeliveryRepository: NotificationDeliveryRepository = InMemoryNotificationDeliveryRepositoryAdapter()
    private val bookingRepository: BookingRepository = InMemoryBookingRepositoryAdapter()
    private val inquiryRepository = InMemoryInquiryRepositoryAdapter()
    private val userRepository = FakeUserRepository()
    private val paymentRecordRepository = object : PaymentRecordRepository {
        override fun save(record: com.demo.tourwave.domain.payment.PaymentRecord) = record
        override fun findByBookingId(bookingId: Long) = null
        override fun findByStatuses(statuses: Set<com.demo.tourwave.domain.payment.PaymentRecordStatus>) = emptyList<com.demo.tourwave.domain.payment.PaymentRecord>()
        override fun findUpdatedBetween(startInclusive: Instant, endExclusive: Instant) = emptyList<com.demo.tourwave.domain.payment.PaymentRecord>()
        override fun findAll() = emptyList<com.demo.tourwave.domain.payment.PaymentRecord>()
        override fun clear() = Unit
    }
    private val service = NotificationService(
        notificationRepository = notificationRepository,
        notificationDeliveryService = NotificationDeliveryService(
            notificationDeliveryRepository = notificationDeliveryRepository,
            notificationChannelPort = FakeEmailNotificationChannelAdapter(),
            clock = Clock.fixed(Instant.parse("2026-03-18T10:00:00Z"), ZoneOffset.UTC)
        ),
        bookingRepository = bookingRepository,
        inquiryRepository = inquiryRepository,
        paymentRecordRepository = paymentRecordRepository,
        userRepository = userRepository,
        notificationTemplateFactory = NotificationTemplateFactory(),
        clock = Clock.fixed(Instant.parse("2026-03-18T10:00:00Z"), ZoneOffset.UTC)
    )

    @Test
    fun `booking and inquiry audit events accumulate notifications and can be marked read`() {
        userRepository.save(
            User.create(
                displayName = "Customer",
                email = "customer@test.com",
                passwordHash = "hash",
                now = Instant.parse("2026-03-18T00:00:00Z")
            ).copy(id = 100L)
        )
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 30L,
                organizationId = 1L,
                leaderUserId = 100L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-18T00:00:00Z")
            )
        )
        val inquiry = inquiryRepository.save(
            Inquiry(
                organizationId = 1L,
                occurrenceId = 30L,
                bookingId = requireNotNull(booking.id),
                createdByUserId = 100L,
                subject = "Pickup",
                createdAt = Instant.parse("2026-03-18T01:00:00Z")
            )
        )

        service.handle(
            AuditEventCommand(
                actor = "USER:100",
                action = "BOOKING_CREATED",
                resourceType = "BOOKING",
                resourceId = requireNotNull(booking.id),
                occurredAtUtc = Instant.parse("2026-03-18T02:00:00Z")
            )
        )
        service.handle(
            AuditEventCommand(
                actor = "USER:200",
                action = "INQUIRY_CREATED",
                resourceType = "INQUIRY",
                resourceId = requireNotNull(inquiry.id),
                occurredAtUtc = Instant.parse("2026-03-18T03:00:00Z")
            )
        )

        val notifications = service.list(100L)
        val marked = service.markRead(100L, requireNotNull(notifications.first().id))

        assertEquals(2, notifications.size)
        assertNotNull(marked.readAt)
        assertEquals(2, service.markAllRead(100L).size)
        assertEquals(2, notificationDeliveryRepository.findAll().size)
        assertEquals("SENT", notificationDeliveryRepository.findAll().first().status.name)
    }
}
