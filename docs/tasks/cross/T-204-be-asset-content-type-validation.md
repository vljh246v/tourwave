---
id: T-204
title: "T-204 — [BE] asset — Content-Type 화이트리스트 검증 (악성 파일 업로드 차단)"
aliases: [T-204]

repo: tourwave
area: be
milestone: M3
domain: asset
layer: domain
size: M
status: in-progress

depends_on: []
blocks: []
sub_tasks: []

github_issue: null
exec_plan: ""

created: 2026-04-28
updated: 2026-04-28
---

#status/in-progress #area/be #risk/high

# T-204 — [BE] asset — Content-Type 화이트리스트 검증

## 파일 소유권
WRITE:
  - `src/main/kotlin/com/demo/tourwave/domain/asset/AssetContentType.kt` (신규 — 화이트리스트 정의)
  - `src/main/kotlin/com/demo/tourwave/domain/asset/Asset.kt` (검증 호출 추가)
  - `src/test/kotlin/com/demo/tourwave/domain/asset/AssetContentTypeTest.kt` (신규)
  - `src/main/kotlin/com/demo/tourwave/application/asset/AssetCommandService.kt` (검증 호출)

READ:
  - `src/main/kotlin/com/demo/tourwave/domain/asset/` 전체
  - `docs/policies.md` §자산 업로드 정책
  - `docs/audit/BE-asset.md`

DO NOT TOUCH:
  - `src/main/kotlin/com/demo/tourwave/adapter/in/web/` (컨트롤러 변경 없음)

## SSOT 근거
- `docs/gap-matrix.md` 표 1, asset 도메인: 🟡 [T-204] Content-Type
- `docs/audit/BE-asset.md` §고위험 갭: Content-Type 검증 부재 → 악성 파일(예: .exe 위장 png) 업로드 가능
- `docs/policies.md` §자산 정책: 허용 MIME 타입 목록 명시 필요

## 현재 상태 (갭)
- [ ] Asset 도메인에 Content-Type 검증 로직 부재 (`grep -r "ALLOWED_CONTENT_TYPES" src/main/kotlin` → 0 hits)
- [ ] 클라이언트가 임의 MIME 타입으로 presigned URL 발급 가능
- [ ] 화이트리스트 정의 부재 (image/jpeg, image/png, image/webp, application/pdf 등)
- [ ] 거부 시 에러코드 매핑 부재

## 구현 지침
1. `AssetContentType` 값 객체: 허용 MIME 타입 enum/sealed class (image/jpeg, image/png, image/webp, image/gif, application/pdf)
2. `Asset.create(...)` 또는 `AssetCommandService.requestUpload(...)`에서 `AssetContentType.fromString(raw)` 호출 → 미허용 시 `DomainException(AssetErrorCode.UNSUPPORTED_CONTENT_TYPE)` 발생
3. 에러코드 `ASSET_UNSUPPORTED_CONTENT_TYPE` → HTTP 422 매핑 (controller advice)
4. 단위 테스트: 허용 5개 + 거부 케이스(text/html, application/x-msdownload, 빈 문자열, null) 검증
5. `docs/policies.md` 자산 정책 섹션에 화이트리스트 명시

## Acceptance Criteria
- [ ] `AssetContentType` 값 객체 정의 (sealed class 또는 enum)
- [ ] 허용 MIME 5개 이상 (image/jpeg, image/png, image/webp, image/gif, application/pdf)
- [ ] `Asset.create()` 또는 도메인 진입점에서 검증 호출
- [ ] 거부 시 422 + `ASSET_UNSUPPORTED_CONTENT_TYPE` 응답
- [ ] 단위 테스트 8개 이상 PASS
- [ ] `./gradlew test --tests "*AssetContentTypeTest"` 통과
- [ ] `./gradlew test --tests "*AssetCommandServiceTest*"` 회귀 PASS

## Verification
```bash
./scripts/verify-task.sh T-204
```

## Rollback
```bash
git checkout -- src/main/kotlin/com/demo/tourwave/domain/asset/
git checkout -- src/main/kotlin/com/demo/tourwave/application/asset/AssetCommandService.kt
git clean -fd src/test/kotlin/com/demo/tourwave/domain/asset/AssetContentTypeTest.kt
./gradlew clean test
```

## Notes
- 도메인 레이어 검증 — Spring/JPA import 금지
- 향후 확장: magic byte 검증(파일 시그니처)은 별도 카드 (adapter.out 레이어, 업로드 후 비동기)
- 현재 카드는 MIME 헤더 화이트리스트만 다룸
