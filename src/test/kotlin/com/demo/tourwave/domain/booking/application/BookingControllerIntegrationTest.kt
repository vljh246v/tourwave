package com.demo.tourwave.domain.booking.application

import com.demo.tourwave.adapter.out.persistence.audit.InMemoryAuditEventAdapter
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.application.inquiry.port.InquiryRepository
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.application.review.port.ReviewRepository
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.inquiry.Inquiry
import com.demo.tourwave.domain.inquiry.InquiryStatus
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.occurrence.OccurrenceStatus
import com.demo.tourwave.domain.participant.BookingParticipant
import com.demo.tourwave.domain.participant.BookingParticipantStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.assertEquals

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    private lateinit var occurrenceRepository: OccurrenceRepository

    @Autowired
    private lateinit var idempotencyStore: IdempotencyStore

    @Autowired
    private lateinit var inquiryRepository: InquiryRepository

    @Autowired
    private lateinit var bookingParticipantRepository: BookingParticipantRepository

    @Autowired
    private lateinit var reviewRepository: ReviewRepository

    @Autowired
    private lateinit var auditEventAdapter: InMemoryAuditEventAdapter

    @BeforeEach
    fun setUp() {
        bookingRepository.clear()
        occurrenceRepository.clear()
        idempotencyStore.clear()
        inquiryRepository.clear()
        bookingParticipantRepository.clear()
        reviewRepository.clear()
        auditEventAdapter.clear()
    }

    @Test
    fun `create booking returns REQUESTED when available seats are enough`() {
        occurrenceRepository.save(Occurrence(id = 7001L, organizationId = 31L, capacity = 10))

        mockMvc.perform(
            post("/occurrences/7001/bookings")
                .header("Idempotency-Key", "idem-k-1")
                .header("X-Actor-User-Id", "101")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":2}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("REQUESTED"))
            .andExpect(jsonPath("$.paymentStatus").value("AUTHORIZED"))
            .andExpect(jsonPath("$.organizationId").value(31))
            .andExpect(jsonPath("$.userId").value(101))

        val participants = bookingParticipantRepository.findByBookingId(1L)
        kotlin.test.assertEquals(1, participants.size)
        kotlin.test.assertEquals(101L, participants.single().userId)
        kotlin.test.assertEquals("LEADER", participants.single().status.name)
    }

    @Test
    fun `create booking requires actor user header`() {
        occurrenceRepository.save(Occurrence(id = 70011L, organizationId = 31L, capacity = 10))

        mockMvc.perform(
            post("/occurrences/70011/bookings")
                .header("Idempotency-Key", "idem-authz-k-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":2}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
    }

    @Test
    fun `create booking returns WAITLISTED when available seats are insufficient`() {
        occurrenceRepository.save(Occurrence(id = 7002L, organizationId = 31L, capacity = 10))
        bookingRepository.save(
            Booking(
                occurrenceId = 7002L,
                organizationId = 31L,
                leaderUserId = 88L,
                partySize = 9,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )

        mockMvc.perform(
            post("/occurrences/7002/bookings")
                .header("Idempotency-Key", "idem-k-2")
                .header("X-Actor-User-Id", "102")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":2}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("WAITLISTED"))
    }

    @Test
    fun `same idempotency key and payload returns identical booking`() {
        occurrenceRepository.save(Occurrence(id = 7003L, organizationId = 31L, capacity = 10))

        val first = mockMvc.perform(
            post("/occurrences/7003/bookings")
                .header("Idempotency-Key", "idem-k-3")
                .header("X-Actor-User-Id", "103")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":2,"noteToOperator":"window"}""")
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        mockMvc.perform(
            post("/occurrences/7003/bookings")
                .header("Idempotency-Key", "idem-k-3")
                .header("X-Actor-User-Id", "103")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":2,"noteToOperator":"window"}""")
        )
            .andExpect(status().isCreated)
            .andExpect { result ->
                kotlin.test.assertEquals(first, result.response.contentAsString)
            }
    }

    @Test
    fun `same idempotency key with different payload returns 422 code`() {
        occurrenceRepository.save(Occurrence(id = 7004L, organizationId = 31L, capacity = 10))

        mockMvc.perform(
            post("/occurrences/7004/bookings")
                .header("Idempotency-Key", "idem-k-4")
                .header("X-Actor-User-Id", "104")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":2}""")
        )
            .andExpect(status().isCreated)

        mockMvc.perform(
            post("/occurrences/7004/bookings")
                .header("Idempotency-Key", "idem-k-4")
                .header("X-Actor-User-Id", "104")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":3}""")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD"))
    }

    @Test
    fun `in-progress idempotency key returns 409 code`() {
        occurrenceRepository.save(Occurrence(id = 7005L, organizationId = 31L, capacity = 10))
        idempotencyStore.markInProgressForTest(
            actorUserId = 105L,
            method = "POST",
            pathTemplate = "/occurrences/{occurrenceId}/bookings",
            idempotencyKey = "idem-k-5",
            requestHash = hash("7005|2|")
        )

        mockMvc.perform(
            post("/occurrences/7005/bookings")
                .header("Idempotency-Key", "idem-k-5")
                .header("X-Actor-User-Id", "105")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":2}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_IN_PROGRESS"))
    }

    @Test
    fun `idempotency scope uses actor user id`() {
        occurrenceRepository.save(Occurrence(id = 7006L, organizationId = 31L, capacity = 10))

        mockMvc.perform(
            post("/occurrences/7006/bookings")
                .header("Idempotency-Key", "idem-k-6")
                .header("X-Actor-User-Id", "106")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":2}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))

        mockMvc.perform(
            post("/occurrences/7006/bookings")
                .header("Idempotency-Key", "idem-k-6")
                .header("X-Actor-User-Id", "107")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":2}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(2))
    }

    @Test
    fun `partySize boundary violation returns 422 with code`() {
        occurrenceRepository.save(Occurrence(id = 7007L, organizationId = 31L, capacity = 10, status = OccurrenceStatus.SCHEDULED))

        mockMvc.perform(
            post("/occurrences/7007/bookings")
                .header("Idempotency-Key", "idem-k-7")
                .header("X-Actor-User-Id", "108")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":0}""")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("PARTY_SIZE_OUT_OF_RANGE"))
    }

    @Test
    fun `canceled occurrence rejects booking create with 409 code`() {
        occurrenceRepository.save(Occurrence(id = 7008L, organizationId = 31L, capacity = 10, status = OccurrenceStatus.CANCELED))

        mockMvc.perform(
            post("/occurrences/7008/bookings")
                .header("Idempotency-Key", "idem-k-8")
                .header("X-Actor-User-Id", "109")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":2}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("OCCURRENCE_ALREADY_CANCELED"))
    }

    @Test
    fun `approve booking returns 204 and updates booking status`() {
        val saved = bookingRepository.save(
            Booking(
                occurrenceId = 7101L,
                organizationId = 31L,
                leaderUserId = 501L,
                partySize = 2,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${saved.id}/approve")
                .header("Idempotency-Key", "approve-k-1")
                .header("X-Actor-User-Id", "701")
        )
            .andExpect(status().isNoContent)

        val updated = bookingRepository.findById(requireNotNull(saved.id))
        kotlin.test.assertEquals(BookingStatus.CONFIRMED, updated?.status)
        kotlin.test.assertEquals(PaymentStatus.PAID, updated?.paymentStatus)
    }

    @Test
    fun `approve booking on terminal state returns 409 with BOOKING_TERMINAL_STATE`() {
        val saved = bookingRepository.save(
            Booking(
                occurrenceId = 7102L,
                organizationId = 31L,
                leaderUserId = 502L,
                partySize = 2,
                status = BookingStatus.CANCELED,
                paymentStatus = PaymentStatus.REFUNDED,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${saved.id}/approve")
                .header("Idempotency-Key", "approve-k-2")
                .header("X-Actor-User-Id", "702")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("BOOKING_TERMINAL_STATE"))
    }

    @Test
    fun `approve booking returns 409 CAPACITY_EXCEEDED when seats are full`() {
        occurrenceRepository.save(Occurrence(id = 7103L, organizationId = 31L, capacity = 3))
        bookingRepository.save(
            Booking(
                occurrenceId = 7103L,
                organizationId = 31L,
                leaderUserId = 503L,
                partySize = 3,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        val saved = bookingRepository.save(
            Booking(
                occurrenceId = 7103L,
                organizationId = 31L,
                leaderUserId = 504L,
                partySize = 1,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T00:01:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${saved.id}/approve")
                .header("Idempotency-Key", "approve-k-3")
                .header("X-Actor-User-Id", "703")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("CAPACITY_EXCEEDED"))
    }

    @Test
    fun `reject booking returns 204 and updates to REJECTED`() {
        val saved = bookingRepository.save(
            Booking(
                occurrenceId = 7104L,
                organizationId = 31L,
                leaderUserId = 505L,
                partySize = 2,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${saved.id}/reject")
                .header("Idempotency-Key", "reject-k-1")
                .header("X-Actor-User-Id", "704")
        )
            .andExpect(status().isNoContent)

        val updated = bookingRepository.findById(requireNotNull(saved.id))
        kotlin.test.assertEquals(BookingStatus.REJECTED, updated?.status)
        kotlin.test.assertEquals(PaymentStatus.REFUNDED, updated?.paymentStatus)
    }

    @Test
    fun `cancel booking returns 204 and refunds payment`() {
        val saved = bookingRepository.save(
            Booking(
                occurrenceId = 7105L,
                organizationId = 31L,
                leaderUserId = 506L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${saved.id}/cancel")
                .header("Idempotency-Key", "cancel-k-1")
                .header("X-Actor-User-Id", "705")
        )
            .andExpect(status().isNoContent)

        val updated = bookingRepository.findById(requireNotNull(saved.id))
        kotlin.test.assertEquals(BookingStatus.CANCELED, updated?.status)
        kotlin.test.assertEquals(PaymentStatus.REFUNDED, updated?.paymentStatus)
    }

    @Test
    fun `accept offer returns 204 and updates to CONFIRMED when leader calls`() {
        occurrenceRepository.save(Occurrence(id = 7106L, organizationId = 31L, capacity = 10))
        val saved = bookingRepository.save(
            Booking(
                occurrenceId = 7106L,
                organizationId = 31L,
                leaderUserId = 507L,
                partySize = 2,
                status = BookingStatus.OFFERED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                offerExpiresAtUtc = Instant.now().plusSeconds(3600),
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${saved.id}/offer/accept")
                .header("Idempotency-Key", "offer-accept-k-1")
                .header("X-Actor-User-Id", "507")
        )
            .andExpect(status().isNoContent)

        val updated = bookingRepository.findById(requireNotNull(saved.id))
        kotlin.test.assertEquals(BookingStatus.CONFIRMED, updated?.status)
        kotlin.test.assertEquals(PaymentStatus.PAID, updated?.paymentStatus)
    }

    @Test
    fun `accept offer returns 409 OFFER_EXPIRED after expiry`() {
        val saved = bookingRepository.save(
            Booking(
                occurrenceId = 7107L,
                organizationId = 31L,
                leaderUserId = 508L,
                partySize = 2,
                status = BookingStatus.OFFERED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                offerExpiresAtUtc = Instant.now().minusSeconds(60),
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${saved.id}/offer/accept")
                .header("Idempotency-Key", "offer-accept-k-2")
                .header("X-Actor-User-Id", "508")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("OFFER_EXPIRED"))
    }

    @Test
    fun `decline offer returns 204 and updates to EXPIRED`() {
        val saved = bookingRepository.save(
            Booking(
                occurrenceId = 7108L,
                organizationId = 31L,
                leaderUserId = 509L,
                partySize = 2,
                status = BookingStatus.OFFERED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                offerExpiresAtUtc = Instant.now().plusSeconds(3600),
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${saved.id}/offer/decline")
                .header("Idempotency-Key", "offer-decline-k-1")
                .header("X-Actor-User-Id", "509")
        )
            .andExpect(status().isNoContent)

        val updated = bookingRepository.findById(requireNotNull(saved.id))
        kotlin.test.assertEquals(BookingStatus.EXPIRED, updated?.status)
        kotlin.test.assertEquals(PaymentStatus.REFUNDED, updated?.paymentStatus)
    }

    @Test
    fun `offer command by non-leader returns 422 with VALIDATION_ERROR`() {
        val saved = bookingRepository.save(
            Booking(
                occurrenceId = 7109L,
                organizationId = 31L,
                leaderUserId = 510L,
                partySize = 2,
                status = BookingStatus.OFFERED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                offerExpiresAtUtc = Instant.now().plusSeconds(3600),
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${saved.id}/offer/accept")
                .header("Idempotency-Key", "offer-accept-k-3")
                .header("X-Actor-User-Id", "999")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
    }

    @Test
    fun `same mutation idempotency key and payload returns 204 replay`() {
        val saved = bookingRepository.save(
            Booking(
                occurrenceId = 7110L,
                organizationId = 31L,
                leaderUserId = 511L,
                partySize = 2,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${saved.id}/approve")
                .header("Idempotency-Key", "approve-k-4")
                .header("X-Actor-User-Id", "711")
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/bookings/${saved.id}/approve")
                .header("Idempotency-Key", "approve-k-4")
                .header("X-Actor-User-Id", "711")
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `same mutation key with different target returns 422 idempotency error`() {
        val firstBooking = bookingRepository.save(
            Booking(
                occurrenceId = 7111L,
                organizationId = 31L,
                leaderUserId = 512L,
                partySize = 2,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        val secondBooking = bookingRepository.save(
            Booking(
                occurrenceId = 7111L,
                organizationId = 31L,
                leaderUserId = 513L,
                partySize = 2,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T00:01:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${firstBooking.id}/approve")
                .header("Idempotency-Key", "approve-k-5")
                .header("X-Actor-User-Id", "712")
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/bookings/${secondBooking.id}/approve")
                .header("Idempotency-Key", "approve-k-5")
                .header("X-Actor-User-Id", "712")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD"))
    }

    @Test
    fun `mutation in-progress idempotency key returns 409 code`() {
        val saved = bookingRepository.save(
            Booking(
                occurrenceId = 7112L,
                organizationId = 31L,
                leaderUserId = 514L,
                partySize = 2,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )

        idempotencyStore.markInProgressForTest(
            actorUserId = 713L,
            method = "POST",
            pathTemplate = "/bookings/{bookingId}/approve",
            idempotencyKey = "approve-k-6",
            requestHash = hash("${saved.id}|APPROVE|")
        )

        mockMvc.perform(
            post("/bookings/${saved.id}/approve")
                .header("Idempotency-Key", "approve-k-6")
                .header("X-Actor-User-Id", "713")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_IN_PROGRESS"))
    }

    @Test
    fun `occurrence cancel cascades non-terminal bookings and blocks offer flow`() {
        occurrenceRepository.save(Occurrence(id = 7301L, organizationId = 31L, capacity = 10, status = OccurrenceStatus.SCHEDULED))
        val requested = bookingRepository.save(
            Booking(
                occurrenceId = 7301L,
                organizationId = 31L,
                leaderUserId = 801L,
                partySize = 2,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        val offered = bookingRepository.save(
            Booking(
                occurrenceId = 7301L,
                organizationId = 31L,
                leaderUserId = 802L,
                partySize = 2,
                status = BookingStatus.OFFERED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                offerExpiresAtUtc = Instant.parse("2026-03-07T00:00:00Z"),
                createdAt = Instant.parse("2026-03-06T00:01:00Z")
            )
        )
        val expired = bookingRepository.save(
            Booking(
                occurrenceId = 7301L,
                organizationId = 31L,
                leaderUserId = 803L,
                partySize = 1,
                status = BookingStatus.EXPIRED,
                paymentStatus = PaymentStatus.REFUNDED,
                createdAt = Instant.parse("2026-03-06T00:02:00Z")
            )
        )

        mockMvc.perform(
            post("/occurrences/7301/cancel")
                .header("Idempotency-Key", "occ-cancel-k-1")
                .header("X-Actor-User-Id", "900")
        )
            .andExpect(status().isNoContent)

        kotlin.test.assertEquals(BookingStatus.CANCELED, bookingRepository.findById(requireNotNull(requested.id))?.status)
        kotlin.test.assertEquals(BookingStatus.CANCELED, bookingRepository.findById(requireNotNull(offered.id))?.status)
        kotlin.test.assertEquals(BookingStatus.EXPIRED, bookingRepository.findById(requireNotNull(expired.id))?.status)

        mockMvc.perform(
            post("/bookings/${offered.id}/offer/accept")
                .header("Idempotency-Key", "offer-after-occ-cancel")
                .header("X-Actor-User-Id", "802")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("OCCURRENCE_ALREADY_CANCELED"))
    }

    @Test
    fun `party size patch decreases size and blocks increase with 422 code`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 7302L,
                organizationId = 31L,
                leaderUserId = 810L,
                partySize = 4,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T01:00:00Z")
            )
        )

        mockMvc.perform(
            patch("/bookings/${booking.id}/party-size")
                .header("Idempotency-Key", "party-size-k-1")
                .header("X-Actor-User-Id", "810")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":2}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.partySize").value(2))

        mockMvc.perform(
            patch("/bookings/${booking.id}/party-size")
                .header("Idempotency-Key", "party-size-k-2")
                .header("X-Actor-User-Id", "810")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":3}""")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("PARTY_SIZE_INCREASE_NOT_ALLOWED"))
    }

    @Test
    fun `party size patch idempotency replay and in-progress follow policy`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 7303L,
                organizationId = 31L,
                leaderUserId = 811L,
                partySize = 4,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T01:00:00Z")
            )
        )

        val firstBody = mockMvc.perform(
            patch("/bookings/${booking.id}/party-size")
                .header("Idempotency-Key", "party-size-k-3")
                .header("X-Actor-User-Id", "811")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":2}""")
        )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        mockMvc.perform(
            patch("/bookings/${booking.id}/party-size")
                .header("Idempotency-Key", "party-size-k-3")
                .header("X-Actor-User-Id", "811")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":2}""")
        )
            .andExpect(status().isOk)
            .andExpect { result -> kotlin.test.assertEquals(firstBody, result.response.contentAsString) }

        idempotencyStore.markInProgressForTest(
            actorUserId = 811L,
            method = "PATCH",
            pathTemplate = "/bookings/{bookingId}/party-size",
            idempotencyKey = "party-size-k-4",
            requestHash = hash("${booking.id}|PARTY_SIZE_PATCH|1")
        )

        mockMvc.perform(
            patch("/bookings/${booking.id}/party-size")
                .header("Idempotency-Key", "party-size-k-4")
                .header("X-Actor-User-Id", "811")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":1}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_IN_PROGRESS"))
    }

    @Test
    fun `booking complete returns 204 and transitions to COMPLETED with idempotent replay`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 73031L,
                organizationId = 31L,
                leaderUserId = 812L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T01:05:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${booking.id}/complete")
                .header("Idempotency-Key", "booking-complete-k-1")
                .header("X-Actor-User-Id", "912")
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/bookings/${booking.id}/complete")
                .header("Idempotency-Key", "booking-complete-k-1")
                .header("X-Actor-User-Id", "912")
        )
            .andExpect(status().isNoContent)

        kotlin.test.assertEquals(BookingStatus.COMPLETED, bookingRepository.findById(requireNotNull(booking.id))?.status)
    }

    @Test
    fun `booking complete verifies 409 422 error codes and idempotency policy`() {
        val requested = bookingRepository.save(
            Booking(
                occurrenceId = 73032L,
                organizationId = 31L,
                leaderUserId = 813L,
                partySize = 2,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T01:06:00Z")
            )
        )
        val confirmedA = bookingRepository.save(
            Booking(
                occurrenceId = 73032L,
                organizationId = 31L,
                leaderUserId = 814L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T01:07:00Z")
            )
        )
        val confirmedB = bookingRepository.save(
            Booking(
                occurrenceId = 73032L,
                organizationId = 31L,
                leaderUserId = 815L,
                partySize = 1,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T01:08:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${requested.id}/complete")
                .header("Idempotency-Key", "booking-complete-k-2")
                .header("X-Actor-User-Id", "913")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"))

        mockMvc.perform(
            post("/bookings/${confirmedA.id}/complete")
                .header("Idempotency-Key", "booking-complete-k-3")
                .header("X-Actor-User-Id", "913")
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/bookings/${confirmedB.id}/complete")
                .header("Idempotency-Key", "booking-complete-k-3")
                .header("X-Actor-User-Id", "913")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD"))

        idempotencyStore.markInProgressForTest(
            actorUserId = 913L,
            method = "POST",
            pathTemplate = "/bookings/{bookingId}/complete",
            idempotencyKey = "booking-complete-k-4",
            requestHash = hash("${confirmedB.id}|COMPLETE|")
        )

        mockMvc.perform(
            post("/bookings/${confirmedB.id}/complete")
                .header("Idempotency-Key", "booking-complete-k-4")
                .header("X-Actor-User-Id", "913")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_IN_PROGRESS"))
    }

    @Test
    fun `occurrence finish returns 204 then blocks new booking with 409 code`() {
        occurrenceRepository.save(Occurrence(id = 73033L, organizationId = 31L, capacity = 10, status = OccurrenceStatus.SCHEDULED))

        mockMvc.perform(
            post("/occurrences/73033/finish")
                .header("Idempotency-Key", "occ-finish-k-1")
                .header("X-Actor-User-Id", "914")
        )
            .andExpect(status().isNoContent)

        kotlin.test.assertEquals(OccurrenceStatus.FINISHED, occurrenceRepository.getOrCreate(73033L).status)

        mockMvc.perform(
            post("/occurrences/73033/bookings")
                .header("Idempotency-Key", "occ-finish-booking-k-1")
                .header("X-Actor-User-Id", "816")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":1}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"))
    }

    @Test
    fun `occurrence finish verifies 409 422 error codes and idempotency policy`() {
        occurrenceRepository.save(Occurrence(id = 73034L, organizationId = 31L, capacity = 12, status = OccurrenceStatus.SCHEDULED))
        occurrenceRepository.save(Occurrence(id = 73035L, organizationId = 31L, capacity = 12, status = OccurrenceStatus.SCHEDULED))

        mockMvc.perform(
            post("/occurrences/73034/finish")
                .header("Idempotency-Key", "occ-finish-k-2")
                .header("X-Actor-User-Id", "915")
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/occurrences/73035/finish")
                .header("Idempotency-Key", "occ-finish-k-2")
                .header("X-Actor-User-Id", "915")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD"))

        idempotencyStore.markInProgressForTest(
            actorUserId = 915L,
            method = "POST",
            pathTemplate = "/occurrences/{occurrenceId}/finish",
            idempotencyKey = "occ-finish-k-3",
            requestHash = hash("73035|finish")
        )

        mockMvc.perform(
            post("/occurrences/73035/finish")
                .header("Idempotency-Key", "occ-finish-k-3")
                .header("X-Actor-User-Id", "915")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_IN_PROGRESS"))

        mockMvc.perform(
            post("/occurrences/73034/finish")
                .header("Idempotency-Key", "occ-finish-k-4")
                .header("X-Actor-User-Id", "915")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"))
    }

    @Test
    fun `inquiry create enforces bookingId required and booking scope match`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 7304L,
                organizationId = 31L,
                leaderUserId = 820L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T02:00:00Z")
            )
        )

        mockMvc.perform(
            post("/occurrences/7304/inquiries")
                .header("Idempotency-Key", "inq-k-1")
                .header("X-Actor-User-Id", "820")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"message":"hello"}""")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("REQUIRED_FIELD_MISSING"))

        mockMvc.perform(
            post("/occurrences/9999/inquiries")
                .header("Idempotency-Key", "inq-k-2")
                .header("X-Actor-User-Id", "820")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"bookingId":${booking.id},"message":"hello"}""")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("BOOKING_SCOPE_MISMATCH"))
    }

    @Test
    fun `inquiry create requires actor user header`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 73041L,
                organizationId = 31L,
                leaderUserId = 820L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T02:00:00Z")
            )
        )

        mockMvc.perform(
            post("/occurrences/73041/inquiries")
                .header("Idempotency-Key", "inq-authz-k-0")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"bookingId":${booking.id},"message":"hello"}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
    }

    @Test
    fun `inquiry create is idempotent and writes audit minimum contract`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 7305L,
                organizationId = 31L,
                leaderUserId = 821L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T02:00:00Z")
            )
        )

        val firstBody = mockMvc.perform(
            post("/occurrences/7305/inquiries")
                .header("Idempotency-Key", "inq-k-3")
                .header("X-Actor-User-Id", "821")
                .header("X-Request-Id", "req-inq-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"bookingId":${booking.id},"subject":"문의","message":"좌석 문의"}""")
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        mockMvc.perform(
            post("/occurrences/7305/inquiries")
                .header("Idempotency-Key", "inq-k-3")
                .header("X-Actor-User-Id", "821")
                .header("X-Request-Id", "req-inq-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"bookingId":${booking.id},"subject":"문의","message":"좌석 문의"}""")
        )
            .andExpect(status().isCreated)
            .andExpect { result -> kotlin.test.assertEquals(firstBody, result.response.contentAsString) }

        val lastEvent = auditEventAdapter.all().last()
        kotlin.test.assertEquals("INQUIRY_CREATED", lastEvent.action)
        kotlin.test.assertEquals("INQUIRY", lastEvent.resourceType)
        kotlin.test.assertEquals("req-inq-001", lastEvent.requestId)
        kotlin.test.assertTrue(lastEvent.actor.startsWith("USER:"))
        kotlin.test.assertNotNull(lastEvent.occurredAtUtc)
    }

    @Test
    fun `inquiry messages allow leader and org operator`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 7311L,
                organizationId = 31L,
                leaderUserId = 880L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T03:10:00Z")
            )
        )
        val inquiry = inquiryRepository.save(
            Inquiry(
                organizationId = 31L,
                occurrenceId = 7311L,
                bookingId = requireNotNull(booking.id),
                createdByUserId = 880L,
                subject = "문의",
                createdAt = Instant.parse("2026-03-06T03:11:00Z")
            )
        )

        mockMvc.perform(
            post("/inquiries/${inquiry.id}/messages")
                .header("Idempotency-Key", "inq-msg-k-1")
                .header("X-Actor-User-Id", "880")
                .header("X-Request-Id", "req-msg-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"리더 메시지"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.inquiryId").value(requireNotNull(inquiry.id)))
            .andExpect(jsonPath("$.body").value("리더 메시지"))

        mockMvc.perform(
            post("/inquiries/${inquiry.id}/messages")
                .header("Idempotency-Key", "inq-msg-k-2")
                .header("X-Actor-User-Id", "990")
                .header("X-Actor-Org-Role", "ORG_ADMIN")
                .header("X-Actor-Org-Id", "31")
                .header("X-Request-Id", "req-msg-002")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"운영자 답변"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.body").value("운영자 답변"))

        mockMvc.perform(
            get("/inquiries/${inquiry.id}/messages")
                .header("X-Actor-User-Id", "880")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].body").value("리더 메시지"))
            .andExpect(jsonPath("$.items[1].body").value("운영자 답변"))

        val operatorEvent = auditEventAdapter.all().last()
        kotlin.test.assertEquals("INQUIRY_MESSAGE_POSTED", operatorEvent.action)
        kotlin.test.assertEquals("INQUIRY_MESSAGE", operatorEvent.resourceType)
        kotlin.test.assertEquals("req-msg-002", operatorEvent.requestId)
        kotlin.test.assertEquals("OPERATOR:990", operatorEvent.actor)
    }

    @Test
    fun `inquiry message rejects unauthorized actor and missing body and scope mismatch`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 7312L,
                organizationId = 31L,
                leaderUserId = 881L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T03:20:00Z")
            )
        )
        val inquiry = inquiryRepository.save(
            Inquiry(
                organizationId = 31L,
                occurrenceId = 7312L,
                bookingId = requireNotNull(booking.id),
                createdByUserId = 881L,
                subject = "문의",
                createdAt = Instant.parse("2026-03-06T03:21:00Z")
            )
        )

        mockMvc.perform(
            post("/inquiries/${inquiry.id}/messages")
                .header("Idempotency-Key", "inq-msg-authz-k-1")
                .header("X-Actor-User-Id", "9999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"no auth"}""")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))

        mockMvc.perform(
            post("/inquiries/${inquiry.id}/messages")
                .header("Idempotency-Key", "inq-msg-body-k-1")
                .header("X-Actor-User-Id", "881")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{}""")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("REQUIRED_FIELD_MISSING"))

        mockMvc.perform(
            post("/inquiries/${inquiry.id}/messages")
                .header("Idempotency-Key", "inq-msg-scope-k-1")
                .header("X-Actor-User-Id", "882")
                .header("X-Actor-Org-Role", "ORG_OWNER")
                .header("X-Actor-Org-Id", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"scope"}""")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
    }

    @Test
    fun `inquiry message follows idempotency replay conflict and in-progress rules`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 7313L,
                organizationId = 31L,
                leaderUserId = 883L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T03:30:00Z")
            )
        )
        val inquiry = inquiryRepository.save(
            Inquiry(
                organizationId = 31L,
                occurrenceId = 7313L,
                bookingId = requireNotNull(booking.id),
                createdByUserId = 883L,
                createdAt = Instant.parse("2026-03-06T03:31:00Z")
            )
        )

        val firstBody = mockMvc.perform(
            post("/inquiries/${inquiry.id}/messages")
                .header("Idempotency-Key", "inq-msg-idem-k-1")
                .header("X-Actor-User-Id", "883")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"중복 테스트"}""")
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        mockMvc.perform(
            post("/inquiries/${inquiry.id}/messages")
                .header("Idempotency-Key", "inq-msg-idem-k-1")
                .header("X-Actor-User-Id", "883")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"중복 테스트"}""")
        )
            .andExpect(status().isCreated)
            .andExpect { result -> kotlin.test.assertEquals(firstBody, result.response.contentAsString) }

        mockMvc.perform(
            post("/inquiries/${inquiry.id}/messages")
                .header("Idempotency-Key", "inq-msg-idem-k-1")
                .header("X-Actor-User-Id", "883")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"다른 payload"}""")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD"))

        idempotencyStore.markInProgressForTest(
            actorUserId = 883L,
            method = "POST",
            pathTemplate = "/inquiries/{inquiryId}/messages",
            idempotencyKey = "inq-msg-idem-k-2",
            requestHash = hash("${inquiry.id}|처리중|")
        )

        mockMvc.perform(
            post("/inquiries/${inquiry.id}/messages")
                .header("Idempotency-Key", "inq-msg-idem-k-2")
                .header("X-Actor-User-Id", "883")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"처리중"}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_IN_PROGRESS"))
    }

    @Test
    fun `inquiry close enforces state transition and idempotency with audit`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 7314L,
                organizationId = 31L,
                leaderUserId = 884L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T03:40:00Z")
            )
        )
        val inquiry = inquiryRepository.save(
            Inquiry(
                organizationId = 31L,
                occurrenceId = 7314L,
                bookingId = requireNotNull(booking.id),
                createdByUserId = 884L,
                createdAt = Instant.parse("2026-03-06T03:41:00Z")
            )
        )

        mockMvc.perform(
            post("/inquiries/${inquiry.id}/close")
                .header("Idempotency-Key", "inq-close-k-1")
                .header("X-Actor-User-Id", "884")
                .header("X-Request-Id", "req-close-001")
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/inquiries/${inquiry.id}/close")
                .header("Idempotency-Key", "inq-close-k-1")
                .header("X-Actor-User-Id", "884")
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/inquiries/${inquiry.id}/close")
                .header("Idempotency-Key", "inq-close-k-2")
                .header("X-Actor-User-Id", "884")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"))

        kotlin.test.assertEquals(InquiryStatus.CLOSED, inquiryRepository.findById(requireNotNull(inquiry.id))?.status)
        val lastEvent = auditEventAdapter.all().last()
        kotlin.test.assertEquals("INQUIRY_CLOSED", lastEvent.action)
        kotlin.test.assertEquals("INQUIRY", lastEvent.resourceType)
        kotlin.test.assertEquals("req-close-001", lastEvent.requestId)
        kotlin.test.assertEquals("USER:884", lastEvent.actor)
        kotlin.test.assertEquals(
            1,
            auditEventAdapter.all().count { it.action == "INQUIRY_CLOSED" }
        )
    }

    @Test
    fun `inquiry close returns 422 for idempotency payload mismatch and 409 in-progress`() {
        val booking1 = bookingRepository.save(
            Booking(
                occurrenceId = 7315L,
                organizationId = 31L,
                leaderUserId = 885L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T03:50:00Z")
            )
        )
        val booking2 = bookingRepository.save(
            Booking(
                occurrenceId = 7316L,
                organizationId = 31L,
                leaderUserId = 885L,
                partySize = 1,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T03:51:00Z")
            )
        )

        val inquiry1 = inquiryRepository.save(
            Inquiry(
                organizationId = 31L,
                occurrenceId = 7315L,
                bookingId = requireNotNull(booking1.id),
                createdByUserId = 885L,
                createdAt = Instant.parse("2026-03-06T03:52:00Z")
            )
        )
        val inquiry2 = inquiryRepository.save(
            Inquiry(
                organizationId = 31L,
                occurrenceId = 7316L,
                bookingId = requireNotNull(booking2.id),
                createdByUserId = 885L,
                createdAt = Instant.parse("2026-03-06T03:53:00Z")
            )
        )

        mockMvc.perform(
            post("/inquiries/${inquiry1.id}/close")
                .header("Idempotency-Key", "inq-close-k-3")
                .header("X-Actor-User-Id", "885")
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/inquiries/${inquiry2.id}/close")
                .header("Idempotency-Key", "inq-close-k-3")
                .header("X-Actor-User-Id", "885")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD"))

        idempotencyStore.markInProgressForTest(
            actorUserId = 885L,
            method = "POST",
            pathTemplate = "/inquiries/{inquiryId}/close",
            idempotencyKey = "inq-close-k-4",
            requestHash = hash("${inquiry2.id}|CLOSE")
        )

        mockMvc.perform(
            post("/inquiries/${inquiry2.id}/close")
                .header("Idempotency-Key", "inq-close-k-4")
                .header("X-Actor-User-Id", "885")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_IN_PROGRESS"))
    }

    @Test
    fun `list inquiry messages rejects unauthorized actor`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 7317L,
                organizationId = 31L,
                leaderUserId = 886L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T03:55:00Z")
            )
        )
        val inquiry = inquiryRepository.save(
            Inquiry(
                organizationId = 31L,
                occurrenceId = 7317L,
                bookingId = requireNotNull(booking.id),
                createdByUserId = 886L,
                createdAt = Instant.parse("2026-03-06T03:56:00Z")
            )
        )

        mockMvc.perform(
            get("/inquiries/${inquiry.id}/messages")
                .header("X-Actor-User-Id", "99999")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
    }

    @Test
    fun `inquiry endpoints validate org role and org id headers consistently`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 7318L,
                organizationId = 31L,
                leaderUserId = 887L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T03:57:00Z")
            )
        )
        val inquiry = inquiryRepository.save(
            Inquiry(
                organizationId = 31L,
                occurrenceId = 7318L,
                bookingId = requireNotNull(booking.id),
                createdByUserId = 887L,
                createdAt = Instant.parse("2026-03-06T03:58:00Z")
            )
        )

        mockMvc.perform(
            get("/inquiries/${inquiry.id}/messages")
                .header("X-Actor-User-Id", "9901")
                .header("X-Actor-Org-Role", "ORG_ADMIN")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("REQUIRED_FIELD_MISSING"))

        mockMvc.perform(
            get("/inquiries/${inquiry.id}/messages")
                .header("X-Actor-User-Id", "9901")
                .header("X-Actor-Org-Id", "31")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("REQUIRED_FIELD_MISSING"))

        mockMvc.perform(
            get("/inquiries/${inquiry.id}/messages")
                .header("X-Actor-User-Id", "9901")
                .header("X-Actor-Org-Role", "ORG_MANAGER")
                .header("X-Actor-Org-Id", "31")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
    }

    @Test
    fun `offer expiry compares by now greater than expiresAt rule`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 7306L,
                organizationId = 31L,
                leaderUserId = 830L,
                partySize = 2,
                status = BookingStatus.OFFERED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                offerExpiresAtUtc = Instant.now().plusSeconds(60),
                createdAt = Instant.parse("2026-03-06T02:10:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${booking.id}/offer/accept")
                .header("Idempotency-Key", "offer-boundary-k-1")
                .header("X-Actor-User-Id", "830")
        )
            .andExpect(status().isNoContent)

        val secondBooking = bookingRepository.save(
            Booking(
                occurrenceId = 7306L,
                organizationId = 31L,
                leaderUserId = 831L,
                partySize = 2,
                status = BookingStatus.OFFERED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                offerExpiresAtUtc = Instant.now().minusMillis(1),
                createdAt = Instant.parse("2026-03-06T02:11:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${secondBooking.id}/offer/decline")
                .header("Idempotency-Key", "offer-boundary-k-2")
                .header("X-Actor-User-Id", "831")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("OFFER_EXPIRED"))
    }

    @Test
    fun `create booking ignores expired offered seat holding on time boundary`() {
        occurrenceRepository.save(Occurrence(id = 7307L, organizationId = 31L, capacity = 2))
        bookingRepository.save(
            Booking(
                occurrenceId = 7307L,
                organizationId = 31L,
                leaderUserId = 840L,
                partySize = 2,
                status = BookingStatus.OFFERED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                offerExpiresAtUtc = Instant.now().minusSeconds(5),
                createdAt = Instant.parse("2026-03-06T02:20:00Z")
            )
        )

        mockMvc.perform(
            post("/occurrences/7307/bookings")
                .header("Idempotency-Key", "create-boundary-k-1")
                .header("X-Actor-User-Id", "841")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":2}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("REQUESTED"))
    }

    @Test
    fun `cancel confirmed booking promotes waitlist and creates offer window`() {
        occurrenceRepository.save(Occurrence(id = 7308L, organizationId = 31L, capacity = 4))
        val confirmed = bookingRepository.save(
            Booking(
                occurrenceId = 7308L,
                organizationId = 31L,
                leaderUserId = 850L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T02:30:00Z")
            )
        )
        bookingRepository.save(
            Booking(
                occurrenceId = 7308L,
                organizationId = 31L,
                leaderUserId = 851L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T02:30:30Z")
            )
        )
        val waitlisted = bookingRepository.save(
            Booking(
                occurrenceId = 7308L,
                organizationId = 31L,
                leaderUserId = 852L,
                partySize = 2,
                status = BookingStatus.WAITLISTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T02:31:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${confirmed.id}/cancel")
                .header("Idempotency-Key", "cancel-promote-k-1")
                .header("X-Actor-User-Id", "850")
                .header("X-Request-Id", "req-waitlist-001")
        )
            .andExpect(status().isNoContent)

        val promoted = bookingRepository.findById(requireNotNull(waitlisted.id))
        kotlin.test.assertEquals(BookingStatus.OFFERED, promoted?.status)
        kotlin.test.assertNotNull(promoted?.offerExpiresAtUtc)

        val actions = auditEventAdapter.all().map { it.action }
        kotlin.test.assertTrue(actions.contains("WAITLIST_PROMOTED_TO_OFFER"))
    }

    @Test
    fun `inquiry create by non booking leader returns 403 forbidden`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 7309L,
                organizationId = 31L,
                leaderUserId = 860L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T02:40:00Z")
            )
        )

        mockMvc.perform(
            post("/occurrences/7309/inquiries")
                .header("Idempotency-Key", "inq-authz-k-1")
                .header("X-Actor-User-Id", "9999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"bookingId":${booking.id},"message":"문의"}""")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
    }

    @Test
    fun `offer accept idempotency in progress returns 409 with code`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 7310L,
                organizationId = 31L,
                leaderUserId = 870L,
                partySize = 2,
                status = BookingStatus.OFFERED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                offerExpiresAtUtc = Instant.now().plusSeconds(3600),
                createdAt = Instant.parse("2026-03-06T02:50:00Z")
            )
        )

        idempotencyStore.markInProgressForTest(
            actorUserId = 870L,
            method = "POST",
            pathTemplate = "/bookings/{bookingId}/offer/accept",
            idempotencyKey = "offer-in-progress-k-1",
            requestHash = hash("${booking.id}|OFFER_ACCEPT|")
        )

        mockMvc.perform(
            post("/bookings/${booking.id}/offer/accept")
                .header("Idempotency-Key", "offer-in-progress-k-1")
                .header("X-Actor-User-Id", "870")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_IN_PROGRESS"))
    }

    @Test
    fun `tour review create returns 201 when attendee completed booking`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 7401L,
                organizationId = 31L,
                leaderUserId = 901L,
                partySize = 2,
                status = BookingStatus.COMPLETED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T04:00:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = 901L,
                createdAt = booking.createdAt
            ).recordAttendance(com.demo.tourwave.domain.booking.AttendanceStatus.ATTENDED)
        )

        mockMvc.perform(
            post("/occurrences/7401/reviews/tour")
                .header("Idempotency-Key", "review-tour-k-1")
                .header("X-Actor-User-Id", "901")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rating":5,"comment":"great tour"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.type").value("TOUR"))
            .andExpect(jsonPath("$.rating").value(5))
    }

    @Test
    fun `review create requires actor user header`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 74011L,
                organizationId = 31L,
                leaderUserId = 901L,
                partySize = 2,
                status = BookingStatus.COMPLETED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T04:00:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = 901L,
                createdAt = booking.createdAt
            ).recordAttendance(com.demo.tourwave.domain.booking.AttendanceStatus.ATTENDED)
        )

        mockMvc.perform(
            post("/occurrences/74011/reviews/tour")
                .header("Idempotency-Key", "review-authz-k-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rating":5,"comment":"great tour"}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
    }

    @Test
    fun `duplicate tour review returns 409 DUPLICATE_REVIEW`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 7402L,
                organizationId = 31L,
                leaderUserId = 902L,
                partySize = 2,
                status = BookingStatus.COMPLETED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T04:10:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = 902L,
                createdAt = booking.createdAt
            ).recordAttendance(com.demo.tourwave.domain.booking.AttendanceStatus.ATTENDED)
        )

        mockMvc.perform(
            post("/occurrences/7402/reviews/tour")
                .header("Idempotency-Key", "review-tour-k-2")
                .header("X-Actor-User-Id", "902")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rating":4}""")
        )
            .andExpect(status().isCreated)

        mockMvc.perform(
            post("/occurrences/7402/reviews/tour")
                .header("Idempotency-Key", "review-tour-k-3")
                .header("X-Actor-User-Id", "902")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rating":5}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("DUPLICATE_REVIEW"))
    }

    @Test
    fun `review create returns 422 ATTENDANCE_NOT_ELIGIBLE for non attendee`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 7403L,
                organizationId = 31L,
                leaderUserId = 903L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T04:20:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = 903L,
                createdAt = booking.createdAt
            )
        )

        mockMvc.perform(
            post("/occurrences/7403/reviews/instructor")
                .header("Idempotency-Key", "review-inst-k-1")
                .header("X-Actor-User-Id", "903")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rating":4}""")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("ATTENDANCE_NOT_ELIGIBLE"))
    }

    @Test
    fun `review create follows idempotency replay and in progress policy`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 7404L,
                organizationId = 31L,
                leaderUserId = 904L,
                partySize = 2,
                status = BookingStatus.COMPLETED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T04:30:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = 904L,
                createdAt = booking.createdAt
            ).recordAttendance(com.demo.tourwave.domain.booking.AttendanceStatus.ATTENDED)
        )

        val firstBody = mockMvc.perform(
            post("/occurrences/7404/reviews/instructor")
                .header("Idempotency-Key", "review-inst-k-2")
                .header("X-Actor-User-Id", "904")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rating":5,"comment":"kind"}""")
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString

        mockMvc.perform(
            post("/occurrences/7404/reviews/instructor")
                .header("Idempotency-Key", "review-inst-k-2")
                .header("X-Actor-User-Id", "904")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rating":5,"comment":"kind"}""")
        )
            .andExpect(status().isCreated)
            .andExpect { result -> kotlin.test.assertEquals(firstBody, result.response.contentAsString) }

        idempotencyStore.markInProgressForTest(
            actorUserId = 904L,
            method = "POST",
            pathTemplate = "/occurrences/{occurrenceId}/reviews/instructor",
            idempotencyKey = "review-inst-k-3",
            requestHash = hash("7404|INSTRUCTOR|3|pending")
        )

        mockMvc.perform(
            post("/occurrences/7404/reviews/instructor")
                .header("Idempotency-Key", "review-inst-k-3")
                .header("X-Actor-User-Id", "904")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rating":3,"comment":"pending"}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_IN_PROGRESS"))
    }

    @Test
    fun `review summary endpoint returns aggregated minimal snapshot`() {
        val booking1 = bookingRepository.save(
            Booking(
                occurrenceId = 7405L,
                organizationId = 31L,
                leaderUserId = 905L,
                partySize = 1,
                status = BookingStatus.COMPLETED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T04:40:00Z")
            )
        )
        val booking2 = bookingRepository.save(
            Booking(
                occurrenceId = 7405L,
                organizationId = 31L,
                leaderUserId = 906L,
                partySize = 1,
                status = BookingStatus.COMPLETED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T04:41:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking1.id),
                userId = 905L,
                createdAt = booking1.createdAt
            ).recordAttendance(com.demo.tourwave.domain.booking.AttendanceStatus.ATTENDED)
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking2.id),
                userId = 906L,
                createdAt = booking2.createdAt
            ).recordAttendance(com.demo.tourwave.domain.booking.AttendanceStatus.ATTENDED)
        )

        mockMvc.perform(
            post("/occurrences/7405/reviews/tour")
                .header("Idempotency-Key", "review-sum-k-1")
                .header("X-Actor-User-Id", "905")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rating":4}""")
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/occurrences/7405/reviews/tour")
                .header("Idempotency-Key", "review-sum-k-2")
                .header("X-Actor-User-Id", "906")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rating":2}""")
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/occurrences/7405/reviews/instructor")
                .header("Idempotency-Key", "review-sum-k-3")
                .header("X-Actor-User-Id", "905")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rating":5}""")
        ).andExpect(status().isCreated)

        mockMvc.perform(
            get("/occurrences/7405/reviews/summary")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.occurrenceId").value(7405))
            .andExpect(jsonPath("$.tour.count").value(2))
            .andExpect(jsonPath("$.tour.averageRating").value(3.0))
            .andExpect(jsonPath("$.instructor.count").value(1))
            .andExpect(jsonPath("$.instructor.averageRating").value(5.0))
    }

    @Test
    fun `accepted attendee can create review even when not booking leader`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 7406L,
                organizationId = 31L,
                leaderUserId = 907L,
                partySize = 2,
                status = BookingStatus.COMPLETED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T04:50:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = 907L,
                createdAt = booking.createdAt
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant(
                bookingId = requireNotNull(booking.id),
                userId = 908L,
                status = BookingParticipantStatus.ACCEPTED,
                attendanceStatus = com.demo.tourwave.domain.booking.AttendanceStatus.ATTENDED,
                invitedAt = Instant.parse("2026-03-05T00:00:00Z"),
                respondedAt = Instant.parse("2026-03-05T01:00:00Z"),
                createdAt = Instant.parse("2026-03-05T00:00:00Z")
            )
        )

        mockMvc.perform(
            post("/occurrences/7406/reviews/tour")
                .header("Idempotency-Key", "review-tour-k-4")
                .header("X-Actor-User-Id", "908")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"rating":5,"comment":"participant review"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.reviewerUserId").value(908))
    }

    private fun hash(raw: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `partySize patch rejects value lower than active participant count`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 9901L,
                organizationId = 31L,
                leaderUserId = 901L,
                partySize = 3,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = 901L,
                createdAt = booking.createdAt
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant(
                bookingId = requireNotNull(booking.id),
                userId = 902L,
                status = BookingParticipantStatus.ACCEPTED,
                invitedAt = Instant.parse("2026-03-05T00:00:00Z"),
                respondedAt = Instant.parse("2026-03-05T01:00:00Z"),
                createdAt = Instant.parse("2026-03-05T00:00:00Z")
            )
        )

        mockMvc.perform(
            patch("/bookings/${booking.id}/party-size")
                .header("Idempotency-Key", "party-size-participant-limit-k-1")
                .header("X-Actor-User-Id", "901")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"partySize":1}""")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.error.details.activeParticipantCount").value(2))
    }

    @Test
    fun `cancel booking cascades participant status to canceled`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 9902L,
                organizationId = 31L,
                leaderUserId = 911L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = 911L,
                createdAt = booking.createdAt
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant(
                bookingId = requireNotNull(booking.id),
                userId = 912L,
                status = BookingParticipantStatus.INVITED,
                invitedAt = Instant.parse("2026-03-05T00:00:00Z"),
                createdAt = Instant.parse("2026-03-05T00:00:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${booking.id}/cancel")
                .header("Idempotency-Key", "cancel-booking-participants-k-1")
                .header("X-Actor-User-Id", "911")
        )
            .andExpect(status().isNoContent)

        val participants = bookingParticipantRepository.findByBookingId(requireNotNull(booking.id))
        assertEquals(setOf(BookingParticipantStatus.CANCELED), participants.map { it.status }.toSet())
    }

    @Test
    fun `cancel occurrence cascades participant status to canceled`() {
        occurrenceRepository.save(Occurrence(id = 9903L, organizationId = 31L, capacity = 10))
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 9903L,
                organizationId = 31L,
                leaderUserId = 921L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = 921L,
                createdAt = booking.createdAt
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant(
                bookingId = requireNotNull(booking.id),
                userId = 922L,
                status = BookingParticipantStatus.ACCEPTED,
                invitedAt = Instant.parse("2026-03-05T00:00:00Z"),
                respondedAt = Instant.parse("2026-03-05T01:00:00Z"),
                createdAt = Instant.parse("2026-03-05T00:00:00Z")
            )
        )

        mockMvc.perform(
            post("/occurrences/9903/cancel")
                .header("Idempotency-Key", "cancel-occurrence-participants-k-1")
                .header("X-Actor-User-Id", "921")
        )
            .andExpect(status().isNoContent)

        val participants = bookingParticipantRepository.findByBookingId(requireNotNull(booking.id))
        assertEquals(setOf(BookingParticipantStatus.CANCELED), participants.map { it.status }.toSet())
    }

    @Test
    fun `create participant invitation returns invited participant`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 9904L,
                organizationId = 31L,
                leaderUserId = 931L,
                partySize = 2,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = 931L,
                createdAt = booking.createdAt
            )
        )

        mockMvc.perform(
            post("/bookings/${booking.id}/participants/invitations")
                .header("Idempotency-Key", "participant-invite-k-1")
                .header("X-Actor-User-Id", "931")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteeUserId":932}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.bookingId").value(requireNotNull(booking.id)))
            .andExpect(jsonPath("$.userId").value(932))
            .andExpect(jsonPath("$.status").value("INVITED"))

        val participants = bookingParticipantRepository.findByBookingId(requireNotNull(booking.id))
        assertEquals(setOf(931L, 932L), participants.map { it.userId }.toSet())
    }

    @Test
    fun `create participant invitation rejects duplicate invite`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 9905L,
                organizationId = 31L,
                leaderUserId = 941L,
                partySize = 3,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = 941L,
                createdAt = booking.createdAt
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant(
                bookingId = requireNotNull(booking.id),
                userId = 942L,
                status = BookingParticipantStatus.INVITED,
                invitedAt = Instant.parse("2026-03-05T00:00:00Z"),
                createdAt = Instant.parse("2026-03-05T00:00:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${booking.id}/participants/invitations")
                .header("Idempotency-Key", "participant-invite-k-2")
                .header("X-Actor-User-Id", "941")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteeUserId":942}""")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
    }

    @Test
    fun `create participant invitation rejects non leader actor`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 9906L,
                organizationId = 31L,
                leaderUserId = 951L,
                partySize = 2,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = 951L,
                createdAt = booking.createdAt
            )
        )

        mockMvc.perform(
            post("/bookings/${booking.id}/participants/invitations")
                .header("Idempotency-Key", "participant-invite-k-3")
                .header("X-Actor-User-Id", "952")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteeUserId":953}""")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
    }

    @Test
    fun `create participant invitation rejects terminal booking`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 9907L,
                organizationId = 31L,
                leaderUserId = 961L,
                partySize = 2,
                status = BookingStatus.CANCELED,
                paymentStatus = PaymentStatus.REFUNDED,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = 961L,
                createdAt = booking.createdAt
            )
        )

        mockMvc.perform(
            post("/bookings/${booking.id}/participants/invitations")
                .header("Idempotency-Key", "participant-invite-k-4")
                .header("X-Actor-User-Id", "961")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteeUserId":962}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("BOOKING_TERMINAL_STATE"))
    }

    @Test
    fun `accept participant invitation updates status to accepted`() {
        val now = Instant.now()
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 9908L,
                organizationId = 31L,
                leaderUserId = 971L,
                partySize = 2,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        val invitation = bookingParticipantRepository.save(
            BookingParticipant(
                bookingId = requireNotNull(booking.id),
                userId = 972L,
                status = BookingParticipantStatus.INVITED,
                invitedAt = now.minusSeconds(60),
                createdAt = now.minusSeconds(60)
            )
        )

        mockMvc.perform(
            post("/bookings/${booking.id}/participants/invitations/${invitation.id}/accept")
                .header("Idempotency-Key", "participant-invite-accept-k-1")
                .header("X-Actor-User-Id", "972")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ACCEPTED"))

        val saved = bookingParticipantRepository.findById(requireNotNull(invitation.id))
        assertEquals(BookingParticipantStatus.ACCEPTED, saved?.status)
    }

    @Test
    fun `decline participant invitation updates status to declined`() {
        val now = Instant.now()
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 9909L,
                organizationId = 31L,
                leaderUserId = 981L,
                partySize = 2,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        val invitation = bookingParticipantRepository.save(
            BookingParticipant(
                bookingId = requireNotNull(booking.id),
                userId = 982L,
                status = BookingParticipantStatus.INVITED,
                invitedAt = now.minusSeconds(60),
                createdAt = now.minusSeconds(60)
            )
        )

        mockMvc.perform(
            post("/bookings/${booking.id}/participants/invitations/${invitation.id}/decline")
                .header("Idempotency-Key", "participant-invite-decline-k-1")
                .header("X-Actor-User-Id", "982")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("DECLINED"))

        val saved = bookingParticipantRepository.findById(requireNotNull(invitation.id))
        assertEquals(BookingParticipantStatus.DECLINED, saved?.status)
    }

    @Test
    fun `respond participant invitation rejects non invited actor`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 9910L,
                organizationId = 31L,
                leaderUserId = 991L,
                partySize = 2,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        val invitation = bookingParticipantRepository.save(
            BookingParticipant(
                bookingId = requireNotNull(booking.id),
                userId = 992L,
                status = BookingParticipantStatus.INVITED,
                invitedAt = Instant.parse("2026-03-05T00:00:00Z"),
                createdAt = Instant.parse("2026-03-05T00:00:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${booking.id}/participants/invitations/${invitation.id}/accept")
                .header("Idempotency-Key", "participant-invite-accept-k-2")
                .header("X-Actor-User-Id", "993")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
    }

    @Test
    fun `respond participant invitation rejects already responded invitation`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 9911L,
                organizationId = 31L,
                leaderUserId = 994L,
                partySize = 2,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        val invitation = bookingParticipantRepository.save(
            BookingParticipant(
                bookingId = requireNotNull(booking.id),
                userId = 995L,
                status = BookingParticipantStatus.ACCEPTED,
                invitedAt = Instant.parse("2026-03-05T00:00:00Z"),
                respondedAt = Instant.parse("2026-03-05T01:00:00Z"),
                createdAt = Instant.parse("2026-03-05T00:00:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${booking.id}/participants/invitations/${invitation.id}/decline")
                .header("Idempotency-Key", "participant-invite-decline-k-2")
                .header("X-Actor-User-Id", "995")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"))
    }

    @Test
    fun `respond participant invitation expires after 48 hours`() {
        val now = Instant.now()
        occurrenceRepository.save(
            Occurrence(
                id = 9912L,
                organizationId = 31L,
                capacity = 10,
                startsAtUtc = now.plusSeconds(24 * 60 * 60L)
            )
        )
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 9912L,
                organizationId = 31L,
                leaderUserId = 996L,
                partySize = 2,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        val invitation = bookingParticipantRepository.save(
            BookingParticipant(
                bookingId = requireNotNull(booking.id),
                userId = 997L,
                status = BookingParticipantStatus.INVITED,
                invitedAt = now.minusSeconds(49 * 60 * 60L),
                createdAt = now.minusSeconds(49 * 60 * 60L)
            )
        )

        mockMvc.perform(
            post("/bookings/${booking.id}/participants/invitations/${invitation.id}/accept")
                .header("Idempotency-Key", "participant-invite-accept-k-3")
                .header("X-Actor-User-Id", "997")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("INVITATION_EXPIRED"))

        val saved = bookingParticipantRepository.findById(requireNotNull(invitation.id))
        assertEquals(BookingParticipantStatus.EXPIRED, saved?.status)
    }

    @Test
    fun `create participant invitation expires pending invite at 6 hours before occurrence`() {
        val now = Instant.now()
        occurrenceRepository.save(
            Occurrence(
                id = 9913L,
                organizationId = 31L,
                capacity = 10,
                startsAtUtc = now.plusSeconds(5 * 60 * 60L)
            )
        )
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 9913L,
                organizationId = 31L,
                leaderUserId = 998L,
                partySize = 2,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        val staleInvitation = bookingParticipantRepository.save(
            BookingParticipant(
                bookingId = requireNotNull(booking.id),
                userId = 999L,
                status = BookingParticipantStatus.INVITED,
                invitedAt = now.minusSeconds(2 * 60 * 60L),
                createdAt = now.minusSeconds(2 * 60 * 60L)
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = 998L,
                createdAt = booking.createdAt
            )
        )

        mockMvc.perform(
            post("/bookings/${booking.id}/participants/invitations")
                .header("Idempotency-Key", "participant-invite-k-5")
                .header("X-Actor-User-Id", "998")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteeUserId":1000}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.userId").value(1000))

        val expiredInvitation = bookingParticipantRepository.findById(requireNotNull(staleInvitation.id))
        assertEquals(BookingParticipantStatus.EXPIRED, expiredInvitation?.status)
    }

    @Test
    fun `record participant attendance updates attendance status`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 9914L,
                organizationId = 31L,
                leaderUserId = 1001L,
                partySize = 2,
                status = BookingStatus.COMPLETED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        val participant = bookingParticipantRepository.save(
            BookingParticipant(
                bookingId = requireNotNull(booking.id),
                userId = 1002L,
                status = BookingParticipantStatus.ACCEPTED,
                createdAt = Instant.parse("2026-03-05T00:00:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${booking.id}/participants/${participant.id}/attendance")
                .header("Idempotency-Key", "participant-attendance-k-1")
                .header("X-Actor-User-Id", "1001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"attendanceStatus":"ATTENDED"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.attendanceStatus").value("ATTENDED"))

        val saved = bookingParticipantRepository.findById(requireNotNull(participant.id))
        assertEquals(com.demo.tourwave.domain.booking.AttendanceStatus.ATTENDED, saved?.attendanceStatus)
    }

    @Test
    fun `record participant attendance rejects non attending participant`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 9915L,
                organizationId = 31L,
                leaderUserId = 1003L,
                partySize = 2,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        val participant = bookingParticipantRepository.save(
            BookingParticipant(
                bookingId = requireNotNull(booking.id),
                userId = 1004L,
                status = BookingParticipantStatus.INVITED,
                invitedAt = Instant.parse("2026-03-05T00:00:00Z"),
                createdAt = Instant.parse("2026-03-05T00:00:00Z")
            )
        )

        mockMvc.perform(
            post("/bookings/${booking.id}/participants/${participant.id}/attendance")
                .header("Idempotency-Key", "participant-attendance-k-2")
                .header("X-Actor-User-Id", "1003")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"attendanceStatus":"NO_SHOW"}""")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
    }

    @Test
    fun `participant list exposes attendance to leader and refreshes expired invitations`() {
        occurrenceRepository.save(
            Occurrence(
                id = 9916L,
                organizationId = 31L,
                capacity = 10,
                startsAtUtc = Instant.parse("2026-03-20T18:00:00Z")
            )
        )
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 9916L,
                organizationId = 31L,
                leaderUserId = 1005L,
                partySize = 3,
                status = BookingStatus.COMPLETED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = 1005L,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            ).recordAttendance(com.demo.tourwave.domain.booking.AttendanceStatus.ATTENDED)
        )
        bookingParticipantRepository.save(
            BookingParticipant(
                bookingId = requireNotNull(booking.id),
                userId = 1006L,
                status = BookingParticipantStatus.ACCEPTED,
                attendanceStatus = com.demo.tourwave.domain.booking.AttendanceStatus.NO_SHOW,
                invitedAt = Instant.parse("2026-03-06T01:00:00Z"),
                respondedAt = Instant.parse("2026-03-06T02:00:00Z"),
                createdAt = Instant.parse("2026-03-06T01:00:00Z")
            )
        )
        val staleInvitation = bookingParticipantRepository.save(
            BookingParticipant(
                bookingId = requireNotNull(booking.id),
                userId = 1007L,
                status = BookingParticipantStatus.INVITED,
                invitedAt = Instant.parse("2026-03-10T00:00:00Z"),
                createdAt = Instant.parse("2026-03-10T00:00:00Z")
            )
        )

        mockMvc.perform(
            get("/bookings/${booking.id}/participants")
                .header("X-Actor-User-Id", "1005")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(3))
            .andExpect(jsonPath("$.items[0].userId").value(1005))
            .andExpect(jsonPath("$.items[0].attendanceStatus").value("ATTENDED"))
            .andExpect(jsonPath("$.items[1].userId").value(1006))
            .andExpect(jsonPath("$.items[1].attendanceStatus").value("NO_SHOW"))
            .andExpect(jsonPath("$.items[2].userId").value(1007))
            .andExpect(jsonPath("$.items[2].status").value("EXPIRED"))

        assertEquals(BookingParticipantStatus.EXPIRED, bookingParticipantRepository.findById(requireNotNull(staleInvitation.id))?.status)
    }

    @Test
    fun `participant list rejects non participant actor`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 9917L,
                organizationId = 31L,
                leaderUserId = 1008L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = 1008L,
                createdAt = booking.createdAt
            )
        )

        mockMvc.perform(
            get("/bookings/${booking.id}/participants")
                .header("X-Actor-User-Id", "9999")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
    }

    @Test
    fun `participant list allows matching org operator`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 9918L,
                organizationId = 31L,
                leaderUserId = 1009L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-06T00:00:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = 1009L,
                createdAt = booking.createdAt
            )
        )

        mockMvc.perform(
            get("/bookings/${booking.id}/participants")
                .header("X-Actor-User-Id", "2000")
                .header("X-Actor-Org-Role", "ORG_ADMIN")
                .header("X-Actor-Org-Id", "31")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].userId").value(1009))
    }
}
