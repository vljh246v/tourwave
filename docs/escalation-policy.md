# Escalation Policy — 사람 승인이 필요한 변경

에이전트가 **절대 단독으로 진행하면 안 되는** 변경 목록입니다.

---

## 고위험 카테고리

### 1. DB 스키마 변경 (최우선)

- Flyway 마이그레이션 파일 신규 생성/수정 (`src/main/resources/db/migration/`)
- JPA 엔티티 필드/테이블 변경이 DDL에 영향을 줄 때
- 인덱스 추가/삭제

> 이유: 프로덕션 마이그레이션은 되돌리기 어렵고 데이터 손실로 이어질 수 있음

### 2. 인증 / 인가 로직

- JWT 토큰 생성/검증 로직 변경
- Spring Security 필터 체인 변경
- 역할(Role) 기반 권한 매트릭스 변경 (`docs/policies.md#인증-역할-매트릭스` 참고)
- `AuthConfig`, `JwtFilter` 관련 코드

### 3. 예약 상태 머신 변경

- 예약 상태 전이 로직 변경 (`domain/booking` 레이어)
- 터미널 상태(REJECTED, EXPIRED, CANCELED, COMPLETED) 관련 로직
- `docs/domain-rules.md` 에 정의된 불변식에 영향을 주는 변경

> 이유: 상태 머신 오류는 데이터 일관성 파괴로 직결됨

### 4. 결제 / 환불 로직

- 환불 정책 계산 로직 변경 (`docs/domain-rules.md#환불-규칙` 참고)
- 결제 외부 API 연동 코드

### 5. 아이덤포턴시 정책

- `Idempotency-Key` 처리 로직
- 동일 키 + 다른 payload → 422 처리 관련 코드

### 6. 빌드 / 배포

- `build.gradle.kts` 의존성 major 버전 변경
- CI/CD 파이프라인 변경

### 7. 하네스 자체 변경

- 검증기 비활성화 (`ENABLE_*_CHECK=false`)
- `harness.config.sh`, `golden-principles.md`, `CLAUDE.md` 수정
- `.claude/hooks/` 비활성화

---

## 절차

1. **exec-plan 작성**: `docs/exec-plans/active/` 에 포함:
   - **Impact**: 영향 범위
   - **Rollback**: 롤백 방법
   - **Verification**: 정상 동작 확인 방법

2. **사람 승인** 받기

3. **승인 후** 워크트리에서 구현 → `task-finish.sh`

---

## 참고

- 도메인 규칙: `docs/domain-rules.md`
- 정책: `docs/policies.md`
- 실패 기록: `docs/agent-failures.md`
