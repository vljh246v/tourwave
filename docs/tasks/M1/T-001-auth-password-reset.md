---
id: T-001
title: "[BE] auth — Password reset 이메일 발송 구현"
aliases: [T-001]

repo: tourwave
area: be
milestone: M1
domain: auth
layer: application
size: M
status: done

depends_on: []
blocks: []
sub_tasks: []

github_issue: null
exec_plan: ""

created: 2026-04-18
updated: 2026-04-26
---

#status/done #area/be

# T-001 — [BE] auth — Password reset 이메일 발송 구현

> GitHub Issue: #17 (생성 전 — 예약 번호)

## 파일 소유권
WRITE:
  - `src/main/kotlin/com/demo/tourwave/application/auth/PasswordResetService.kt` (신규)
  - `src/main/kotlin/com/demo/tourwave/application/auth/NotificationChannelPort.kt` (신규 Port)
  - `src/test/kotlin/com/demo/tourwave/application/auth/PasswordResetServiceTest.kt` (신규)
  - `src/main/kotlin/com/demo/tourwave/adapter/out/notification/` (이메일 어댑터, 신규)

READ:
  - `src/main/kotlin/com/demo/tourwave/application/auth/AuthCommandService.kt` (requestPasswordReset 지점)
  - `src/main/kotlin/com/demo/tourwave/domain/auth/UserActionToken.kt` (토큰 구조)
  - `docs/policies.md` (알림 정책)

DO NOT TOUCH:
  - `src/main/kotlin/com/demo/tourwave/domain/auth/` (domain 엔티티 수정 불가)
  - `src/main/kotlin/com/demo/tourwave/adapter/in/web/` (컨트롤러 기존 로직)

## SSOT 근거
- `docs/audit/BE-auth.md` §관찰된 문제 #3: "Password Reset 이메일 검증 부재 — requestPasswordReset()은 이메일 존재 여부만 확인하고 실제 이메일 발송 구현 부재"
- `docs/policies.md` §4.3: 인증 정책 행렬에서 password reset flow 명시
- `docs/domain-rules.md` §Audit Log Policy: 비밀번호 리셋 요청/확정 감사 이벤트 필수 기록

## 현재 상태 (갭)
- [ ] `AuthCommandService.requestPasswordReset(email)` 호출 후 실제 이메일 발송 로직 없음 (only DB lookup)
- [ ] `NotificationChannelPort` 인터페이스 정의 부재 (이메일 어댑터 추상화 필요)
- [ ] 비밀번호 리셋 이메일 템플릿 및 발송 로직 미구현
- [ ] 이메일 발송 실패 시 재시도/로깅 정책 미정의

## 구현 지침
1. `NotificationChannelPort` 인터페이스 정의 (메서드: `send(recipient, subject, body, templateType)`)
2. `PasswordResetService` 생성 (의존성: `NotificationChannelPort`, `UserActionTokenService`, `UserRepository`)
3. `AuthCommandService.requestPasswordReset(email)`을 `PasswordResetService` 호출로 위임
4. 비밀번호 리셋 토큰을 포함한 이메일 본문 생성 (토큰 만료 시간, 확정 URL 포함)
5. 이메일 발송 실패 시 DomainException → HTTP 500 응답 (재시도는 클라이언트 담당)
6. 이메일 발송 감사 이벤트 기록 (AuditEventPort.append with action="PASSWORD_RESET_EMAIL_SENT")
7. 단위 테스트: FakeNotificationChannelPort, FakeRepositories 기반 (Spring 불필요)

## Acceptance Criteria
- [ ] `NotificationChannelPort` 정의 완료
- [ ] `PasswordResetService` 구현 완료 (send 호출 위임)
- [ ] `AuthCommandService.requestPasswordReset` 통합 완료
- [ ] 이메일 본문에 (a) 토큰, (b) 만료 시간, (c) 확정 링크 포함
- [ ] 발송 실패 시 적절한 예외 처리
- [ ] `./gradlew test --tests "*PasswordResetServiceTest"` 통과
- [ ] `./gradlew test --tests "*AuthCommandServiceTest*"` 통과 (기존 테스트 회귀 확인)

## Verification
```bash
./scripts/verify-task.sh T-001
```
예상 결과: build ✓ / test ✓ / lint ✓ / security ✓ / docs ✓

## Rollback
```bash
git checkout -- src/main/kotlin/com/demo/tourwave/application/auth/PasswordResetService.kt
git checkout -- src/main/kotlin/com/demo/tourwave/application/auth/NotificationChannelPort.kt
git checkout -- src/main/kotlin/com/demo/tourwave/application/auth/AuthCommandService.kt
git clean -fd src/main/kotlin/com/demo/tourwave/adapter/out/notification/
git clean -fd src/test/kotlin/com/demo/tourwave/application/auth/PasswordResetServiceTest.kt
./gradlew clean test
```

## Notes
- `UserActionTokenService.issue(userId, PASSWORD_RESET, ttl)` 패턴 재사용
- 이메일 템플릿은 하드코드 또는 classpath 리소스로 관리 (향후 i18n 고려)
- 실제 SMTP/SES 어댑터는 T-207 에서 구현; 현재는 인터페이스만 정의
- 감사 이벤트는 "PASSWORD_RESET_EMAIL_SENT" 액션으로 기록 (user 식별 시점 고려)
