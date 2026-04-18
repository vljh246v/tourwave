# 감사 인덱스

마지막 갱신: 2026-04-18

이 디렉토리는 Phase 1 감사 산출물이다. BE/FE 각 도메인의 현재 구현 스냅샷.

## BE 바운디드 컨텍스트 (16개)

| 도메인 | 구현 | 테스트 | OpenAPI path | 문서 |
|---|---|---|---|---|
| announcement | 🟡 | 🟡 | Announcements | [BE-announcement](BE-announcement.md) |
| asset | 🟡 | 🟡 | Assets | [BE-asset](BE-asset.md) |
| auth | ✅ | ✅ | Auth | [BE-auth](BE-auth.md) |
| booking | ✅ | ✅ | Bookings (14) | [BE-booking](BE-booking.md) |
| common | ✅ | 🟡 | N/A | [BE-common](BE-common.md) |
| customer | ✅ | 🟡 | Favorites/Notifications/Notes | [BE-customer](BE-customer.md) |
| inquiry | ✅ | ✅ | Inquiries (6) | [BE-inquiry](BE-inquiry.md) |
| instructor | 🟡 | 🟡 | Instructors | [BE-instructor](BE-instructor.md) |
| occurrence | ✅ | 🟡 | Occurrences/Calendar/Waitlist | [BE-occurrence](BE-occurrence.md) |
| operations | ✅ | ✅ | Operations | [BE-operations](BE-operations.md) |
| organization | ✅ | ✅ | Organizations | [BE-organization](BE-organization.md) |
| participant | ✅ | ✅ | Participants | [BE-participant](BE-participant.md) |
| payment | 🟡 | 🟡 | Payments/Finance | [BE-payment](BE-payment.md) |
| review | ✅ | ✅ | Reviews | [BE-review](BE-review.md) |
| tour | 🟡 | 🟡 | Tours | [BE-tour](BE-tour.md) |
| user | 🟡 | 🟡 | Me | [BE-user](BE-user.md) |

## FE OpenAPI 태그 (23개 + 통합요약)

전체 시드 상태 — 구현·테스트 대부분 ❌. 상세는 각 파일.

| 태그 | 문서 |
|---|---|
| Announcements | [FE-announcements](FE-announcements.md) |
| Assets | [FE-assets](FE-assets.md) |
| Auth | [FE-auth](FE-auth.md) |
| Bookings | [FE-bookings](FE-bookings.md) |
| Calendar | [FE-calendar](FE-calendar.md) |
| Favorites | [FE-favorites](FE-favorites.md) |
| Finance | [FE-finance](FE-finance.md) |
| Inquiries | [FE-inquiries](FE-inquiries.md) |
| Instructors | [FE-instructors](FE-instructors.md) |
| Me | [FE-me](FE-me.md) |
| Notes | [FE-notes](FE-notes.md) |
| Notifications | [FE-notifications](FE-notifications.md) |
| Occurrences | [FE-occurrences](FE-occurrences.md) |
| Operations | [FE-operations](FE-operations.md) |
| Organizations | [FE-organizations](FE-organizations.md) |
| Participants | [FE-participants](FE-participants.md) |
| Payments | [FE-payments](FE-payments.md) |
| Policies | [FE-policies](FE-policies.md) |
| Reports | [FE-reports](FE-reports.md) |
| Reviews | [FE-reviews](FE-reviews.md) |
| Search | [FE-search](FE-search.md) |
| Tours | [FE-tours](FE-tours.md) |
| Waitlist | [FE-waitlist](FE-waitlist.md) |
| **전체 요약** | [FE-SUMMARY](FE-SUMMARY.md) |

## 주요 발견 (Phase 2 입력)

### 🔴 고위험
- **Refresh token 로테이션 race condition** (auth) — 분산 환경 취약
- **Occurrence 용량 무결성** — 동시 예약 생성 시 동시성 제어 약함
- **Asset Content-Type 검증 없음** — 위험 파일 타입 필터링 부재 (보안)
- **User ApplicationService 부재** — 계층 격리 불완전

### 🟡 중위험
- **Payment Provider 어댑터 미구현** — PaymentProviderPort만 정의, Stripe/Tosspayments 연동 대기, Capture 프로세스 미존재
- **Idempotency-Key 부분 미구현** — announcement, operations, organization, instructor
- **감사 이벤트 미기록** — announcement, organization, instructor, tour
- **CommunicationReportingIntegrationTest** — main에서 실패 (이 브랜치 원인 아님, 회귀 추정)
- **통합 테스트 결합** — InstructorAndTourControllerIntegrationTest 분리 권장
- **Password reset 이메일 발송 미구현** — NotificationChannelPort 배송 구현 부재
- **Favorite 유니크 제약 미지원**
- **iCal RFC 5545 준수 검증 부재**

### 🟢 FE 전반
- 141개 BE 엔드포인트 (재카운트 필요, openapi.yaml 103 path 기준) 대비 FE 0% 구현
- 크로스컷팅 프리쿼지트 5종: API 클라이언트, schema.ts 생성, Auth 모듈, 인증 훅, 라우트 가드
- 우선 도메인: Auth → 공개 카탈로그 → 예약 흐름 → Operator 대시보드

## 다음 단계

Phase 2 — 갭 매트릭스 생성 (`docs/gap-matrix.md`). 이 인덱스 + 각 도메인 파일을 근거로 ❌/🟡 셀에 향후 태스크 ID를 매핑.
