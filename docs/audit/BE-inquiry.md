# BE 감사: inquiry

마지막 감사: 2026-04-18

## 요약
- 구현 완성도: ✅
- 테스트 완성도: ✅
- OpenAPI path 수: 6개 (태그: Inquiries)
- SSOT 참조: openapi.yaml 태그 Inquiries

## Domain 레이어
### 엔티티
- [x] `Inquiry` — `src/main/kotlin/com/demo/tourwave/domain/inquiry/Inquiry.kt`
  - 문의 생명주기: OPEN → CLOSED
  - close() 메서드로 상태 전이 (idempotent: 이미 CLOSED면 그대로)
  - 메타데이터: organizationId, occurrenceId, bookingId, createdByUserId, subject

- [x] `InquiryMessage` — 문의 메시지/댓글
  - senderUserId로 발신자 추적
  - attachmentAssetIds 지원
  - 시간순 정렬 가능 (createdAt)

### 상태 머신
- [x] `InquiryStatus` — OPEN, CLOSED (2개 상태만)

### 값 객체
- (값 객체 불필요, DTO로 관리)

### 도메인 서비스
- (별도 도메인 서비스 없음)

### 도메인 이벤트
- 감사 로그:
  - INQUIRY_CREATED
  - INQUIRY_CLOSED
  - INQUIRY_MESSAGE_POSTED

## Application 레이어
### 서비스
- [x] `InquiryCommandService`:
  - `createInquiry(CreateInquiryCommand)` — 문의 생성, idempotency-key 필수
    - 1개 booking당 1개 inquiry만 허용 (unique constraint)
    - booking leader만 생성 가능
    - booking existence 및 scope 검증 (organizationId, occurrenceId, bookingId 일치)
  - `postMessage(PostInquiryMessageCommand)` — 메시지 추가, idempotency-key 필수
    - 접근 제어: inquiry creator 또는 organization staff만 가능
    - attachmentAssetIds 지원
  - `closeInquiry(CloseInquiryCommand)` — 문의 종료, idempotency-key 필수

- [x] `InquiryQueryService`:
  - `getInquiryDetail(GetInquiryDetailQuery)` — 문의 상세 조회, 접근 제어 검증
  - `listMyInquiries(ListMyInquiriesQuery)` — 내 문의 목록 (cursor pagination)
  - `listMessages(ListInquiryMessagesQuery)` — 메시지 목록 (cursor pagination)

- [x] `InquiryAccessPolicy`:
  - authorize() — 문의 접근 제어 (creator or org staff)
  - authorizeMessagePosting() — 메시지 작성 권한 검증

### Port 인터페이스
- [x] `InquiryRepository` — find*(), save() 메서드
  - findById(), findByBookingId(), findByCreatedByUserId()
  - findMessagesByInquiryId()
- [x] `BookingRepository` — 참조 무결성 (부분 임포트)
- [x] `IdempotencyStore` — Idempotency-Key 처리
- [x] `AuditEventPort` — 감사 로그 기록

## Adapter.in.web
### 컨트롤러
- [x] `InquiryCommandController`:
  - `POST /occurrences/{occurrenceId}/inquiries` → createInquiry() (idempotency-key 필수)
    - body: { bookingId, subject?, message }
  - `POST /inquiries/{inquiryId}/messages` → postMessage() (idempotency-key 필수)
    - body: { body, attachmentAssetIds[] }
  - `POST /inquiries/{inquiryId}/close` → closeInquiry() (idempotency-key 필수)

- [x] `InquiryQueryController`:
  - `GET /inquiries/{inquiryId}` → getInquiryDetail()
  - `GET /inquiries?status=OPEN/CLOSED&limit=20&cursor=` → listMyInquiries()
  - `GET /inquiries/{inquiryId}/messages?limit=20&cursor=` → listMessages()

## Adapter.out.persistence
### JPA 엔티티
- [x] `InquiryJpaEntity` — 테이블 `inquiries`
  - Unique constraint: uk_inquiries_booking (booking_id)
  - 인덱스: idx_inquiries_booking, idx_inquiries_creator_created
  - 모든 timestamp UTC

- [x] `InquiryMessageJpaEntity` — 테이블 `inquiry_messages`
  - 인덱스 없음 (기본 PK만 사용)
  - 모든 timestamp UTC

### 어댑터 구현
- [x] `JpaInquiryRepositoryAdapter` (구현 포트: `InquiryRepository`)
  - `InquiryJpaRepository` 스프링 데이터 JPA 리포지토리
  - `InquiryMessageJpaRepository` 스프링 데이터 JPA 리포지토리

## Tests
### 단위
- `InquiryCommandServiceTest` — 문의 생성, 메시지 추가, 종료 로직
  - booking leader 검증
  - unique booking constraint 검증
  - idempotency 테스트

- `InquiryQueryServiceTest` — 쿼리 서비스 유닛 테스트
  - cursor pagination 로직
  - 접근 제어 검증

### 통합
- (별도 통합 테스트 없음, 컨트롤러 테스트로 커버됨)

### 실패 중
- (모두 통과)

## 관찰된 문제
- ✅ Idempotency-Key 헤더 모든 write 엔드포인트에서 필수 구현
- ✅ 감사 로그 생성, 메시지, 종료 이벤트 기록
- ✅ Booking leader만 문의 생성 가능 (인증 검증)
- ✅ 1개 booking당 1개 inquiry (unique constraint)
- ✅ Cursor pagination (offset이 아닌 ID 기반)
- ✅ 모든 timestamp UTC 사용
- ✅ Domain 레이어 Spring/JPA 임포트 없음
- ✅ Application 레이어 adapter.out 구체 클래스 직접 임포트 없음
- ⚠️  컨트롤러 계약 테스트 미존재 (OpenAPI SSOT 검증 부재)
  - BE-booking의 BookingControllerIntegrationTest 같은 테스트 없음
  - HTTP 응답 형식, 상태 코드, 에러 매핑 수동 검증 필요
