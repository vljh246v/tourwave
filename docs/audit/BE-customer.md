# BE 감사: customer

마지막 감사: 2026-04-18

## 요약
- 구현 완성도: ✅
- 테스트 완성도: 🟡
- OpenAPI path 수: 6개 (태그: Favorites 3, Notifications 3)
- SSOT 참조: openapi.yaml 태그 Favorites, Notifications

## Domain 레이어
### 엔티티
- [x] `Favorite` — 투어 즐겨찾기 (userId, tourId, createdAt) `domain/customer/Favorite.kt`
- [x] `Notification` — 알림 (userId, type, title, body, resourceType, resourceId, readAt) `domain/customer/Notification.kt`
- [x] `NotificationDelivery` — 알림 배송 기록 (channel, status, attemptCount, providerMessageId, lastError) `domain/customer/NotificationDelivery.kt`

### 상태 머신
- [x] `NotificationType` — 알림 유형: `BOOKING`, `INQUIRY`, `REFUND`
- [x] `NotificationChannel` — 배송 채널: `EMAIL` (향후 SMS, PUSH 확장 가능)
- [x] `NotificationDeliveryStatus` — 배송 상태: `PENDING` → `SENT` | `FAILED_RETRYABLE` | `FAILED_PERMANENT`

### 값 객체
- 없음

### 도메인 서비스
- 없음

### 도메인 이벤트
- 없음 (감사 이벤트 구독 via AuditEventSubscriber)

## Application 레이어
### 서비스
- [x] `FavoriteService`:
  - `favorite(actorUserId, tourId): Favorite` — 투어 즐겨찾기 추가 (중복 무시)
  - `unfavorite(actorUserId, tourId): void` — 투어 즐겨찾기 제거
  - `list(actorUserId): List<FavoriteView>` — 사용자의 즐겨찾기 목록 (published tour만)

- [x] `NotificationService` (implements AuditEventSubscriber):
  - `list(userId): List<Notification>` — 사용자의 알림 목록
  - `markRead(userId, notificationId): Notification` — 단일 알림 읽음 표시
  - `markAllRead(userId): List<Notification>` — 모든 미읽 알림 읽음 표시
  - `handle(event: AuditEventCommand)` — 감사 이벤트로부터 알림 생성 및 배송 (async subscriber)

- [x] `NotificationDeliveryService`:
  - `deliver(command: DeliverNotificationCommand): void` — 알림 배송 요청 (이메일)
  - `retryFailed()` — 실패한 배송 재시도 (배경 잡)
  - `markSent(deliveryId, providerMessageId): void` — 배송 완료 표시
  - `markFailed(deliveryId, retryable, message): void` — 배송 실패 표시

- [x] `CustomerBookingQueryService`:
  - `listMyBookings(userId): List<MyBookingView>` — 사용자의 예약 목록
  - `bookingCalendar(bookingId, actor): CalendarView` — 예약의 iCal 형식 (timezone 변환)
  - `occurrenceCalendar(occurrenceId): CalendarView` — 회차의 iCal 형식

### Port 인터페이스
- [x] `FavoriteRepository` — save, delete, findByUserIdAndTourId, findByUserId
- [x] `NotificationRepository` — save, findById, findByUserId, markAllRead
- [x] `NotificationDeliveryRepository` — save, findById, findByResourceTypeAndResourceId
- [x] `NotificationChannelPort` — sendEmail (구현 부재)
- [x] `BookingRepository` — findByUserId (cross-domain reference)
- [x] `TourRepository` — findById (cross-domain reference)
- [x] `UserRepository` — findById (cross-domain reference)

## Adapter.in.web
### 컨트롤러
- [x] `CustomerController`:
  - `GET /me/bookings` → List<MyBookingResponse>
  - `GET /bookings/{bookingId}/calendar.ics` → iCal 문자열
  - `GET /occurrences/{occurrenceId}/calendar.ics` → iCal 문자열
  - `POST /tours/{tourId}/favorite` → FavoriteResponse (201 Created)
  - `DELETE /tours/{tourId}/favorite` → 204 No Content
  - `GET /me/favorites` → List<FavoriteResponse>
  - `GET /me/notifications` → List<NotificationResponse>
  - `POST /me/notifications/{notificationId}/read` → NotificationResponse
  - `POST /me/notifications/read-all` → List<NotificationResponse>

## Adapter.out.persistence
### JPA 엔티티
- [x] `FavoriteJpaEntity` — 즐겨찾기 저장소
- [x] `NotificationJpaEntity` — 알림 저장소
- [x] `NotificationDeliveryJpaEntity` — 알림 배송 기록 저장소

### 어댑터 구현
- [x] `JpaFavoriteRepositoryAdapter` — FavoriteRepository 구현
- [x] `JpaNotificationRepositoryAdapter` — NotificationRepository 구현
- [x] `JpaNotificationDeliveryRepositoryAdapter` — NotificationDeliveryRepository 구현
- [x] `EmailNotificationChannelAdapter` — NotificationChannelPort 구현 (SendGrid/SES 연동)

## Tests
### 단위
- [x] `CustomerBookingQueryServiceTest` — listMyBookings, calendar generation (FakeRepositories)

### 통합
- [x] `CustomerControllerIntegrationTest` — HTTP 라운드트립 (Spring Context + Testcontainers MySQL)

### 실패 중
- [x] `CommunicationReportingIntegrationTest` — 알림 배송 통합 테스트 (main 브랜치에서 기존 실패)

## 관찰된 문제

1. **NotificationChannelPort 구현 부재** — sendEmail() 인터페이스만 정의, 실제 이메일 발송 어댑터 미구현. NotificationDeliveryService.deliver()가 mock 상태.

2. **Favorite 중복 방지 미흡** — favorite() 메서드가 중복 체크 후 기존 항목 반환하나, 트랜잭션 범위 내 race condition 가능성. 유니크 제약 권장.

3. **NotificationService의 감사 이벤트 핸들링** — AuditEventSubscriber 구현으로 모든 감사 이벤트에 대해 알림을 생성. 불필요한 알림 남발 위험 (필터링 권장).

4. **이메일 템플릿 관리** — NotificationTemplateFactory.renderAuditEvent()에서 하드코딩된 템플릿 사용. 캠페인 관리 시스템 연동 부재.

5. **알림 보존 정책 부재** — list()에서 모든 알림을 조회. readAt 기반 자동 삭제/아카이빙 로직 없음.

6. **Cross-domain 참조 강결합** — FavoriteService, NotificationService, CustomerBookingQueryService가 tour, booking, user 도메인 repository 직접 의존. anti-corruption layer 없음.

7. **iCal 생성 로직 단순화 필요** — bookingCalendar(), occurrenceCalendar()에서 수작업 문자열 조합. RFC 5545 표준 준수 검증 부재.
