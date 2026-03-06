package com.demo.tourwave.domain.booking.application

import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.booking.repository.BookingRepository
import com.demo.tourwave.domain.common.IdempotencyStore
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.occurrence.OccurrenceRepository
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

    private fun hash(raw: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
