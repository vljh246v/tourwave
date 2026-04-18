# BE 감사: occurrence

마지막 감사: 2026-04-18

## 요약
- 구현 완성도: ✅
- 테스트 완성도: 🟡
- OpenAPI path 수: 10개 (태그: Occurrences 8, Calendar 2)
- SSOT 참조: openapi.yaml 태그 Occurrences, Calendar

## Domain 레이어
### 엔티티
- [x] `Occurrence` — 투어 회차 (tourId, capacity, startsAtUtc, endsAtUtc, timezone, unitPrice, locationText, meetingPoint) `domain/occurrence/Occurrence.kt`

### 상태 머신
- [x] `OccurrenceStatus` — 상태: `SCHEDULED`, `CANCELED`, `FINISHED`

### 값 객체
- 없음 (향후 Money, Timezone 값 객체 추가 권장)

### 도메인 서비스
- 없음

### 도메인 이벤트
- 없음 (상태 변경 감사는 application 에서 발행)

## Application 레이어
### 서비스
- [x] `OccurrenceCommandService`:
  - `create(command: CreateOccurrenceCommand): Occurrence` — 회차 생성 (용량 검증, 강사 검증, 시간대 검증)
  - `update(command: UpdateOccurrenceCommand): Occurrence` — 회차 업데이트 (기본정보: 강사, 용량, 시간, 타임존, 위치)
  - `reschedule(command: RescheduleOccurrenceCommand): Occurrence` — 회차 재예약 (시간, 위치 변경만, 미래 시간 필수)
  - `cancel(command: CancelOccurrenceCommand): Occurrence` — 회차 취소 (상태 → CANCELED)
  - `finish(command: FinishOccurrenceCommand): Occurrence` — 회차 완료 (상태 → FINISHED)

- [x] `CatalogQueryService`:
  - `listPublished(filter: OccurrenceCatalogFilter): List<PublishedOccurrenceView>` — 공개 회차 목록 (검색, 필터, 페이징)
  - `getPublishedDetail(occurrenceId): PublishedOccurrenceDetailView` — 회차 상세조회

- [x] `OccurrenceValidation` (utility):
  - `requireValidOccurrenceWindow(startsAtUtc, endsAtUtc)` — 시작 < 종료 검증
  - `requireValidOccurrenceCapacity(capacity)` — 용량 1-9999 검증
  - `requireValidUnitPrice(unitPrice)` — 가격 0-10000000 검증
  - `requireValidCurrency(currency)` — 통화 코드 검증 (KRW, USD 등)

### Port 인터페이스
- [x] `OccurrenceRepository` — save, findById, nextId
- [x] `TourRepository` — findById (tour 검증)
- [x] `InstructorProfileRepository` — findById (강사 검증)
- [x] `BookingRepository` — findByOccurrenceId (용량 체크)
- [x] `OrganizationAccessGuard` — requireOperator (권한 검증)

## Adapter.in.web
### 컨트롤러
- [x] `OccurrenceOperatorController` (운영진 전용):
  - `POST /tours/{tourId}/occurrences` → OccurrenceResponse (201 Created)
  - `PATCH /occurrences/{occurrenceId}` → OccurrenceResponse
  - `POST /occurrences/{occurrenceId}/reschedule` → OccurrenceResponse

- [x] `OccurrencePublicController` (공개):
  - `GET /tours/{tourId}/occurrences` → List<PublishedOccurrenceResponse>
  - `GET /occurrences/{occurrenceId}` → PublishedOccurrenceDetailResponse
  - `POST /occurrences/{occurrenceId}/cancel` — 취소 (운영진만)
  - `POST /occurrences/{occurrenceId}/finish` — 완료 (운영진만)

## Adapter.out.persistence
### JPA 엔티티
- [x] `OccurrenceJpaEntity` — 회차 저장소
- [x] `PublishedOccurrenceViewJpa` — 공개 조회용 뷰/쿼리 최적화

### 어댑터 구현
- [x] `JpaOccurrenceRepositoryAdapter` — OccurrenceRepository 구현
- [x] `JpaCatalogQueryAdapter` — CatalogQueryService 구현 (동적 쿼리, 페이징)

## Tests
### 단위
- [x] `OccurrenceCommandServiceTest` — create, update, reschedule, cancel, finish (FakeRepositories)
  - 용량 검증, 시간대 검증, 시작 시간 미래 필수, 기존 예약 용량 제약

### 통합
- [x] `OccurrenceCatalogControllerIntegrationTest` — HTTP 라운드트립 (Spring Context + Testcontainers MySQL)

### 실패 중
- [x] `OccurrenceCatalogControllerIntegrationTest` — main 브랜치에서 기존 실패 (이 브랜치 원인 아님)

## 관찰된 문제

1. **무결성 제약 미흡** — 회차 용량(capacity)과 기존 예약의 합(confirmedSeats + offeredSeats)이 불변식으로 선언되나, 트랜잭션 내 동시성 제어 부재. 예약 생성 중 회차 용량 감소 시 race condition 가능.

2. **타임존 변환 일관성** — Occurrence.timezone과 시간 경계 규칙(초대 6h 차단, offer 만료 48h) 계산이 application 레이어 임시 로직에 의존. DB 타임존 함수 사용 금지 정책 준수하되, 중앙화된 conversion utility 필요.

3. **회차 취소/완료 상태전이 검증 부재** — cancel(), finish() 메서드가 OccurrenceStatus enum만 사용. 터미널 상태(CANCELED, FINISHED)로의 전이 후 추가 전이 금지 로직 없음.

4. **회차 재예약 제약 약함** — reschedule()은 "미래 시간 필수"만 검증하고, 이미 시작한 회차(startsAtUtc < now)에 예약이 있으면 재예약 불가. 하지만 동시 예약 생성 중 상태 변경 가능.

5. **강사 프로필 검증 미완료** — requireInstructorProfileId()가 ACTIVE 상태만 검증. 강사 프로필 탈퇴/휴직 상태는 미처리.

6. **공개 카탈로그 필터 권한 부재** — CatalogQueryService.listPublished()가 모든 PUBLISHED 회차 반환. 조직/투어 private 플래그 미지원.

7. **회차 수정 후 예약 알림 부재** — update(), reschedule()은 기존 예약자에게 변경 사항 통지 로직 없음. NotificationService와 통합 필요.
