# T-002 — [BE] auth — Refresh token 로테이션 race condition 해결

## Meta
- ID: T-002
- Milestone: M1 (인증·탐색)
- Domain: auth
- Area: BE
- Layer: domain + adapter.out
- Size: L (~6h)
- Depends on: 없음
- Blocks: 없음
- GitHub Issue: #18 (생성 전)
- Status: Backlog

## 파일 소유권
WRITE:
  - `src/main/kotlin/com/demo/tourwave/domain/auth/AuthRefreshToken.kt` (version 필드 추가)
  - `src/main/kotlin/com/demo/tourwave/adapter/out/persistence/auth/JpaAuthRefreshTokenRepositoryAdapter.kt` (rotate 로직 수정)
  - `db/migration/V<timestamp>__add_refresh_token_version.sql` (Flyway 마이그레이션)
  - `src/test/kotlin/com/demo/tourwave/domain/auth/AuthRefreshTokenTest.kt` (version 테스트)
  - `src/test/kotlin/com/demo/tourwave/adapter/out/persistence/auth/MysqlAuthRefreshTokenRaceTest.kt` (동시성 테스트)

READ:
  - `src/main/kotlin/com/demo/tourwave/application/auth/AuthTokenLifecycleService.kt` (rotate 호출)
  - `src/main/kotlin/com/demo/tourwave/adapter/out/persistence/auth/AuthRefreshTokenJpaEntity.kt` (JPA 구조)
  - `docs/audit/BE-auth.md` §관찰된 문제 #2

DO NOT TOUCH:
  - `src/main/kotlin/com/demo/tourwave/adapter/in/web/auth/AuthController.kt` (엔드포인트 기존)
  - `src/main/kotlin/com/demo/tourwave/application/auth/JwtTokenService.kt` (JWT 로직)

## SSOT 근거
- `docs/audit/BE-auth.md` §관찰된 문제 #2: "Refresh Token 로테이션 현재 rotate() 구현이 기존 토큰 철회 후 신규 발급하므로 동시 요청에 race condition 위험"
- `docs/domain-rules.md` §Core Principle: "트랜잭션 경계는 application에서 관리; 동시성 제어는 행 락 먼저 획득"

## 현재 상태 (갭)
- [ ] `AuthRefreshToken` 도메인 엔티티에 version 필드 없음
- [ ] `AuthTokenLifecycleService.rotate()` 구현이 기존 토큰 조회 → 철회 → 신규 발급 순서이므로 동시 요청 시 문제 발생 가능
- [ ] 동시 요청에 대한 테스트 커버리지 없음

## 구현 지침
1. `AuthRefreshToken` domain entity에 `version: Long` 필드 추가 (Optimistic Locking)
2. Flyway 마이그레이션: `refresh_tokens` 테이블에 `version` 컬럼 추가 (DEFAULT 0)
3. `JpaAuthRefreshTokenRepositoryAdapter.rotate(oldToken)` 로직 수정:
   - 기존: revoke(oldToken) → issue(new)
   - 신규: SELECT ... FOR UPDATE on oldToken by version → 버전 불일치 시 OptimisticLockException → 재시도 (클라이언트)
4. `AuthTokenLifecycleService.rotate(refreshToken)` 예외 처리: OptimisticLockException → DomainException(REFRESH_TOKEN_ROTATION_CONFLICT)
5. 동시성 테스트: 2개 이상의 동시 rotate() 호출 시나리오 (Testcontainers + 10개 스레드 병렬)
6. 클라이언트 재시도: AuthController에서 409 Conflict 응답 (재시도 헤더 포함)

## Acceptance Criteria
- [ ] `AuthRefreshToken.version` 필드 추가 완료
- [ ] Flyway 마이그레이션 작성 및 적용 완료
- [ ] `rotate()` 구현이 OptimisticLocking 사용 확인
- [ ] 동시 rotate() 시뮬레이션 테스트 통과 (2+ threads, no data loss)
- [ ] `./gradlew test --tests "*AuthRefreshTokenTest"` 통과
- [ ] `./gradlew test --tests "*MysqlAuthRefreshTokenRaceTest"` 통과
- [ ] 기존 `AuthCommandServiceTest` 재귀 확인 (409 응답 처리)

## Verification
`./scripts/verify-task.sh T-002`
예상 결과: build ✓ / test ✓ / lint ✓ / migration ✓ / docs ✓

## Rollback
```bash
git checkout -- src/main/kotlin/com/demo/tourwave/domain/auth/AuthRefreshToken.kt
git checkout -- src/main/kotlin/com/demo/tourwave/adapter/out/persistence/auth/
git checkout -- src/test/kotlin/com/demo/tourwave/domain/auth/AuthRefreshTokenTest.kt
git checkout -- src/test/kotlin/com/demo/tourwave/adapter/out/persistence/auth/MysqlAuthRefreshTokenRaceTest.kt
# 마이그레이션 롤백은 별도 (수동 확인 필요)
./gradlew clean test
```

## Notes
- OptimisticLocking은 JPA의 @Version 어노테이션 사용 (자동 관리)
- 409 Conflict는 명시적 재시도를 위한 신호 (Exponential backoff 권장)
- MySQL의 SELECT ... FOR UPDATE는 사용하지 않음 (JPA Optimistic Lock으로 통일)
- 향후 Redis 기반 토큰 저장소로 전환 시에도 version 필드 개념 유지
