package com.demo.tourwave.application.booking

import com.demo.tourwave.TourwaveApplication
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.occurrence.Occurrence
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(classes = [TourwaveApplication::class])
@ActiveProfiles("mysql-test")
class MysqlBookingConcurrencyTest {
    @Autowired
    private lateinit var bookingCommandService: BookingCommandService

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    private lateinit var occurrenceRepository: OccurrenceRepository

    @Autowired
    private lateinit var paymentRecordRepository: PaymentRecordRepository

    @Autowired
    private lateinit var bookingParticipantRepository: BookingParticipantRepository

    @Autowired
    private lateinit var idempotencyStore: IdempotencyStore

    @BeforeEach
    fun setUp() {
        bookingParticipantRepository.clear()
        paymentRecordRepository.clear()
        bookingRepository.clear()
        occurrenceRepository.clear()
        idempotencyStore.clear()
    }

    @Test
    fun `concurrent approve keeps one booking within capacity`() {
        occurrenceRepository.save(
            Occurrence(
                id = 9301L,
                organizationId = 31L,
                capacity = 1,
                startsAtUtc = Instant.parse("2026-03-20T09:00:00Z"),
                timezone = "Asia/Seoul",
            ),
        )
        val first =
            bookingRepository.save(
                Booking(
                    occurrenceId = 9301L,
                    organizationId = 31L,
                    leaderUserId = 1L,
                    partySize = 1,
                    status = BookingStatus.REQUESTED,
                    paymentStatus = PaymentStatus.AUTHORIZED,
                    createdAt = Instant.parse("2026-03-12T00:00:00Z"),
                ),
            )
        val second =
            bookingRepository.save(
                Booking(
                    occurrenceId = 9301L,
                    organizationId = 31L,
                    leaderUserId = 2L,
                    partySize = 1,
                    status = BookingStatus.REQUESTED,
                    paymentStatus = PaymentStatus.AUTHORIZED,
                    createdAt = Instant.parse("2026-03-12T00:01:00Z"),
                ),
            )

        val executor = Executors.newFixedThreadPool(2)
        val latch = CountDownLatch(1)
        try {
            val results =
                listOf(first, second).mapIndexed { index, booking ->
                    executor.submit(
                        Callable {
                            latch.await()
                            runCatching {
                                bookingCommandService.mutateBooking(
                                    MutateBookingCommand(
                                        bookingId = requireNotNull(booking.id),
                                        actorUserId = 900L + index,
                                        idempotencyKey = "approve-${booking.id}",
                                        mutationType = BookingMutationType.APPROVE,
                                    ),
                                )
                            }
                        },
                    )
                }
            latch.countDown()
            val outcomes = results.map { it.get() }
            val successes = outcomes.filter { it.isSuccess }
            val failures = outcomes.filter { it.isFailure }.map { it.exceptionOrNull() }

            val confirmed = bookingRepository.findByOccurrenceAndStatuses(9301L, setOf(BookingStatus.CONFIRMED))
            assertEquals(1, confirmed.size)
            assertTrue(
                successes.size == 1 || failures.isNotEmpty(),
                "outcomes=$outcomes",
            )
            failures
                .filterIsInstance<DomainException>()
                .firstOrNull()
                ?.let { assertEquals(ErrorCode.CAPACITY_EXCEEDED, it.errorCode) }
        } finally {
            executor.shutdownNow()
        }
    }
}
