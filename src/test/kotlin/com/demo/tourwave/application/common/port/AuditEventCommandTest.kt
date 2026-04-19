package com.demo.tourwave.application.common.port

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * 순수 단위 테스트 — Spring/DB/Testcontainers 사용 금지.
 *
 * 드리프트 노트(T-901 exec-plan 참조):
 *   태스크 카드가 `domain/common/AuditEvent.kt`를 대상으로 명시했으나
 *   해당 파일은 존재하지 않고, 실제 구현체는 이 패키지의 AuditEventCommand / AuditActorType이다.
 *   phantom 도메인 엔티티를 새로 생성하는 대신 실제 클래스를 테스트한다.
 */
class AuditEventCommandTest {

    // ------------------------------------------------------------------
    // 1. AuditActorType 열거형 — 전수 확인
    // ------------------------------------------------------------------

    @Test
    fun `AuditActorType enum entries are exactly USER OPERATOR SYSTEM JOB`() {
        val entries = AuditActorType.entries.map { it.name }.toSet()
        assertEquals(setOf("USER", "OPERATOR", "SYSTEM", "JOB"), entries)
    }

    // ------------------------------------------------------------------
    // 2. actor 파싱 — actorType 유도
    // ------------------------------------------------------------------

    @Test
    fun `actor string USER prefix resolves actorType to USER`() {
        val cmd = buildCommand(actor = "USER:123")
        assertEquals(AuditActorType.USER, cmd.actorType)
    }

    @Test
    fun `actor string OPERATOR prefix resolves actorType to OPERATOR`() {
        val cmd = buildCommand(actor = "OPERATOR:5")
        assertEquals(AuditActorType.OPERATOR, cmd.actorType)
    }

    @Test
    fun `actor string JOB prefix resolves actorType to JOB`() {
        val cmd = buildCommand(actor = "JOB:99")
        assertEquals(AuditActorType.JOB, cmd.actorType)
    }

    @Test
    fun `actor string SYSTEM prefix resolves actorType to SYSTEM`() {
        val cmd = buildCommand(actor = "SYSTEM")
        assertEquals(AuditActorType.SYSTEM, cmd.actorType)
    }

    @Test
    fun `unknown actor prefix resolves actorType to SYSTEM`() {
        val cmd = buildCommand(actor = "UNKNOWN:7")
        assertEquals(AuditActorType.SYSTEM, cmd.actorType)
    }

    // ------------------------------------------------------------------
    // 3. actor 파싱 — actorId 유도
    // ------------------------------------------------------------------

    @Test
    fun `actor string USER colon numeric resolves actorId`() {
        val cmd = buildCommand(actor = "USER:123")
        assertEquals(123L, cmd.actorId)
    }

    @Test
    fun `actor string with no colon suffix resolves actorId to null`() {
        val cmd = buildCommand(actor = "SYSTEM")
        assertNull(cmd.actorId)
    }

    @Test
    fun `actor string with non-numeric suffix resolves actorId to null`() {
        val cmd = buildCommand(actor = "USER:abc")
        assertNull(cmd.actorId)
    }

    @Test
    fun `actor string with empty suffix resolves actorId to null`() {
        val cmd = buildCommand(actor = "JOB:")
        assertNull(cmd.actorId)
    }

    // ------------------------------------------------------------------
    // 4. 기본값 검증
    // ------------------------------------------------------------------

    @Test
    fun `default optional fields are null or empty`() {
        val cmd = buildCommand()
        assertNull(cmd.requestId)
        assertNull(cmd.reasonCode)
        assertNull(cmd.beforeJson)
        assertNull(cmd.afterJson)
        assertTrue(cmd.details.isEmpty(), "details should default to emptyMap()")
    }

    // ------------------------------------------------------------------
    // 5. 명시적 값으로 생성 — 정상 경로
    // ------------------------------------------------------------------

    @Test
    fun `command created with all fields retains every value`() {
        val now = Instant.parse("2026-04-19T12:00:00Z")
        val before = mapOf<String, Any?>("status" to "REQUESTED")
        val after = mapOf<String, Any?>("status" to "CONFIRMED")
        val details = mapOf<String, Any?>("reason" to "operator approval")

        val cmd = AuditEventCommand(
            actor = "OPERATOR:7",
            action = "BOOKING_APPROVED",
            resourceType = "Booking",
            resourceId = 42L,
            occurredAtUtc = now,
            requestId = "req-abc",
            details = details,
            reasonCode = "APPROVED_BY_OPERATOR",
            beforeJson = before,
            afterJson = after
        )

        assertEquals("OPERATOR:7", cmd.actor)
        assertEquals("BOOKING_APPROVED", cmd.action)
        assertEquals("Booking", cmd.resourceType)
        assertEquals(42L, cmd.resourceId)
        assertEquals(now, cmd.occurredAtUtc)
        assertEquals("req-abc", cmd.requestId)
        assertEquals(details, cmd.details)
        assertEquals("APPROVED_BY_OPERATOR", cmd.reasonCode)
        assertEquals(before, cmd.beforeJson)
        assertEquals(after, cmd.afterJson)
        assertEquals(AuditActorType.OPERATOR, cmd.actorType)
        assertEquals(7L, cmd.actorId)
    }

    // ------------------------------------------------------------------
    // 6. 도메인 규칙 B에 명시된 감사 액션 값 표현 가능 확인
    // ------------------------------------------------------------------

    @Test
    fun `domain audit actions from domain-rules can be expressed as action strings`() {
        val actions = listOf(
            "BOOKING_CREATED",
            "BOOKING_APPROVED",
            "BOOKING_REJECTED",
            "BOOKING_CANCELED",
            "OFFER_CREATED",
            "OFFER_ACCEPTED",
            "OFFER_DECLINED",
            "OFFER_EXPIRED",
            "PARTY_SIZE_CHANGED",
            "OCCURRENCE_CANCELED",
            "OCCURRENCE_FINISHED",
            "INQUIRY_CREATED",
            "INQUIRY_CLOSED",
            "PAYMENT_CAPTURED",
            "PAYMENT_REFUNDED",
            "IDEMPOTENCY_PURGED"
        )

        actions.forEach { action ->
            val cmd = buildCommand(actor = "SYSTEM", action = action)
            assertEquals(action, cmd.action,
                "Expected action '$action' to be preserved in AuditEventCommand")
        }
    }

    // ------------------------------------------------------------------
    // 7. occurredAtUtc 타임스탬프 — 에포크 양수 검증
    // ------------------------------------------------------------------

    @Test
    fun `occurredAtUtc epoch millis is positive when set to a future date`() {
        val ts = Instant.parse("2026-04-19T00:00:00Z")
        val cmd = buildCommand(occurredAtUtc = ts)
        assertTrue(cmd.occurredAtUtc.toEpochMilli() > 0,
            "occurredAtUtc epoch millis should be positive")
    }

    // ------------------------------------------------------------------
    // 8. data class 동등성 / copy 의미론
    // ------------------------------------------------------------------

    @Test
    fun `two commands with identical fields are equal`() {
        val ts = Instant.parse("2026-04-19T00:00:00Z")
        val a = buildCommand(actor = "USER:1", action = "BOOKING_CREATED", occurredAtUtc = ts)
        val b = buildCommand(actor = "USER:1", action = "BOOKING_CREATED", occurredAtUtc = ts)
        assertEquals(a, b)
    }

    @Test
    fun `copy with different action produces different command`() {
        val original = buildCommand(actor = "USER:1", action = "BOOKING_CREATED")
        val copy = original.copy(action = "BOOKING_CANCELED")
        assertEquals("BOOKING_CANCELED", copy.action)
        assertTrue(original != copy)
    }

    // ------------------------------------------------------------------
    // 헬퍼
    // ------------------------------------------------------------------

    private fun buildCommand(
        actor: String = "SYSTEM",
        action: String = "BOOKING_CREATED",
        resourceType: String = "Booking",
        resourceId: Long = 1L,
        occurredAtUtc: Instant = Instant.parse("2026-04-19T00:00:00Z")
    ): AuditEventCommand = AuditEventCommand(
        actor = actor,
        action = action,
        resourceType = resourceType,
        resourceId = resourceId,
        occurredAtUtc = occurredAtUtc
    )
}
