package com.demo.tourwave.adapter.out.persistence.jpa

import com.demo.tourwave.TourwaveApplication
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.common.port.IdempotencyDecision
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.application.inquiry.port.InquiryRepository
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.application.review.port.ReviewRepository
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.booking.AttendanceStatus
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.inquiry.Inquiry
import com.demo.tourwave.domain.inquiry.InquiryMessage
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.participant.BookingParticipant
import com.demo.tourwave.domain.participant.BookingParticipantStatus
import com.demo.tourwave.domain.payment.PaymentRecord
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import com.demo.tourwave.domain.review.Review
import com.demo.tourwave.domain.review.ReviewType
import com.demo.tourwave.domain.user.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(classes = [TourwaveApplication::class])
@ActiveProfiles("mysql-test")
class MysqlPersistenceIntegrationTest {
    @Autowired
    private lateinit var occurrenceRepository: OccurrenceRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    private lateinit var paymentRecordRepository: PaymentRecordRepository

    @Autowired
    private lateinit var bookingParticipantRepository: BookingParticipantRepository

    @Autowired
    private lateinit var inquiryRepository: InquiryRepository

    @Autowired
    private lateinit var reviewRepository: ReviewRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var idempotencyStore: IdempotencyStore

    @BeforeEach
    fun setUp() {
        reviewRepository.clear()
        inquiryRepository.clear()
        bookingParticipantRepository.clear()
        paymentRecordRepository.clear()
        bookingRepository.clear()
        occurrenceRepository.clear()
        userRepository.clear()
        idempotencyStore.clear()
    }

    @Test
    fun `mysql adapters persist booking aggregate and related views`() {
        occurrenceRepository.save(
            Occurrence(
                id = 9101L,
                organizationId = 31L,
                capacity = 10,
                startsAtUtc = Instant.parse("2026-03-20T09:00:00Z"),
                timezone = "Asia/Seoul"
            )
        )
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 9101L,
                organizationId = 31L,
                leaderUserId = 501L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-12T00:00:00Z")
            )
        )
        val bookingId = requireNotNull(booking.id)
        paymentRecordRepository.save(
            PaymentRecord(
                bookingId = bookingId,
                status = PaymentRecordStatus.CAPTURED,
                createdAtUtc = Instant.parse("2026-03-12T00:00:00Z"),
                updatedAtUtc = Instant.parse("2026-03-12T00:05:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = bookingId,
                userId = 501L,
                createdAt = Instant.parse("2026-03-12T00:00:00Z")
            ).recordAttendance(AttendanceStatus.ATTENDED)
        )
        reviewRepository.save(
            Review(
                occurrenceId = 9101L,
                reviewerUserId = 501L,
                type = ReviewType.TOUR,
                rating = 5,
                comment = "great",
                createdAt = Instant.parse("2026-03-21T00:00:00Z")
            )
        )

        val persisted = bookingRepository.findById(bookingId)
        val payment = paymentRecordRepository.findByBookingId(bookingId)
        val participants = bookingParticipantRepository.findByBookingId(bookingId)
        val reviews = reviewRepository.findByOccurrenceAndType(9101L, ReviewType.TOUR)

        assertNotNull(persisted)
        assertEquals(BookingStatus.CONFIRMED, persisted.status)
        assertEquals(PaymentRecordStatus.CAPTURED, payment?.status)
        assertEquals(1, participants.size)
        assertEquals(AttendanceStatus.ATTENDED, participants.single().attendanceStatus)
        assertEquals(1, reviews.size)
    }

    @Test
    fun `mysql adapters persist inquiry messages users and idempotency replay`() {
        val user = userRepository.save(User.create(name = "Jae", email = "JAE@EXAMPLE.COM"))
        assertEquals("jae@example.com", user.email)

        val inquiry = inquiryRepository.save(
            Inquiry(
                organizationId = 31L,
                occurrenceId = 9102L,
                bookingId = 9202L,
                createdByUserId = requireNotNull(user.id),
                subject = "Need pickup",
                createdAt = Instant.parse("2026-03-10T10:00:00Z")
            )
        )
        inquiryRepository.saveMessage(
            InquiryMessage(
                inquiryId = requireNotNull(inquiry.id),
                senderUserId = requireNotNull(user.id),
                body = "hello",
                attachmentAssetIds = listOf(11L, 12L),
                createdAt = Instant.parse("2026-03-10T10:01:00Z")
            )
        )

        val reserved = idempotencyStore.reserveOrReplay(1L, "POST", "/bookings/{bookingId}/cancel", "idem-1", "abc123")
        assertTrue(reserved is IdempotencyDecision.Reserved)
        idempotencyStore.complete(1L, "POST", "/bookings/{bookingId}/cancel", "idem-1", 204, mapOf("ok" to true))
        val replay = idempotencyStore.reserveOrReplay(1L, "POST", "/bookings/{bookingId}/cancel", "idem-1", "abc123")

        val messages = inquiryRepository.findMessagesByInquiryId(requireNotNull(inquiry.id))
        assertEquals(1, messages.size)
        assertEquals(listOf(11L, 12L), messages.single().attachmentAssetIds)
        assertTrue(replay is IdempotencyDecision.Replay)
        assertEquals(204, replay.status)
    }
}
