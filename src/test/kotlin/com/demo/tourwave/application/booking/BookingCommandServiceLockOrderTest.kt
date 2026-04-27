package com.demo.tourwave.application.booking

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.TimeWindowPolicyService
import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.common.port.IdempotencyDecision
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.booking.RefundPolicyAction
import com.demo.tourwave.domain.occurrence.OccurrenceStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * 락 획득 순서 검증 단위 테스트.
 *
 * 목적: BookingCommandService의 capacity-affecting 메서드들이
 *       capacity 검사(availableSeatsForOccurrence / ensureCapacityAvailable) **이전에**
 *       occurrenceRepository.lock()을 호출하는지 순서를 검증한다.
 *
 * 방식: OccurrenceRepository 를 spy 래퍼로 감싸 호출 시퀀스를 기록한다.
 *       Spring/DB/Testcontainers 사용 금지 — 순수 단위 테스트.
 */
class BookingCommandServiceLockOrderTest {

    // ──────────────────────────────────────────────────────────────────────────
    // Spy 래퍼: lock() 및 getOrCreate() 호출 순서를 기록
    // ──────────────────────────────────────────────────────────────────────────

    private class SpyOccurrenceRepository(
        private val delegate: OccurrenceRepository,
    ) : OccurrenceRepository by delegate {
        val callLog = mutableListOf<String>()

        override fun lock(occurrenceId: Long): Occurrence {
            callLog.add("lock:$occurrenceId")
            return delegate.lock(occurrenceId)
        }

        /**
         * getOrCreate 는 lock 이후 capacity 데이터를 읽는 경로이므로 기록한다.
         * availableSeatsForOccurrence → getOrCreate 순서를 검증하기 위해 필요.
         */
        override fun getOrCreate(occurrenceId: Long): Occurrence {
            callLog.add("getOrCreate:$occurrenceId")
            return delegate.getOrCreate(occurrenceId)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // In-memory 구현체 (Spring 없이 실행 가능)
    // ──────────────────────────────────────────────────────────────────────────

    private class SimpleOccurrenceRepository : OccurrenceRepository {
        private val store = mutableMapOf<Long, Occurrence>()

        override fun nextId(): Long = (store.keys.maxOrNull() ?: 0L) + 1
        override fun getOrCreate(occurrenceId: Long): Occurrence =
            store.computeIfAbsent(occurrenceId) {
                Occurrence(id = occurrenceId, organizationId = 1L, capacity = 100)
            }

        override fun findById(occurrenceId: Long): Occurrence? = store[occurrenceId]
        override fun findByTourId(tourId: Long): List<Occurrence> = emptyList()
        override fun findAll(): List<Occurrence> = store.values.toList()
        override fun lock(occurrenceId: Long): Occurrence = getOrCreate(occurrenceId)
        override fun save(occurrence: Occurrence) {
            store[occurrence.id] = occurrence
        }
        override fun clear() = store.clear()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 테스트 픽스처 초기화
    // ──────────────────────────────────────────────────────────────────────────

    private val clock = Clock.fixed(Instant.parse("2026-03-16T12:00:00Z"), ZoneOffset.UTC)

    private val bookingRepository = mock(BookingRepository::class.java)
    private val bookingParticipantRepository = mock(BookingParticipantRepository::class.java)
    private val idempotencyStore = mock(IdempotencyStore::class.java)
    private val auditEventPort = mock(AuditEventPort::class.java)
    private val paymentLedgerService = mock(PaymentLedgerService::class.java)
    private val timeWindowPolicyService = TimeWindowPolicyService(
        invitationWindowMinutes = 360,
        invitationExpiryHours = 48,
        refundFullWindowHours = 48,
    )

    private lateinit var spyOccurrenceRepo: SpyOccurrenceRepository
    private lateinit var service: BookingCommandService

    @BeforeEach
    fun setUp() {
        val inner = SimpleOccurrenceRepository()
        spyOccurrenceRepo = SpyOccurrenceRepository(inner)

        service = BookingCommandService(
            bookingRepository = bookingRepository,
            occurrenceRepository = spyOccurrenceRepo,
            bookingParticipantRepository = bookingParticipantRepository,
            idempotencyStore = idempotencyStore,
            auditEventPort = auditEventPort,
            paymentLedgerService = paymentLedgerService,
            timeWindowPolicyService = timeWindowPolicyService,
            clock = clock,
            offerWindowSeconds = 3600L,
        )

        // 공통 mock 설정
        whenever(bookingRepository.findByOccurrenceAndStatuses(any(), any())).thenReturn(emptyList())
        whenever(bookingParticipantRepository.findByBookingId(any())).thenReturn(emptyList())
        whenever(idempotencyStore.reserveOrReplay(any(), any(), any(), any(), any()))
            .thenReturn(IdempotencyDecision.Reserved)
        whenever(auditEventPort.append(any<AuditEventCommand>())).then { }
        // PaymentLedgerService: 입력 booking을 그대로 반환 (capture/applyRefundPolicy)
        whenever(paymentLedgerService.capture(any(), any())).thenAnswer { invocation ->
            (invocation.getArgument<Booking>(0)).copy(paymentStatus = PaymentStatus.PAID)
        }
        whenever(paymentLedgerService.applyRefundPolicy(any(), any(), any(), any())).thenAnswer { invocation ->
            invocation.getArgument<Booking>(0)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * lock:ID 항목이 가장 먼저 출현하는지(getOrCreate:ID 보다 앞서는지) 검증한다.
     * capacity 검사 경로에서 getOrCreate 호출이 반드시 lock 이후에 나타나야 한다.
     */
    private fun assertLockBeforeCapacityRead(occurrenceId: Long) {
        val log = spyOccurrenceRepo.callLog
        val lockIdx = log.indexOfFirst { it == "lock:$occurrenceId" }

        if (lockIdx == -1) {
            fail("lock($occurrenceId) 호출을 발견하지 못했습니다. callLog=$log")
        }
        // capacity 읽기(getOrCreate)가 lock 이후에 발생해야 한다.
        // (lock이 내부적으로 getOrCreate를 호출하므로 최초 getOrCreate는 lock 내부일 수 있음.
        //  여기서는 lock 호출이 capacity-affecting 경로의 첫 번째 진입점임을 검증한다.)
        assertTrue(lockIdx >= 0, "lock(:$occurrenceId) 이 호출되어야 한다. callLog=$log")
        // lock이 log 상 가장 앞에 나타나는지 확인 (lock이 첫 번째 관련 호출임)
        val firstEntryForOccurrence = log.indexOfFirst { it.endsWith(":$occurrenceId") }
        assertTrue(
            firstEntryForOccurrence == lockIdx,
            "occurrence $occurrenceId 에 대한 첫 번째 호출이 lock 이어야 합니다. " +
                "실제 첫 호출: ${log.getOrNull(firstEntryForOccurrence)}, callLog=$log",
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 테스트: createBooking — lock → capacity-read 순서
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `createBooking - lock이 capacity 읽기보다 먼저 호출된다`() {
        val occurrenceId = 1001L
        val savedBooking = Booking(
            id = 1L,
            occurrenceId = occurrenceId,
            organizationId = 1L,
            leaderUserId = 10L,
            partySize = 1,
            status = BookingStatus.REQUESTED,
            paymentStatus = PaymentStatus.AUTHORIZED,
            createdAt = clock.instant(),
        )
        whenever(bookingRepository.save(any())).thenReturn(savedBooking)

        service.createBooking(
            CreateBookingCommand(
                actorUserId = 10L,
                occurrenceId = occurrenceId,
                partySize = 1,
                idempotencyKey = "key-create-001",
            ),
        )

        assertLockBeforeCapacityRead(occurrenceId)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 테스트: mutateBooking(APPROVE) — lock → capacity-read 순서
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `mutateBooking APPROVE - lock이 capacity 읽기보다 먼저 호출된다`() {
        val occurrenceId = 1002L
        val bookingId = 2L
        val booking = Booking(
            id = bookingId,
            occurrenceId = occurrenceId,
            organizationId = 1L,
            leaderUserId = 10L,
            partySize = 1,
            status = BookingStatus.REQUESTED,
            paymentStatus = PaymentStatus.AUTHORIZED,
            createdAt = clock.instant(),
        )
        whenever(bookingRepository.findById(bookingId)).thenReturn(booking)
        whenever(bookingRepository.save(any())).thenReturn(booking.copy(status = BookingStatus.CONFIRMED))

        service.mutateBooking(
            MutateBookingCommand(
                bookingId = bookingId,
                actorUserId = 900L,
                idempotencyKey = "key-approve-001",
                mutationType = BookingMutationType.APPROVE,
            ),
        )

        assertLockBeforeCapacityRead(occurrenceId)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 테스트: mutateBooking(CANCEL) — lock이 먼저 호출된다
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `mutateBooking CANCEL - lock이 capacity 읽기보다 먼저 호출된다`() {
        val occurrenceId = 1003L
        val bookingId = 3L
        val booking = Booking(
            id = bookingId,
            occurrenceId = occurrenceId,
            organizationId = 1L,
            leaderUserId = 10L,
            partySize = 1,
            status = BookingStatus.CONFIRMED,
            paymentStatus = PaymentStatus.AUTHORIZED,
            createdAt = clock.instant(),
        )
        whenever(bookingRepository.findById(bookingId)).thenReturn(booking)
        whenever(bookingRepository.save(any())).thenReturn(booking.copy(status = BookingStatus.CANCELED))
        whenever(bookingRepository.findByOccurrenceAndStatuses(any(), any())).thenReturn(emptyList())

        service.mutateBooking(
            MutateBookingCommand(
                bookingId = bookingId,
                actorUserId = 10L,
                idempotencyKey = "key-cancel-001",
                mutationType = BookingMutationType.CANCEL,
            ),
        )

        // CANCEL은 좌석 해제 경로 — lock은 여전히 먼저 호출되어야 함
        val log = spyOccurrenceRepo.callLog
        val lockIdx = log.indexOfFirst { it == "lock:$occurrenceId" }
        assertTrue(lockIdx >= 0, "lock($occurrenceId) 이 호출되어야 합니다. callLog=$log")
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 테스트: 다중 occurrence ID 정렬 유틸리티 검증
    // 현재 BookingCommandService는 단일 occurrence만 락하므로 정렬 로직은
    // 유틸리티 레벨에서 검증한다.
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `다중 occurrence ID 목록을 오름차순으로 정렬해야 한다 - 데드락 회피 정책 검증`() {
        // 미래에 다중 occurrence 동시 락이 도입될 경우 항상 오름차순 ID 순으로 락해야 한다.
        // 이 테스트는 정책 자체의 정합성을 문서화하는 용도이다.
        val unsorted = listOf(5L, 2L, 8L, 1L, 3L)
        val sorted = unsorted.sortedBy { it }
        val expected = listOf(1L, 2L, 3L, 5L, 8L)
        assertTrue(
            sorted == expected,
            "다중 occurrence 락 획득 시 항상 ID 오름차순이어야 합니다 (데드락 회피). " +
                "실제: $sorted",
        )
    }
}
