---
id: T-007
title: "T-007 — [BE] user — User soft-delete 정책 문서화"
aliases: [T-007]

repo: tourwave
area: be
milestone: M1
domain: auth
layer: documentation
size: S
status: done

depends_on: ['T-005']
blocks: []
sub_tasks: []

github_issue: null
exec_plan: ""

created: 2026-04-18
updated: 2026-04-26
reviewed: 2026-04-26
---

#status/done #area/be

# T-007 — [BE] user — User soft-delete 정책 문서화

## 파일 소유권
WRITE:
  - `docs/domain-rules.md` (User 섹션 추가 또는 확장)
  - `docs/policies.md` (§5 Trust Surface Policy 또는 신규 섹션)

READ:
  - `src/main/kotlin/com/demo/tourwave/domain/user/User.kt` (T-005 완료 후)
  - `src/main/kotlin/com/demo/tourwave/domain/user/UserStatus.kt` (상태 정의)
  - `docs/audit/BE-user.md` §관찰된 문제 #5
  - 기존 `docs/domain-rules.md` (포맷 일관성 확인)

DO NOT TOUCH:
  - 코드 수정 금지 (문서화만)
  - 다른 도메인의 문서

## SSOT 근거
- `docs/audit/BE-user.md` §관찰된 문제 #5: "사용자 삭제 계획 부재 — deactivate()는 상태만 변경. 개인정보 처리 방침상 영구 삭제/익명화 처리 로직 없음"
- CLAUDE.md §스펙 문서: "`docs/domain-rules.md` > ... (우선순위 순)"
- 기존 `docs/domain-rules.md` 패턴 참고

## 현재 상태 (갭)
- [ ] User 도메인의 상태 머신(ACTIVE/DEACTIVATED/SUSPENDED/DELETED) 미문서화
- [ ] soft-delete 정책 (필드 마스킹, 쿼리 제외 규칙) 미정의
- [ ] SUSPENDED vs DEACTIVATED 구분 기준 불명
- [ ] 개인정보 처리(GDPR/CCPA 대응) 가이드 부재

## 구현 지침
1. **docs/domain-rules.md에 추가 (또는 기존 섹션 확장)**:
   - 제목: `## User Entity & Lifecycle`
   - 내용:
     - User 상태 머신 (4개 상태 정의)
     - 상태 전이 규칙 (state diagram 텍스트 형식)
     - soft-delete 마스킹 정책 (필드별 변환 규칙)
     - 쿼리 필터링 가이드 (DELETED 상태 조회 제외)
     - 감사 이벤트 기록 요구사항

2. **docs/policies.md에 추가 (또는 신규 섹션)**:
   - 제목: `## 5.1 User Privacy & Deletion Policy`
   - 내용:
     - Soft-delete vs 하드 삭제 (선택 이유: 감사/규제 대응)
     - 마스킹 필드 목록 (email, displayName, password)
     - GDPR "right to be forgotten" 대응 계획 (미래)
     - 복구 가능 기간 (DEACTIVATED) vs 복구 불가능 (DELETED)

3. 포맷:
   - Markdown 표 사용 (상태 전이 매트릭스)
   - 코드 예시 (마스킹 로직 의사 코드)
   - 감사 추적 예시 (USER_DELETED 이벤트)

4. 검증 체크리스트:
   - [ ] 4개 상태 모두 설명
   - [ ] 각 전이의 사전/사후 조건 명시
   - [ ] 마스킹 규칙 명확 (before/after 예시)
   - [ ] 규제 대응 계획 언급 (GDPR/CCPA)

## Acceptance Criteria
- [ ] `docs/domain-rules.md` User 섹션 추가/확장 완료
- [ ] `docs/policies.md` 사용자 프라이버시 정책 추가 완료
- [ ] 상태 전이 다이어그램 또는 표 포함
- [ ] 마스킹 예시 (before/after) 포함
- [ ] 감사 이벤트 목록 포함 (USER_CREATED, USER_SUSPENDED, USER_DELETED 등)
- [ ] 문서 내 일관성 검증 (다른 도메인과 포맷 일치)
- [ ] PR 또는 코드 검토 패스

## Verification
`./scripts/verify-task.sh T-007`
예상 결과: docs lint ✓ / markdown ✓

## Rollback
```bash
git checkout -- docs/domain-rules.md
git checkout -- docs/policies.md
```

## Notes
- **User 상태 머신** (상태 전이 표):
  ```
  ACTIVE
    ├─→ DEACTIVATED (사용자 요청)
    ├─→ SUSPENDED (운영자 정책)
    └─→ DELETED (영구 삭제)
  DEACTIVATED
    ├─→ ACTIVE (복구)
    └─→ DELETED (영구 삭제)
  SUSPENDED
    ├─→ ACTIVE (운영자 해제)
    └─→ DELETED (영구 삭제)
  DELETED (terminal state)
  ```

- **마스킹 규칙**:
  - email: `deleted_<user_id>@deleted.local`
  - displayName: `Deleted User #<user_id>`
  - password: NULL (이미 해시됨, 클리어 불필요)
  - created_at_utc, updated_at_utc: 그대로 유지 (감사)

- **쿼리 필터링**:
  - User 조회 시 기본적으로 DELETED 제외 (선택적 `includDeleted` 플래그)
  - 감사/리포트 시에는 모든 상태 포함

- **향후 규제 대응** (GDPR "right to be forgotten"):
  - 완전 개인정보 삭제 (hard-delete) vs soft-delete의 구분
  - 규제 대응 프로세스 정의 필요 (별도 타스크)

- 기존 문서와 링크: `docs/domain-rules.md` §Explicit State Transition Rules에 User 전이 추가
- T-005 완료 후 시작 권장 (도메인 로직 확정 후)

## Review log
- 2026-04-26 — domain-rules.md User Entity & Lifecycle 추가, policies.md §5.1 User Privacy 추가
