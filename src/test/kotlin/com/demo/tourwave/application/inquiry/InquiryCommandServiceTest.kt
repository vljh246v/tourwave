package com.demo.tourwave.application.inquiry

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.common.port.IdempotencyDecision
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.application.inquiry.port.InquiryRepository
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.inquiry.Inquiry
import com.demo.tourwave.domain.inquiry.InquiryMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class InquiryCommandServiceTest {
    private val bookingRepository = mock(BookingRepository::class.java)
    private val inquiryRepository = mock(InquiryRepository::class.java)
    private val inquiryAccessPolicy = InquiryAccessPolicy(bookingRepository)
    private val idempotencyStore = mock(IdempotencyStore::class.java)
    private val auditEventPort = mock(AuditEventPort::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-03-16T12:00:00Z"), ZoneOffset.UTC)

    private val inquiryCommandService = InquiryCommandService(
        bookingRepository = bookingRepository,
        inquiryRepository = inquiryRepository,
        inquiryAccessPolicy = inquiryAccessPolicy,
        idempotencyStore = idempotencyStore,
        auditEventPort = auditEventPort,
        clock = clock
    )

    @Test
    fun `create inquiry persists initial message when inquiry is created`() {
        val booking = Booking(
            id = 71L,
            occurrenceId = 901L,
            organizationId = 31L,
            leaderUserId = 101L,
            partySize = 2,
            status = BookingStatus.CONFIRMED,
            paymentStatus = PaymentStatus.PAID,
            createdAt = Instant.parse("2026-03-10T00:00:00Z")
        )
        whenever(
            idempotencyStore.reserveOrReplay(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(IdempotencyDecision.Reserved)
        whenever(bookingRepository.findById(71L)).thenReturn(booking)
        whenever(inquiryRepository.findByBookingId(71L)).thenReturn(null)
        whenever(inquiryRepository.save(org.mockito.kotlin.any<Inquiry>())).thenAnswer {
            it.getArgument<Inquiry>(0).copy(id = 501L)
        }
        whenever(inquiryRepository.saveMessage(org.mockito.kotlin.any<InquiryMessage>())).thenAnswer {
            it.getArgument(0)
        }

        inquiryCommandService.createInquiry(
            CreateInquiryCommand(
                occurrenceId = 901L,
                actorUserId = 101L,
                idempotencyKey = "inq-create-1",
                bookingId = 71L,
                subject = "좌석 문의",
                message = "  첫 메시지입니다.  "
            )
        )

        val messageCaptor = argumentCaptor<InquiryMessage>()
        verify(inquiryRepository, times(1)).saveMessage(messageCaptor.capture())
        assertEquals(501L, messageCaptor.firstValue.inquiryId)
        assertEquals(101L, messageCaptor.firstValue.senderUserId)
        assertEquals("첫 메시지입니다.", messageCaptor.firstValue.body)
    }

    @Test
    fun `create inquiry rejects blank initial message`() {
        val exception = assertThrows<DomainException> {
            inquiryCommandService.createInquiry(
                CreateInquiryCommand(
                    occurrenceId = 901L,
                    actorUserId = 101L,
                    idempotencyKey = "inq-create-2",
                    bookingId = 71L,
                    subject = "좌석 문의",
                    message = "   "
                )
            )
        }

        assertEquals(422, exception.status)
        verify(idempotencyStore, never()).reserveOrReplay(
            any(),
            any(),
            any(),
            any(),
            any()
        )
    }
}
