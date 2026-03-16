package com.demo.tourwave.application.inquiry

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.inquiry.port.InquiryRepository
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.inquiry.Inquiry
import com.demo.tourwave.domain.inquiry.InquiryMessage
import com.demo.tourwave.domain.inquiry.InquiryStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.assertEquals

class InquiryQueryServiceTest {
    private val bookingRepository = mock(BookingRepository::class.java)
    private val inquiryRepository = mock(InquiryRepository::class.java)
    private val inquiryAccessPolicy = InquiryAccessPolicy(bookingRepository)
    private val inquiryQueryService = InquiryQueryService(
        inquiryRepository = inquiryRepository,
        inquiryAccessPolicy = inquiryAccessPolicy
    )

    @Test
    fun `list messages applies cursor and limit`() {
        val inquiry = Inquiry(
            id = 81L,
            organizationId = 31L,
            occurrenceId = 901L,
            bookingId = 71L,
            createdByUserId = 101L,
            createdAt = Instant.parse("2026-03-10T00:00:00Z")
        )
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

        whenever(inquiryRepository.findById(81L)).thenReturn(inquiry)
        whenever(bookingRepository.findById(71L)).thenReturn(booking)
        whenever(inquiryRepository.findMessagesByInquiryId(81L)).thenReturn(
            listOf(
                InquiryMessage(id = 1L, inquiryId = 81L, senderUserId = 101L, body = "a", createdAt = Instant.parse("2026-03-10T00:01:00Z")),
                InquiryMessage(id = 2L, inquiryId = 81L, senderUserId = 101L, body = "b", createdAt = Instant.parse("2026-03-10T00:02:00Z")),
                InquiryMessage(id = 3L, inquiryId = 81L, senderUserId = 101L, body = "c", createdAt = Instant.parse("2026-03-10T00:03:00Z"))
            )
        )

        val result = inquiryQueryService.listMessages(
            ListInquiryMessagesQuery(
                inquiryId = 81L,
                actor = InquiryActorContext(actorUserId = 101L),
                cursor = "1",
                limit = 1
            )
        )

        assertEquals(1, result.items.size)
        assertEquals(2L, result.items.single().id)
        assertEquals("3", result.nextCursor)
    }

    @Test
    fun `list my inquiries rejects invalid cursor`() {
        val exception = assertThrows<DomainException> {
            inquiryQueryService.listMyInquiries(
                ListMyInquiriesQuery(
                    actorUserId = 101L,
                    status = InquiryStatus.OPEN,
                    cursor = "bad-cursor",
                    limit = 20
                )
            )
        }

        assertEquals(422, exception.status)
    }
}
