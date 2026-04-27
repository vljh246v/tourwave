# BE 감사: asset

마지막 감사: 2026-04-28

## 요약
- 구현 완성도: 🟡 (Content-Type 검증 완료 — T-204)
- 테스트 완성도: 🟡
- OpenAPI path 수: 4개 (태그: Assets)
- SSOT 참조: openapi.yaml 태그 Assets

## Domain 레이어
### 엔티티
- `Asset` — ownerUserId, organizationId(nullable), status(UPLOADING/READY), fileName, contentType, storageKey, uploadUrl, publicUrl(nullable), sizeBytes(nullable), checksumSha256(nullable), createdAt, completedAt(nullable)

### 상태 머신
- **Asset.status**: UPLOADING → READY (단방향, 역전이 불가)

### 값 객체
- `AssetStatus` enum: UPLOADING, READY
- `AssetContentType` enum: IMAGE_JPEG, IMAGE_PNG, IMAGE_WEBP, IMAGE_GIF, APPLICATION_PDF (T-204, 2026-04-28)

### 도메인 서비스
- Asset: `complete()`, Factory 메서드 `create()`
- 도메인 검증: publicUrl 필수, sizeBytes >= 0

### 도메인 이벤트
- 명시적 도메인 이벤트 없음

## Application 레이어
### 서비스
- `AssetCommandService` — issueUpload(), completeUpload(), attachOrganizationAssets(), attachTourAssets()

### Port 인터페이스
- `AssetRepository` — save(), findById(), findByIdAndOwnerUserId()
- `AssetUploadPort` (외부 스토리지) — getUploadUrl(), markAsReady()

## Adapter.in.web
### 컨트롤러
- `AssetController` (4개 엔드포인트)
  - POST /assets/uploads (201) — issueUpload (presigned upload URL 발급)
  - POST /assets/{assetId}/complete (200) — completeUpload (uploadUrl → publicUrl 전환)
  - PUT /operator/organizations/{organizationId}/assets (200) — attachOrganizationAssets (조직 프로필 이미지 바인딩)
  - PUT /tours/{tourId}/assets (200) — attachTourAssets (투어 이미지 바인딩)

## Adapter.out.persistence
### JPA 엔티티
- `AssetJpaEntity` — id, ownerUserId, organizationId, status, fileName, contentType, storageKey, uploadUrl, publicUrl, sizeBytes, checksumSha256, createdAtUtc, completedAtUtc

### 어댑터 구현
- `JpaAssetRepositoryAdapter`

## Tests
### 단위
- `AssetCommandServiceTest` ✅ — issueUpload(), completeUpload(), attachment flows
- `AssetContentTypeTest` ✅ — 13개: 허용 5종, 거부 4종, 대소문자 정규화, mimeType 속성 (T-204, 2026-04-28)

### 통합
- Asset 전용 컨트롤러 통합 테스트 없음 (관련 테스트 미존재)

### 실패 중
- 없음 (단위 테스트만 존재)

## 관찰된 문제
1. **Upload 완료 콜백 미검증**: completeUpload() 호출 시 실제 파일이 S3/storage에 업로드되었는지 확인 불가
2. **checksumSha256 검증 없음**: 클라이언트 제공 checksum을 신뢰만 함 (무결성 검증 부재)
3. **organizationId nullable 처리**: 개인 자산 vs 조직 자산 구분 기준이 모호 (권한 검증 필요)
4. ~~**Content-Type 검증 부재**: 위험한 파일 타입(executable, script) 필터링 없음~~ ✅ **T-204에서 해결** (2026-04-28) — AssetContentType 화이트리스트(5종) 검증 추가
5. **파일 크기 제한 없음**: Asset.sizeBytes에 상한선(max file size) 검증 미포함
6. **통합 테스트 부족**: presigned URL 발급 + 업로드 + complete 전체 flow 테스트 없음
7. **organizationId 소유권 검증 미흡**: attachOrganizationAssets() 호출 시 접근 제어 권한 확인 필요

## 스키마 검증
✅ 모든 timestamp UTC
⚠ uploadUrl, publicUrl 둘 다 저장 — 상태 전이 일관성 필요 (UPLOADING 상태에서 publicUrl은 null이어야 함)
⚠ storageKey 보안 노출 가능성 (외부 노출 금지 필드)

## 구현 리스크
- ~~🔴 **높음**: Content-Type 검증 없음 (보안)~~ ✅ T-204 완료
- 🟡 **중간**: 파일 크기 상한 미정의 (비용/리소스)
- 🟡 **중간**: checksum 무결성 검증 없음 (데이터 신뢰성)

## 권장 조치
1. ~~Content-Type 화이트리스트 검증 추가~~ ✅ T-204 완료 (2026-04-28)
2. 파일 크기 상한선(max 100MB) 정책 정의
3. 선택적 checksum 검증 (SHA-256 비교)
4. 조직 자산과 개인 자산 권한 분리 강화
5. AssetControllerIntegrationTest 생성 (end-to-end flow)
