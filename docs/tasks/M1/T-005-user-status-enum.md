---
id: T-005
title: "T-005 — [BE] user — UserStatus enum 완성 (SUSPENDED, DELETED 구현)"
aliases: [T-005]

repo: tourwave
area: be
milestone: M1
domain: auth
layer: domain + application
size: M
status: done

depends_on: []
blocks: ['T-006']
sub_tasks: []

github_issue: null
exec_plan: ""

created: 2026-04-18
updated: 2026-04-19
---

#status/done #area/be

# T-005 — [BE] user — UserStatus enum 완성 (SUSPENDED, DELETED 구현)

## 파일 소유권
WRITE:
  - `src/main/kotlin/com/demo/tourwave/domain/user/UserStatus.kt` (enum 완성)
  - `src/main/kotlin/com/demo/tourwave/domain/user/User.kt` (상태 전이 로직 추가)
  - `src/main/kotlin/com/demo/tourwave/application/user/UserService.kt` (suspend, delete 메서드 추가 — T-004 다음)
  - `db/migration/V<timestamp>__user_status_soft_delete.sql` (Flyway 마이그레이션)
  - `src/test/kotlin/com/demo/tourwave/domain/user/UserStatusTest.kt` (상태 전이 테스트)

READ:
  - `docs/audit/BE-user.md` §관찰된 문제 #4, #5
  - `docs/domain-rules.md` (상태 머신 규칙)
  - `docs/policies.md` (soft-delete 정책)

DO NOT TOUCH:
  - `src/main/kotlin/com/demo/tourwave/adapter/in/web/` (컨트롤러 기존)
  - `src/main/kotlin/com/demo/tourwave/adapter/out/persistence/` (어댑터 기존)

## SSOT 근거
- `docs/audit/BE-user.md` §관찰된 문제 #4: "SUSPENDED, DELETED 상태 미구현 — User enum에 정의했으나 비활성화는 DEACTIVATED만 사용. SUSPENDED 용도 불명, DELETED는 소프트 딜리트 미지원"
- `docs/audit/BE-user.md` §관찰된 문제 #5: "사용자 삭제 계획 부재 — deactivate()는 상태만 변경. 개인정보 처리 방침상 영구 삭제/익명화 처리 로직 없음"
- `docs/domain-rules.md` §Terminal state rule: 터미널 상태 규칙 (추가 전이 불가)

## 현재 상태 (갭)
- [ ] `UserStatus.SUSPENDED` 용도 미정의
- [ ] `UserStatus.DELETED` 상태로의 전이 로직 없음 (소프트 딜리트 미지원)
- [ ] 상태 전이 규칙 (state machine) 미정의
- [ ] DELETED 상태 사용자 필터링 로직 없음 (쿼리에서 제외)
- [ ] 개인정보 처리(마스킹, 익명화) 로직 부재

## 구현 지침
1. `UserStatus` enum 상태 정의 명확화:
   - `ACTIVE` — 정상 활동 중 (초기 상태)
   - `DEACTIVATED` — 사용자가 계정 비활성화 요청 (재활성화 가능)
   - `SUSPENDED` — 운영자 정책 위반으로 일시 정지 (재활성화는 운영자 판단)
   - `DELETED` — 소프트 딜리트 (로그인 불가, 개인정보 마스킹)

2. `User` domain entity에 상태 전이 규칙 추가:
   - ACTIVE → DEACTIVATED, SUSPENDED, DELETED (모두 가능)
   - DEACTIVATED → ACTIVE, DELETED (복구 또는 삭제)
   - SUSPENDED → ACTIVE, DELETED (해제 또는 영구 삭제)
   - DELETED → (전이 불가, 터미널 상태)

3. Flyway 마이그레이션: `users` 테이블에 `deleted_at_utc` 컬럼 추가 (소프트 딜리트 타임스탬프)

4. `UserService` 메서드 추가:
   - `suspend(userId, reason): void` (감사 이벤트 기록)
   - `delete(userId): void` (개인정보 마스킹 + DELETED 상태 전이)
   - `restore(userId): void` (DEACTIVATED → ACTIVE 복구)

5. `UserRepository.findById()` 개선: DELETED 상태 사용자는 조회 불가 (선택적, 별도 메서드로 분리 가능)

6. 마스킹 로직:
   - displayName → "Deleted User #<id>"
   - email → "deleted_<id>@deleted.local"
   - password → null (이미 해시됨)

## Acceptance Criteria
- [ ] `UserStatus` enum 4개 상태 정의 완료
- [ ] `User.transition(toStatus)` 메서드 구현 (규칙 검증)
- [ ] `UserService.suspend()`, `delete()`, `restore()` 구현 완료
- [ ] Flyway 마이그레이션 작성 완료
- [ ] 마스킹 로직 구현 완료 (delete 시)
- [ ] `./gradlew test --tests "*UserStatusTest"` 통과
- [ ] `./gradlew test` 전체 통과 (migration 동작 확인)

## Verification
`./scripts/verify-task.sh T-005`
예상 결과: build ✓ / test ✓ / lint ✓ / migration ✓ / docs ✓

## Rollback
```bash
git checkout -- src/main/kotlin/com/demo/tourwave/domain/user/
git checkout -- src/main/kotlin/com/demo/tourwave/application/user/UserService.kt
git clean -fd src/test/kotlin/com/demo/tourwave/domain/user/UserStatusTest.kt
# 마이그레이션 롤백
./gradlew clean test
```

## Notes
- `deleted_at_utc` 타임스탬프는 soft-delete 감사 용도 (쿼리 필터링은 status enum으로)
- SUSPENDED vs DEACTIVATED 구분: 한쪽은 사용자 의도, 한쪽은 운영자 정책
- 마스킹은 delete() 호출 시점에 수행 (되돌릴 수 없음 — 별도 "undo" 불가)
- 향후 GDPR "right to be forgotten" 규제 대응 시 별도 작업
- T-006에서 상태 전이 테스트 케이스 추가 예정
