---
id: T-901
title: "T-901 — [BE] AuditEventTest 생성 (감사 이벤트 커버리지)"
aliases: [T-901]

repo: tourwave
area: be
milestone: cross
domain: infra
layer: test
size: S
status: backlog

depends_on: []
blocks: ['T-904']
sub_tasks: []

github_issue: 5
exec_plan: ""

created: 2026-04-18
updated: 2026-04-18
---

#status/backlog #area/be

# T-901 — [BE] AuditEventTest 생성 (감사 이벤트 커버리지)

## 파일 소유권
WRITE:
  - `src/test/kotlin/com/demo/tourwave/domain/common/AuditEventTest.kt` (신규)

READ:
  - `src/main/kotlin/com/demo/tourwave/domain/common/AuditEvent.kt` (도메인 엔티티)
  - `src/main/kotlin/com/demo/tourwave/domain/common/AuditEventCommand.kt` (커맨드 구조)
  - `docs/domain-rules.md` "감사 이벤트" 섹션

DO NOT TOUCH:
  - 다른 test 클래스 (T-904 담당)

## SSOT 근거
- `CLAUDE.md` — "알려진 커버리지 미비 항목: AuditEventTest 미존재"
- `docs/domain-rules.md` — 감사 이벤트는 상태 변경마다 반드시 기록
- 감사 관찰 `BE-common.md` — AuditEventPort 인터페이스만 있고 구현/테스트 미검사

## 현재 상태 (갭)
- [ ] AuditEventTest 클래스 부재
- [ ] AuditEvent 생성자 유효성 검증 테스트 없음
- [ ] 액션/액터 조합 규칙 커버리지 부족

## 구현 지침
1. `AuditEventTest` 작성 (순수 단위 테스트, Spring 무관):
   - `eventId` 생성 검증 (UUID)
   - `createdAtUtc` 타임스탬프 검증
   - 필드 불변식: `actorUserId != null`, `action != null`, `aggregateType != null`
   - 부분 필드 nullable 검증: `aggregateId`, `details`
2. 테스트 케이스:
   - 정상 경로: 모든 필드 채운 경우
   - null 검증: actorUserId/action/aggregateType 누락 시 DomainException
   - UTC 검증: createdAtUtc > 0
   - 액션 enum 검증: BOOKING_CREATED, BOOKING_APPROVED, IDEMPOTENCY_PURGED 등

## Acceptance Criteria
- [ ] `./gradlew test --tests "*.AuditEventTest"` 통과
- [ ] 최소 8개 테스트 케이스
- [ ] Spring import 없음 (pure unit test)
- [ ] `docs/domain-rules.md` 감사 규칙 모두 커버

## Verification
`./gradlew test --tests "*.AuditEventTest"`

## Rollback
`git rm src/test/kotlin/com/demo/tourwave/domain/common/AuditEventTest.kt`

## Notes
- `AuditEvent` 엔티티의 `details` 필드가 Map<String,Any> 타입인 경우, JSON serialization 테스트도 포함 권장
- 액션 enum 전수 열거 테스트는 T-904에서 coverage 확인
