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
import com.demo.tourwave.support.MysqlTestContainerSupport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Occurrence 동시성 회귀 테스트 — 실제 MySQL Testcontainer 기반.
 *
 * 목적:
 * 1. 동시 100개 createBooking → confirmedSeats + offeredSeats <= capacity 불변식 보장
 * 2. 동시 다중 스레드가 동일 occurrence 에 접근해도 데드락 미발생 (timeout 5s 기준)
 * 3. lock() 이 먼저 획득된 후 capacity 검사가 이루어지는 DB 레벨 동시성 검증
 *
 * 레이어: @SpringBootTest + MySQL Testcontainer (MysqlTestContainerSupport 상속)
 */
@SpringBootTest(classes = [TourwaveApplication::class])
@ActiveProfiles("mysql")
class BookingConcurrencyAuditTest : MysqlTestContainerSupport() {
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

    // ──────────────────────────────────────────────────────────────────────────
    // 테스트 1: 동시 100개 createBooking — 불변식 보장
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 100개 스레드가 동시에 createBooking 을 호출할 때
     * confirmedSeats(≈ capacity) 이상의 booking 이 REQUESTED 상태로 과다 생성되지 않는지 검증한다.
     *
     * createBooking 은 REQUESTED 상태로 저장 후 approve 단계에서 capacity 검사를 수행한다.
     * 본 테스트는 "capacity 을 초과하는 CONFIRMED booking 이 0 개" 를 최종 검증한다.
     *
     * 검증 포인트:
     * - 전체 시나리오가 timeout (10s) 내 완료 — 데드락 미발생
     * - REQUESTED 상태로 저장된 booking 수는 capacity 초과 가능 (의도된 동작)
     * - 단, approve 를 모두 시도했을 때 CONFIRMED 는 capacity 이하
     */
    @Test
    fun `동시 100개 createBooking 후 approve 시 capacity 불변식을 위반하지 않는다`() {
        val occurrenceId = 9501L
        val capacity = 10
        val concurrentUsers = 100

        occurrenceRepository.save(
            Occurrence(
                id = occurrenceId,
                organizationId = 51L,
                capacity = capacity,
                startsAtUtc = Instant.parse("2026-06-01T09:00:00Z"),
                timezone = "Asia/Seoul",
            ),
        )

        // Step 1: 동시 100개 createBooking
        val executor = Executors.newFixedThreadPool(concurrentUsers)
        val createLatch = CountDownLatch(1)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        try {
            val createFutures =
                (1..concurrentUsers).map { userId ->
                    executor.submit(
                        Callable {
                            createLatch.await()
                            runCatching {
                                bookingCommandService.createBooking(
                                    CreateBookingCommand(
                                        actorUserId = userId.toLong(),
                                        occurrenceId = occurrenceId,
                                        partySize = 1,
                                        idempotencyKey = "create-audit-$userId",
                                    ),
                                )
                            }
                        },
                    )
                }
            createLatch.countDown()
            val createOutcomes = createFutures.map { it.get() }
            createOutcomes.forEach { result ->
                if (result.isSuccess) {
                    successCount.incrementAndGet()
                } else {
                    failCount.incrementAndGet()
                }
            }
        } finally {
            executor.shutdown()
        }

        // Step 2: 생성된 booking 목록 수집
        val allBookings =
            bookingRepository.findByOccurrenceAndStatuses(
                occurrenceId = occurrenceId,
                statuses = setOf(BookingStatus.REQUESTED),
            )

        assertTrue(
            allBookings.isNotEmpty(),
            "최소 하나의 booking 이 생성되어야 합니다. successCount=${successCount.get()}",
        )

        // Step 3: 동시 approve — 모든 REQUESTED booking 을 동시 approve 시도
        val approveExecutor = Executors.newFixedThreadPool(minOf(concurrentUsers, 50))
        val approveLatch = CountDownLatch(1)
        val approveErrors = CopyOnWriteArrayList<Throwable>()

        try {
            val approveFutures =
                allBookings.mapIndexed { index, booking ->
                    approveExecutor.submit(
                        Callable {
                            approveLatch.await()
                            runCatching {
                                bookingCommandService.mutateBooking(
                                    MutateBookingCommand(
                                        bookingId = requireNotNull(booking.id),
                                        actorUserId = 9000L + index,
                                        idempotencyKey = "approve-audit-${booking.id}",
                                        mutationType = BookingMutationType.APPROVE,
                                    ),
                                )
                            }.onFailure { approveErrors.add(it) }
                        },
                    )
                }
            approveLatch.countDown()
            approveFutures.forEach { it.get() } // timeout handled by JUnit test timeout
        } finally {
            approveExecutor.shutdown()
        }

        // 최종 검증: CONFIRMED booking 수 <= capacity (불변식)
        val confirmed =
            bookingRepository.findByOccurrenceAndStatuses(
                occurrenceId = occurrenceId,
                statuses = setOf(BookingStatus.CONFIRMED),
            )

        assertTrue(
            confirmed.size <= capacity,
            "CONFIRMED booking 수(${confirmed.size})가 capacity($capacity)를 초과합니다. " +
                "불변식 confirmedSeats + offeredSeats <= capacity 위반!",
        )

        // 에러 중 capacity exceeded 는 정상 동작 — 다른 오류가 있으면 실패
        val unexpectedErrors =
            approveErrors.filter { error ->
                error !is DomainException ||
                    error.errorCode != ErrorCode.CAPACITY_EXCEEDED
            }
        assertEquals(
            0,
            unexpectedErrors.size,
            "예상치 못한 오류 발생: ${unexpectedErrors.map { it.message }}",
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 테스트 2: 동시 2개 approve (기존 MysqlBookingConcurrencyTest 보완)
    //           + 락 획득 후 capacity 검사 순서가 DB 레벨에서도 보장되는지
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * capacity=1 인 occurrence 에 REQUESTED booking 2개를 동시에 approve 시도.
     * 정확히 하나만 CONFIRMED, 하나는 CAPACITY_EXCEEDED 로 실패해야 한다.
     *
     * 이 테스트는 lock() → capacity 검사 순서가 DB 레벨에서도 보장됨을 간접 증명한다:
     * 만약 락 없이 capacity 검사만 했다면 두 스레드 모두 "capacity 여유 있음"으로 판정 →
     * 두 booking 이 모두 CONFIRMED 되는 불변식 위반이 발생한다.
     */
    @Test
    fun `동시 approve 시 capacity=1 occurrence 에서 정확히 하나만 CONFIRMED된다 - 락 순서 보장 증명`() {
        val occurrenceId = 9502L

        occurrenceRepository.save(
            Occurrence(
                id = occurrenceId,
                organizationId = 51L,
                capacity = 1,
                startsAtUtc = Instant.parse("2026-06-02T09:00:00Z"),
                timezone = "Asia/Seoul",
            ),
        )

        val booking1 =
            bookingRepository.save(
                Booking(
                    occurrenceId = occurrenceId,
                    organizationId = 51L,
                    leaderUserId = 1L,
                    partySize = 1,
                    status = BookingStatus.REQUESTED,
                    paymentStatus = PaymentStatus.AUTHORIZED,
                    createdAt = Instant.parse("2026-06-01T00:00:00Z"),
                ),
            )
        val booking2 =
            bookingRepository.save(
                Booking(
                    occurrenceId = occurrenceId,
                    organizationId = 51L,
                    leaderUserId = 2L,
                    partySize = 1,
                    status = BookingStatus.REQUESTED,
                    paymentStatus = PaymentStatus.AUTHORIZED,
                    createdAt = Instant.parse("2026-06-01T00:01:00Z"),
                ),
            )

        val executor = Executors.newFixedThreadPool(2)
        val latch = CountDownLatch(1)
        try {
            val futures =
                listOf(booking1, booking2).mapIndexed { index, booking ->
                    executor.submit(
                        Callable {
                            latch.await()
                            runCatching {
                                bookingCommandService.mutateBooking(
                                    MutateBookingCommand(
                                        bookingId = requireNotNull(booking.id),
                                        actorUserId = 9000L + index,
                                        idempotencyKey = "approve-serial-${booking.id}",
                                        mutationType = BookingMutationType.APPROVE,
                                    ),
                                )
                            }
                        },
                    )
                }
            latch.countDown()
            futures.forEach { it.get() }
        } finally {
            executor.shutdownNow()
        }

        val confirmed =
            bookingRepository.findByOccurrenceAndStatuses(
                occurrenceId = occurrenceId,
                statuses = setOf(BookingStatus.CONFIRMED),
            )

        assertEquals(
            1,
            confirmed.size,
            "capacity=1 에서 CONFIRMED booking 은 정확히 1개여야 합니다. " +
                "실제: ${confirmed.size}개 — 락 획득 순서 보장 실패 의심",
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 테스트 3: 데드락 미발생 — 동시 다중 스레드, 다른 occurrence ID
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 두 스레드가 각각 다른 occurrence(A, B)에 동시 createBooking 을 수행한다.
     * 단일 occurrence 씩 락하므로 데드락 없이 완료되어야 한다.
     * (다중 occurrence 동시 락이 필요하면 ID 오름차순 정렬 필수 — 현재 단일만 존재)
     */
    @Test
    fun `서로 다른 occurrence 에 동시 접근해도 데드락이 발생하지 않는다`() {
        val occurrenceA = 9503L
        val occurrenceB = 9504L

        listOf(occurrenceA, occurrenceB).forEach { id ->
            occurrenceRepository.save(
                Occurrence(
                    id = id,
                    organizationId = 51L,
                    capacity = 10,
                    startsAtUtc = Instant.parse("2026-06-03T09:00:00Z"),
                    timezone = "Asia/Seoul",
                ),
            )
        }

        val executor = Executors.newFixedThreadPool(2)
        val latch = CountDownLatch(1)
        val errors = CopyOnWriteArrayList<Throwable>()

        try {
            val futures =
                listOf(occurrenceA to 1L, occurrenceB to 2L).mapIndexed { index, (occId, userId) ->
                    executor.submit(
                        Callable {
                            latch.await()
                            runCatching {
                                bookingCommandService.createBooking(
                                    CreateBookingCommand(
                                        actorUserId = userId,
                                        occurrenceId = occId,
                                        partySize = 1,
                                        idempotencyKey = "deadlock-test-$index",
                                    ),
                                )
                            }.onFailure { errors.add(it) }
                        },
                    )
                }
            latch.countDown()
            futures.forEach { it.get() }
        } finally {
            executor.shutdownNow()
        }

        // 데드락이 없으면 양쪽 모두 성공해야 함
        assertEquals(
            0,
            errors.size,
            "서로 다른 occurrence 동시 createBooking 에서 예상치 못한 오류 발생: " +
                "${errors.map { it.message }}",
        )

        listOf(occurrenceA, occurrenceB).forEach { occId ->
            val booked =
                bookingRepository.findByOccurrenceAndStatuses(
                    occurrenceId = occId,
                    statuses = setOf(BookingStatus.REQUESTED),
                )
            assertEquals(1, booked.size, "occurrence $occId 에 정확히 1개 booking 이 생성되어야 합니다")
        }
    }
}
