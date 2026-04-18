# BE 감사: tour

마지막 감사: 2026-04-18

## 요약
- 구현 완성도: ✅
- 테스트 완성도: 🟡
- OpenAPI path 수: 5개 (태그: Tours)
- SSOT 참조: openapi.yaml 태그 Tours

## Domain 레이어
### 엔티티
- `Tour` — organizationId, title, summary, status(DRAFT/PUBLISHED), content(TourContent 값 객체), attachmentAssetIds(List), publishedAt(UTC), createdAt/updatedAt(UTC)
- `TourContent` — description, highlights(List), inclusions(List), exclusions(List), preparations(List), policies(List)

### 상태 머신
- **Tour.status**: DRAFT → PUBLISHED (단방향, 역전이 불가)

### 값 객체
- `TourStatus` enum: DRAFT, PUBLISHED
- `TourContent` — 6개 필드 (description, highlights, inclusions, exclusions, preparations, policies)

### 도메인 서비스
- Tour: `updateMetadata()`, `updateContent()`, `updateAttachments()`, `publish()`, Factory 메서드 `create()`

### 도메인 이벤트
- 명시적 도메인 이벤트 없음 (감사는 application 레이어에서 기록)

## Application 레이어
### 서비스
- `TourCommandService` — create(), update(), updateContent(), publish()
- `TourQueryService` — getById(), listByOrganization(), getPublicTour(), listPublicTours()

### Port 인터페이스
- `TourRepository` — save(), findById(), findByIdAndOrganizationId(), findByOrganizationId(), findPublishedById()

## Adapter.in.web
### 컨트롤러
- `TourOperatorController` (5개 엔드포인트)
  - POST /organizations/{organizationId}/tours (201) — create
  - GET /organizations/{organizationId}/tours (200) — listByOrganization
  - PATCH /tours/{tourId} (200) — updateMetadata (title, summary)
  - PUT /tours/{tourId}/content (200) — updateContent
  - POST /tours/{tourId}/publish (200) — publish

- `TourPublicController` (4개 엔드포인트)
  - GET /public/tours (200) — listPublicTours (paginated)
  - GET /public/tours/{tourId} (200) — getPublicTour (PUBLISHED만)

## Adapter.out.persistence
### JPA 엔티티
- `TourJpaEntity` — id, organizationId, title, summary, status, content(JSON with all 6 TourContent fields), attachmentAssetIds(JSON), publishedAtUtc, createdAtUtc, updatedAtUtc

### 어댑터 구현
- `JpaTourRepositoryAdapter`

## Tests
### 단위
- `TourCommandServiceTest` ✅ — create(), update(), publish()

### 통합
- `InstructorAndTourControllerIntegrationTest` 🟡 — instructor + tour (결합 테스트, 분리 필요)
- `CatalogQueryService` 엔드포인트 정보 (별도 `CatalogControllerIntegrationTest` 실패 중인지 확인 필요)

### 실패 중
- `CatalogQueryService` 관련 통합 테스트는 CLAUDE.md에 기존부터 실패 중 명시

## 관찰된 문제
1. **감사 이벤트 미기록**: 투어 생성/수정/발행 시 AuditEvent 기록 없음
2. **Idempotency-Key 미사용**: 중복 생성 요청 미보장
3. **발행 이후 수정 정책 미정의**: publishedAt 이후 updateContent() 허용 여부 비-명시
4. **접근 제어 검증 부족**: updateContent() 호출 시 organizationId 소유권 검증 필요 (현재 tourId만 사용)
5. **통합 테스트 분리 필요**: InstructorAndTourControllerIntegrationTest 분리

## 스키마 검증
✅ 모든 timestamp UTC
✅ organizationId 외래키 제약
⚠ content 필드 (JSON) — 6개 필드 스키마 검증 (validation 부재 시 불완전한 데이터 가능)
⚠ attachmentAssetIds (JSON array) — 유효성 검증 미보장

## 아키텍처 체크
✅ adapter.in → application → domain 단방향 의존성
✅ Service 레이어에서 트랜잭션 경계 관리
⚠ TourQueryService.listPublicTours() pagination 전략 확인 필요 (성능)
