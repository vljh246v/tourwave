---
id: T-100
title: "T-100 — [BE] occurrence — 락 획득 순서 검증 + race condition 회귀 테스트"
aliases: [T-100]

repo: tourwave
area: be
milestone: M2
domain: occurrence
layer: application
size: M
status: in-progress

depends_on: []
blocks: []
sub_tasks: []

github_issue: null
exec_plan: ""

created: 2026-04-28
updated: 2026-04-28
---

#status/in-progress #area/be #risk/high

# T-100 — [BE] occurrence — 락 획득 순서 검증

## 파일 소유권
WRITE:
  - `src/test/kotlin/com/demo/tourwave/adapter/in/web/booking/BookingConcurrencyAuditTest.kt` (신규)
  - `src/test/kotlin/com/demo/tourwave/application/booking/BookingCommandServiceLockOrderTest.kt` (신규)
  - `src/main/kotlin/com/demo/tourwave/application/booking/BookingCommandService.kt` (필요 시 락 호출 위치 보정)

READ:
  - `src/main/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/occurrence/OccurrenceJpaRepository.kt` (`findLockedById` 존재)
  - `src/main/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/occurrence/JpaOccurrenceRepositoryAdapter.kt`
  - `src/main/kotlin/com/demo/tourwave/application/booking/BookingCommandService.kt`
  - `src/main/kotlin/com/demo/tourwave/application/occurrence/OccurrenceCommandService.kt`
  - `src/test/kotlin/com/demo/tourwave/adapter/in/web/booking/MysqlBookingConcurrencyTest.kt` (기존 동시성 테스트 참고)
  - `docs/domain-rules.md` §정원 불변식

DO NOT TOUCH:
  - `src/main/kotlin/com/demo/tourwave/domain/occurrence/` (도메인 불변식 변경 없음)

## SSOT 근거
- `docs/gap-matrix.md` 주요 갭 §🔴 고위험 #2: "Occurrence 용량 동시성 제어 검증 — 락은 존재(`findLockedById`), 호출 순서·정렬 검증 필요"
- `docs/domain-rules.md` §불변식: `confirmedSeats + offeredSeats <= capacity`
- `CLAUDE.md` §핵심 도메인 규칙: "좌석/정원 흐름은 occurrence 행 락을 먼저 획득한다"

## 현재 상태 (갭)
- [x] `OccurrenceJpaRepository.findLockedById` 정의됨 (PESSIMISTIC_WRITE)
- [x] `JpaOccurrenceRepositoryAdapter.findLockedById` 어댑터 노출됨
- [ ] `BookingCommandService.create/approve/...` 가 capacity 검사 **이전에** 락을 획득하는지 정적/동적 검증 부재
- [ ] 멀티 occurrence 동시 잠금 시 일관된 정렬(낮은 ID 순) 강제 부재 → 데드락 위험
- [ ] 회귀 테스트 보강 필요 (`MysqlBookingConcurrencyTest` 외 추가 시나리오)

## 구현 지침
1. **정적 검사 테스트** — `BookingCommandServiceLockOrderTest`:
   - FakeRepositories에 호출 순서 기록
   - capacity 검사 호출 이전에 `findLockedById` 호출 존재 검증
   - 다중 occurrence 처리 시 ID 오름차순 락 획득 검증
2. **동시성 회귀 테스트** — `BookingConcurrencyAuditTest` (MySQL Testcontainer):
   - 동시 100개 예약 생성 → `confirmedSeats + offeredSeats <= capacity` 항상 유지
   - 동시 다른 occurrence 락 획득 → 데드락 발생 여부 (timeout 5s)
   - 락 미획득 경로 발견 시 production 코드 보정
3. 보정 시 `BookingCommandService` 락 호출 순서를 capacity 검사 직전으로 재배치, occurrence ID 오름차순 정렬

## Acceptance Criteria
- [ ] `BookingCommandServiceLockOrderTest` 단위 테스트 신규 (Spring 미사용, FakeRepositories 기반)
- [ ] 모든 capacity-affecting 메서드(`create`, `approveOffer`, `cancelBooking` 등)에서 락 → capacity 검사 순서 강제
- [ ] 다중 occurrence 시 ID 오름차순 락 획득 강제
- [ ] `BookingConcurrencyAuditTest` MySQL 컨테이너 통합 테스트 신규
- [ ] 동시 100 동작 후 `confirmedSeats + offeredSeats <= capacity` 위반 0
- [ ] `./gradlew test --tests "*BookingCommandServiceLockOrderTest"` PASS
- [ ] `./gradlew test --tests "*BookingConcurrencyAuditTest"` PASS
- [ ] 기존 `MysqlBookingConcurrencyTest` 회귀 PASS

## Verification
```bash
./scripts/verify-task.sh T-100
```

## Rollback
```bash
git checkout -- src/main/kotlin/com/demo/tourwave/application/booking/BookingCommandService.kt
git clean -fd src/test/kotlin/com/demo/tourwave/adapter/in/web/booking/BookingConcurrencyAuditTest.kt
git clean -fd src/test/kotlin/com/demo/tourwave/application/booking/BookingCommandServiceLockOrderTest.kt
./gradlew clean test
```

## Notes
- 락 정렬 정책: 다중 occurrence 동시 락 시 항상 ID 오름차순 → 데드락 회피
- 기존 `MysqlBookingConcurrencyTest`는 단일 occurrence만 검증 — 본 카드는 다중 + 정렬 + 호출 순서 추가
- 보정이 production 코드에 닿으면 별도 PR 분리 검토 (테스트 우선 추가 → 회귀 발견 시 핫픽스)
