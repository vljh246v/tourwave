# BE 감사: announcement

마지막 감사: 2026-04-18

## 요약
- 구현 완성도: ✅ 완성
- 테스트 완성도: ✅ 완성 (단위 + 통합)
- OpenAPI path 수: 5개 (태그: Announcements)
- SSOT 참조: `docs/openapi.yaml` (Announcements 태그)

## Domain 레이어

### 엔티티
- **Announcement**: 공지사항 메인 엔티티
  - `organizationId`, `title`, `body` (1-200자, 1-5000자)
  - `visibility` enum: DRAFT / PUBLIC / INTERNAL
  - `publishStartsAtUtc`, `publishEndsAtUtc` (게시 윈도우)
  - 불변식: `publishEndsAtUtc >= publishStartsAtUtc`

### 상태 머신
- **AnnouncementVisibility**: 3개 상태 (DRAFT → PUBLIC/INTERNAL, 상태 전이 제약 없음)
- 터미널 상태: 없음 (모든 상태에서 update/delete 가능)

### 값 객체
- 없음

### 도메인 서비스
- **Announcement.create()**: 새 공지사항 생성
- **Announcement.update()**: 기존 공지사항 수정 (partial update 지원)
- **Announcement.isVisibleToPublic(now)**: 시각 기준 게시 윈도우 판정

### 도메인 이벤트
- 없음

## Application 레이어

### 서비스
- **AnnouncementService**
  - `create(CreateAnnouncementCommand)` → 새 공지사항 저장
  - `listOperatorAnnouncements(...)` → 조직별 전체 목록 (페이지네이션)
  - `listPublicAnnouncements(...)` → 공개 + 윈도우 내 공지사항 필터링
  - `update(UpdateAnnouncementCommand)` → 기존 공지사항 수정
  - `delete(actorUserId, announcementId)` → 공지사항 삭제

### Port 인터페이스
- **AnnouncementRepository**: CRUD + findByOrganizationId, findAll, clear

## Adapter.in.web

### 컨트롤러
- **AnnouncementController** (5 엔드포인트)
  - `GET /public/announcements` (공개 목록, organizationId optional)
  - `GET /operator/organizations/{organizationId}/announcements` (운영자 목록)
  - `POST /organizations/{organizationId}/announcements` (생성, 201)
  - `PATCH /announcements/{announcementId}` (수정)
  - `DELETE /announcements/{announcementId}` (삭제)

### 인증/권한
- `X-Actor-User-Id` 헤더 필수 (operator 엔드포인트)
- `OrganizationAccessGuard.requireOperator()` 호출

### Idempotency
- **⚠ 미구현**: POST/PATCH/DELETE에 Idempotency-Key 없음 (도메인 규칙 위반 가능성)

## Adapter.in.job

해당 없음

## Adapter.out.persistence

### JPA 엔티티
- **AnnouncementJpaEntity**
  - 테이블: `announcements`
  - 인덱스: `(organization_id, created_at)`, `(visibility, publish_starts_at_utc, publish_ends_at_utc)`

### 어댑터 구현
- **JpaAnnouncementRepositoryAdapter**: 표준 CRUD 구현
- **InMemoryAnnouncementRepositoryAdapter**: 테스트용 in-memory 구현

## Tests

### 단위
- **AnnouncementServiceTest** (6개 테스트)
  - 공개 목록 노출 필터링
  - 운영자 목록 권한 검증
  - 생성/수정/삭제 happy path
  - 시간 기반 가시성 판정

### 통합
- **CommunicationReportingIntegrationTest** (announcement 포함)
  - 실제 MySQL 컨테이너 기반
  - 공지사항 + 보고서 엔드포인트 권한 검증
  - ✅ 현재 develop에서 성공 (main에서 기존 실패 보고됨)

### 실패 중인 테스트
- **CommunicationReportingIntegrationTest** (main 브랜치에서만 실패)
  - 원인 스케치:
    - 테스트는 announcement 관련 엔드포인트 접근 제어 검증
    - develop에서는 성공하므로 main과의 merged state 차이 추정
    - 공지사항 리포지토리 초기화 또는 권한 가드 로직 회귀 가능성
    - 현재 develop에서는 setUp() 후 clear() + save()가 정상 작동

## 관찰된 문제
1. **Idempotency-Key 미구현**: POST/PATCH/DELETE 엔드포인트가 멱등성 키를 사용하지 않음 (도메인 규칙 위반)
2. **상태 머신 부재**: 공지사항에 명시적 상태 머신이 없어 DRAFT → PUBLIC 정규 전이 검증 불가
3. **감사 이벤트 미기록**: create/update/delete 시 감사 로그 기록 안 됨
