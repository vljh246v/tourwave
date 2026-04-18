# BE 감사: participant

마지막 감사: 2026-04-18

## 요약
- 구현 완성도: ✅ 완성
- 테스트 완성도: ✅ 완성 (단위 + 통합)
- OpenAPI path 수: 5개 (태그: Participants)
- SSOT 참조: `docs/openapi.yaml` (Participants 태그), `docs/domain-rules.md`

## Domain 레이어

### 엔티티
- **BookingParticipant**: 예약 참여자
  - `bookingId`, `userId`, `status` (BookingParticipantStatus)
  - `attendanceStatus` (AttendanceStatus: UNKNOWN / PRESENT / ABSENT)
  - `invitedAt`, `respondedAt` 타임스탐프

### 상태 머신
- **BookingParticipantStatus**: 6개 상태
  - LEADER (리더, 활성)
  - INVITED (초대됨, 활성)
  - ACCEPTED (수락, 활성)
  - DECLINED (거절, 터미널)
  - EXPIRED (만료, 터미널)
  - CANCELED (취소, 터미널)
  - 전이: LEADER/INVITED → ACCEPTED/DECLINED/EXPIRED/CANCELED
  - 불변식: 터미널 상태 → 추가 전이 불가 (cancel() no-op)

### 값 객체
- 없음

### 도메인 서비스
- **BookingParticipant.leader()**: 리더 생성 팩토리
- **BookingParticipant.isActive()**: 활성 상태 판정 (LEADER/INVITED/ACCEPTED)
- **BookingParticipant.accept/decline/expire/cancel()**: 상태 전이
- **BookingParticipant.recordAttendance()**: 출석 기록 (LEADER/ACCEPTED만 가능)

### 도메인 이벤트
- 없음

## Application 레이어

### 서비스
- **ParticipantCommandService** (멱등성 키 + 감사 이벤트 구현)
  - `createInvitation(CreateParticipantInvitationCommand)`: 초대 생성
    - 아이덤포턴시: YES (idempotencyStore 사용, requestHash="bookingId|userId")
    - 감사: YES (AuditEventPort.append() 호출)
  - `respondInvitation(RespondParticipantInvitationCommand)`: accept/decline
    - 아이덤포턴시: YES
    - 감사: YES
  - `recordAttendance(RecordParticipantAttendanceCommand)`: 출석 기록
    - 아이덤포턴시: YES
    - 감사: YES
- **ParticipantQueryService**: 참여자 조회 (읽기 전용)
- **ParticipantInvitationLifecycleService**: 초대 만료 자동 갱신
- **InvitedParticipantExpirationService**: 배경 잡 (초대 48h 만료)

### Port 인터페이스
- **BookingParticipantRepository**: CRUD + findByBookingId, findByUserId, findByStatus

## Adapter.in.web

### 컨트롤러
- **ParticipantCommandController** (4 엔드포인트, 모두 Idempotency-Key 필수)
  - `POST /bookings/{bookingId}/participants/invitations` (201)
  - `POST /bookings/{bookingId}/participants/invitations/{participantId}/accept`
  - `POST /bookings/{bookingId}/participants/invitations/{participantId}/decline`
  - `POST /bookings/{bookingId}/participants/{participantId}/attendance`
- **ParticipantQueryController**: 조회 엔드포인트
- **ParticipantRosterController**: 명단 조회

### 인증/권한
- `X-Actor-User-Id` 헤더 필수
- `ParticipantAccessPolicy`: 사용자 본인 또는 리더만 접근 가능

### Idempotency
- ✅ **모든 변경 엔드포인트에 Idempotency-Key 필수** (도메인 규칙 준수)
- 동일 키 + 다른 payload → 422 IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD

## Adapter.in.job

- **InvitedParticipantExpirationJob**: 48시간 이상 응답 없는 초대를 EXPIRED로 전환

## Adapter.out.persistence

### JPA 엔티티
- **BookingParticipantJpaEntity**
  - 테이블: `booking_participants`
  - 유니크 제약: `(booking_id, user_id)`
  - 인덱스: `(booking_id)`, `(status)`

### 어댑터 구현
- **JpaBookingParticipantRepositoryAdapter**: 표준 CRUD
- **InMemoryBookingParticipantRepositoryAdapter**: 테스트용

## Tests

### 단위
- **BookingParticipantTest** (7개 테스트)
  - 상태 전이: leader → accept/decline/expire/cancel
  - 활성 상태 판정
  - 출석 기록 (LEADER/ACCEPTED만 가능)
- **ParticipantCommandServiceTest**: idempotency 재생 검증
- **ParticipantQueryServiceTest**: 조회 권한 검증
- **InvitedParticipantExpirationServiceTest**: 48h 만료 로직
- **ParticipantRosterQueryServiceTest**: 명단 출력 형식

### 통합
- 예약 통합 테스트 (BookingControllerIntegrationTest 등)
  - 참여자 초대 → 수락 → 출석 기록 flow 검증

### 실패 중인 테스트
- 없음 (현재 develop에서 모두 성공)

## 관찰된 문제
1. **정원 불변식 검증**: 파티 크기 초과 초대 시 에러 코드 422이지만, 정원(capacity) 불변식과의 관계 문서화 필요
2. **초대 윈도우**: `TimeWindowPolicyService.isInvitationWindowClosed()` 호출하지만 시간 상수 (6시간) 는 CLAUDE.md에만 언급 (코드에 매직값 확인 필요)
