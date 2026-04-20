---
id: T-903
title: "T-903 — [BE] Idempotency-Key 일관성 검토 (announcement/operations/organization/instructor)"
aliases: [T-903]

repo: tourwave
area: be
milestone: cross
domain: infra
layer: policy
size: L
status: backlog

depends_on: []
blocks: []
sub_tasks: []

github_issue: 7
exec_plan: ""

created: 2026-04-18
updated: 2026-04-18
---

#status/backlog #area/be

# T-903 — [BE] Idempotency-Key 일관성 검토 (announcement/operations/organization/instructor)

## 파일 소유권
WRITE:
  - `src/main/kotlin/com/demo/tourwave/adapter/in/web/announcement/AnnouncementController.kt` (수정)
  - `src/main/kotlin/com/demo/tourwave/adapter/in/web/operations/OperatorRemediationQueueController.kt` (수정)
  - `src/main/kotlin/com/demo/tourwave/adapter/in/web/organization/OrganizationOperatorController.kt` (수정)
  - `src/main/kotlin/com/demo/tourwave/adapter/in/web/instructor/InstructorProfileController.kt` (수정)
  - `src/main/kotlin/com/demo/tourwave/adapter/in/web/instructor/InstructorRegistrationController.kt` (수정)
  - 해당 컨트롤러 테스트 (각 컨트롤러 통합 테스트)

READ:
  - `docs/domain-rules.md` "모든 상태 변경 엔드포인트에 Idempotency-Key 필수"
  - `docs/policies.md` "Idempotency-Key 정책"
  - `docs/openapi.yaml` (현 상태 API 정의)
  - 감사: `BE-announcement.md`, `BE-operations.md`, `BE-organization.md`, `BE-instructor.md`

DO NOT TOUCH:
  - booking/occurrence/inquiry 컨트롤러 (이미 구현)

## SSOT 근거
- `docs/domain-rules.md` — "모든 상태 변경 엔드포인트에 Idempotency-Key 헤더 필수"
  ```
  동일 키 + 다른 payload → 422 IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD
  ```
- 감사 관찰: `BE-announcement.md` "⚠ 미구현: POST/PATCH/DELETE에 Idempotency-Key 없음"
- 감사 관찰: `BE-operations.md` "⚠ 미구현: POST에 Idempotency-Key 없음"
- 감사 관찰: `BE-organization.md` "Idempotency-Key 미사용: 멤버 초대 중복 요청 시 멱등성 미보장"
- 감사 관찰: `BE-instructor.md` "Idempotency-Key 미사용: 중복 제출 검증 미보장"

## 현재 상태 (갭)
- [ ] AnnouncementController POST/PATCH/DELETE 미구현
- [ ] OperatorRemediationQueueController POST 미구현
- [ ] OrganizationOperatorController POST (invite, changeRole) 미구현
- [ ] InstructorProfileController POST (createMyProfile) 미구현
- [ ] InstructorRegistrationController POST (submitRegistration, approve, reject) 미구현
- [ ] 컨트롤러 어노테이션 `@IdempotencyKey` 미정의 또는 미사용

## 구현 지침
1. **컨트롤러 어노테이션 검증/정의**:
   - 기존 booking/occurrence에서 `@IdempotencyKey` 또는 필터 구현 참조
   - 없으면 커스텀 어노테이션 또는 필터 구현
2. **각 컨트롤러 수정** (상태 변경 POST/PATCH/DELETE):
   - 메서드에 `@IdempotencyKey` 추가 또는 헤더 검증 로직 추가
   - 요청 본문이 변경되면 IdempotencyStore.reserveOrReplay()에서 422 반환
3. **통합 테스트 패치**:
   - 각 컨트롤러 통합 테스트에 "동일 키 + 다른 payload" 케이스 추가
   - 예: 공지사항 title 변경하면서 같은 key 사용 → 422 검증
4. **OpenAPI 갱신**:
   - `docs/openapi.yaml`에 Idempotency-Key 헤더 문서화

## Acceptance Criteria
- [ ] AnnouncementController 모든 상태 변경 엔드포인트에 Idempotency-Key 구현
- [ ] OperatorRemediationQueueController POST에 Idempotency-Key 구현
- [ ] OrganizationOperatorController invite/changeRole에 Idempotency-Key 구현
- [ ] InstructorProfileController createMyProfile에 Idempotency-Key 구현
- [ ] InstructorRegistrationController submit/approve/reject에 Idempotency-Key 구현
- [ ] 각 엔드포인트 통합 테스트에 "동일 키 + 다른 payload → 422" 케이스 포함
- [ ] `./gradlew test --tests "*ControllerIntegrationTest"` 통과 (해당 컨트롤러들)

## Verification
`./scripts/verify-task.sh T-903`
- 각 컨트롤러 통합 테스트 실행
- OpenAPI 스키마 검증 (Idempotency-Key 헤더 presence)

## Rollback
`git checkout -- src/main/kotlin/com/demo/tourwave/adapter/in/web/`

## Notes
- **분할 고려**: 현재 5개 컨트롤러 + 각 테스트. 너무 크면 announcement/operations/organization/instructor 별로 4개 태스크로 분할 가능.
- 도메인 규칙: 422 에러코드는 `IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD` enum으로 정의. 존재 확인 후 사용.
- IdempotencyStore.reserveOrReplay() 반환값 활용: `IdempotencyDecision.REPLAY` 시 이전 응답 재사용.
