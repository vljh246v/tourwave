# BE 감사: auth

마지막 감사: 2026-04-18

## 요약
- 구현 완성도: ✅
- 테스트 완성도: ✅
- OpenAPI path 수: 8개 (태그: Auth)
- SSOT 참조: openapi.yaml 태그 Auth

## Domain 레이어
### 엔티티
- [x] `AuthRefreshToken` — JWT refresh token (활성, 만료, 철회 상태) `domain/auth/AuthRefreshToken.kt`
- [x] `UserActionToken` — 이메일 인증/비밀번호 리셋/조직 초대 용도의 일회용 토큰 `domain/auth/UserActionToken.kt`

### 상태 머신
- [x] `UserActionTokenPurpose` — 용도: `EMAIL_VERIFICATION`, `PASSWORD_RESET`, `ORG_INVITATION`
- [x] 토큰 생명주기: `active` (issueAtUtc < now < expiresAtUtc, consumedAtUtc=null) → `consumed` | `expired`

### 값 객체
- 없음 (향후 Email, Password 값 객체 추가 고려)

### 도메인 서비스
- 없음

### 도메인 이벤트
- 없음 (감사 이벤트는 application 에서 발행)

## Application 레이어
### 서비스
- [x] `AuthCommandService`:
  - `signup(displayName, email, password): AuthResult` — 회원가입 (이메일 검증 토큰 발행)
  - `login(email, password): AuthResult` — 로그인 (JWT 발급)
  - `refresh(refreshToken): AuthResult` — 토큰 갱신 (refresh token → new access + refresh)
  - `logout(userId): void` — 로그아웃 (refresh token 전부 철회)
  - `requestEmailVerification(userId): void` — 이메일 인증 요청
  - `confirmEmailVerification(token): void` — 이메일 인증 확정 (감사 이벤트 기록)
  - `requestPasswordReset(email): void` — 비밀번호 리셋 요청 (이메일 검증)
  - `confirmPasswordReset(token, newPassword): void` — 비밀번호 리셋 확정 (모든 refresh token 철회, 감사 이벤트)
  - `deactivate(userId): void` — 계정 비활성화 (DEACTIVATED, 토큰 철회, 감사 이벤트)

- [x] `JwtTokenService`:
  - `issueAccessToken(userId, roles, orgId): String` — HS256 JWT 발급 (TTL 설정 가능)
  - `parse(token): AccessTokenClaims` — JWT 파싱 및 서명 검증

- [x] `AuthTokenLifecycleService`:
  - `issueRefreshToken(userId): String` — refresh token 생성 및 저장
  - `rotate(refreshToken): AuthRefreshToken` — 토큰 로테이션 (기존 철회, 신규 발급)
  - `revokeAll(userId): void` — 사용자의 모든 refresh token 철회

- [x] `UserActionTokenService`:
  - `issue(userId, purpose, ttl): UserActionToken` — 액션 토큰 발행
  - `consume(token, purpose): UserActionToken` — 토큰 소비 및 목적 검증

### Port 인터페이스
- [x] `AuthRefreshTokenRepository` — save, findById, findByUserId, revokeAllForUser
- [x] `UserActionTokenRepository` — save, findByTokenHash, revokeAllForUser
- [x] `PasswordHasher` — hash, matches (구현: BCrypt)
- [x] `AuditEventPort` — append (감사 이벤트 발행)

## Adapter.in.web
### 컨트롤러
- [x] `AuthController`:
  - `POST /auth/signup` — SignupRequest → AuthResponse (토큰 + 사용자)
  - `POST /auth/login` — LoginRequest → AuthResponse
  - `POST /auth/refresh` — RefreshRequest → AuthResponse
  - `POST /auth/logout` — X-Actor-User-Id 헤더로 인증
  - `POST /auth/email/verify-request` — 인증된 사용자 (X-Actor-User-Id)
  - `POST /auth/email/verify-confirm` — token 제출
  - `POST /auth/password/reset-request` — email 제출 (인증 불필요, 이메일 검증)
  - `POST /auth/password/reset-confirm` — token + newPassword 제출

- [x] `MeController`:
  - `GET /me` → MeResponse (user + memberships)
  - `PATCH /me` — displayName 변경
  - `POST /me/deactivate` — 계정 삭제

## Adapter.out.persistence
### JPA 엔티티
- [x] `AuthRefreshTokenJpaEntity` — refresh token 저장소
- [x] `UserActionTokenJpaEntity` — action token 저장소

### 어댑터 구현
- [x] `JpaAuthRefreshTokenRepositoryAdapter` — AuthRefreshTokenRepository 구현
- [x] `JpaUserActionTokenRepositoryAdapter` — UserActionTokenRepository 구현
- [x] `BcryptPasswordHasher` — PasswordHasher 구현

## Tests
### 단위
- [x] `AuthCommandServiceTest` — signup, login, refresh, logout, email verification, password reset, deactivate (FakeRepositories 기반)
- [x] `UserActionTokenServiceTest` — issue, consume, expiry, invalid purpose

### 통합
- [x] `AuthControllerIntegrationTest` — HTTP 라운드트립 (Spring Context + Testcontainers MySQL)

### 실패 중
- 없음

## 구현 완료 (T-002)

**T-002 작업 (2026-04-26):** Refresh token rotation race condition 해결
- OptimisticLock(@Version) 구현 추가: `AuthRefreshToken.version` 필드 및 JPA @Version 매핑
- `rotate()` + `revokeAll()` 패턴: `AuthTokenLifecycleService.rotate()`가 해당 토큰 rotate 후 동일 userId의 다른 모든 토큰 철회
- 보안 정책 일관성: logout=revokeAll, refresh=rotate+revokeAll 둘 다 모든 토큰 철회
- 에러 메시지 동시성 힌트 제거 완료 (M-2)
- MEDIUM 이슈 해소 완료

## 관찰된 문제

1. **토큰 소비 멱등성** — `confirm*` 엔드포인트에서 Idempotency-Key 검증 미지원. 같은 토큰 2회 제출 시 2번 소비 가능.

2. **Password Reset 이메일 검증 부재** — requestPasswordReset()은 이메일 존재 여부만 확인하고 실제 이메일 발송 구현 부재. NotificationDeliveryService와 통합 필요.

3. **UserStatus.DELETED 미사용** — User 엔티티가 DELETED 상태를 정의했으나 애플리케이션에서 사용하지 않음. deactivate만 구현됨.

4. **AccessTokenClaims 검증 부재** — JWT parse 후 claim 유효성 (iat, exp 범위) 검증이 기본 범위 초과/이전 체크만 함. clock skew 고려 권장.

5. **ORG_INVITATION 목적 미구현** — UserActionTokenPurpose enum에 정의했으나 signup/reset 외 사용 코드 없음.
