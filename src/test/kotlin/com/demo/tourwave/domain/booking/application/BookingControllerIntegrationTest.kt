package com.demo.tourwave.domain.booking.application

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.occurrence.OccurrenceStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.security.MessageDigest
import java.time.Instant

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

    @BeforeEach
    fun setUp() {
        bookingRepository.clear()
        occurrenceRepository.clear()
        idempotencyStore.clear()
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
            requestHash = hash("${saved.id}|APPROVE")
        )

        mockMvc.perform(
            post("/bookings/${saved.id}/approve")
                .header("Idempotency-Key", "approve-k-6")
                .header("X-Actor-User-Id", "713")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_IN_PROGRESS"))
    }

    private fun hash(raw: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
