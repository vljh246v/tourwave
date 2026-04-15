# Spec Docs Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tourwave 저장소의 `agent/` + `docs/` 분산 스펙 문서 23개(5,444줄)를 `docs/` 단일 폴더 아래 7개 파일(약 1,500~2,000줄)로 통합하고, 문서 drift 가드 테스트를 새 구조에 맞게 축소·경로 갱신한다.

**Architecture:** 매 커밋이 `./gradlew test`를 그린으로 유지하도록 작업 순서를 설계한다. (1) 삭제 대상 문서를 참조하는 테스트 assertion을 먼저 제거, (2) 새 통합 문서를 **추가**만 수행 (기존 파일 건드리지 않음), (3) `git mv`와 테스트 경로 갱신을 하나의 커밋으로 묶어 유지되는 파일 이동, (4) 흡수 원본 삭제. 모든 파일 이동은 `git mv`로 해서 히스토리 보존.

**Tech Stack:** Markdown, YAML (OpenAPI 3.1), Kotlin/JUnit5 (테스트만 수정), Gradle 테스트 러너.

**Reference Spec:** `docs/superpowers/specs/2026-04-15-agent-folder-slimming-design.md`

---

## File Structure

**Created (new):**
- `README.md` — 루트 신규, 제품 한 줄 요약 + 디렉토리 구조 + 필독 문서 링크 + 빌드 커맨드
- `docs/architecture.md` — 헥사고날 규칙 + 구현 노트 (기존 `agent/10` + `agent/06` 흡수)
- `docs/policies.md` — 권한·운영 정책·trust surface (기존 `agent/05` + `08` + `18` 흡수, 슬림화)
- `docs/testing.md` — 테스트 규약 + 인덱스 (기존 `agent/07` + `14` 흡수)
- `docs/operations.md` — 런타임/운영/환경/출시 체크리스트 (기존 `agent/12` + `19` + `docs/launch-readiness-checklist.md` + `docs/profile-env-matrix.md` 흡수)

**Moved (git mv, 내용 동일):**
- `agent/01_domain_rules.md` → `docs/domain-rules.md`
- `agent/02_schema_mysql.md` → `docs/schema.md`
- `agent/04_openapi.yaml` → `docs/openapi.yaml`

**Modified (test code):**
- `src/test/kotlin/com/demo/tourwave/agent/DocumentationBaselineTest.kt` — 삭제 대상 문서를 참조하는 assert 블록 2개 제거, 컨트롤러 mapping assert 1개만 유지
- `src/test/kotlin/com/demo/tourwave/agent/OpenApiContractVerificationTest.kt` — `Path.of("agent/04_openapi.yaml")` 4곳 → `Path.of("docs/openapi.yaml")`

**Deleted (git rm):**
- `agent/00_overview.md`, `agent/03_api_catalog.md`, `agent/05_authz_model.md`, `agent/06_implementation_notes.md`, `agent/07_test_scenarios.md`, `agent/08_operational_policy_tables.md`, `agent/09_spec_index.md`, `agent/10_architecture_hexagonal.md`, `agent/11_current_implementation_status.md`, `agent/12_runtime_topology_and_operations.md`, `agent/13_api_status_matrix.md`, `agent/14_test_traceability_matrix.md`, `agent/15_next_development_backlog.md`, `agent/16_product_delivery_roadmap.md`, `agent/17_release_gap_execution_plan.md`, `agent/18_trust_surface_policy.md`, `agent/19_launch_ops_baseline.md`
- `docs/launch-readiness-checklist.md`, `docs/openapi-gap-report.md`, `docs/profile-env-matrix.md`

**Preserved:**
- `docs/superpowers/specs/2026-04-15-agent-folder-slimming-design.md`
- `docs/superpowers/plans/2026-04-15-spec-docs-consolidation.md` (이 문서)
- `src/` 코드 런타임 전체, `build.gradle.kts`, `.github/workflows/ci.yml`(테스트 클래스명 유지 전제)

---

## Task 1: Shrink DocumentationBaselineTest (문서 assert 제거, 컨트롤러 mapping assert만 유지)

**Files:**
- Modify: `src/test/kotlin/com/demo/tourwave/agent/DocumentationBaselineTest.kt`

**왜 먼저**: 이 테스트는 삭제 대상 문서(`agent/13_api_status_matrix.md`, `agent/09_spec_index.md`, `agent/03_api_catalog.md`, `agent/18_trust_surface_policy.md`, `agent/19_launch_ops_baseline.md`, `agent/14_test_traceability_matrix.md`, `docs/openapi-gap-report.md`, `docs/launch-readiness-checklist.md`)를 `readProjectFile`로 읽는다. 이 문서들을 삭제하기 전에 테스트에서 참조를 제거해야 그린 유지 가능. 이 시점에는 원본 문서가 아직 있으므로 축소 후에도 그린.

- [ ] **Step 1: 현재 테스트 구조 확인**

Run: `head -40 src/test/kotlin/com/demo/tourwave/agent/DocumentationBaselineTest.kt`
Expected: `package com.demo.tourwave.agent` + 20여 개 컨트롤러 import + `DocumentationBaselineTest` class

- [ ] **Step 2: 파일 전체를 아래 내용으로 교체**

```kotlin
package com.demo.tourwave.agent

import com.demo.tourwave.adapter.`in`.web.asset.AssetController
import com.demo.tourwave.adapter.`in`.web.announcement.AnnouncementController
import com.demo.tourwave.adapter.`in`.web.booking.BookingRefundPreviewController
import com.demo.tourwave.adapter.`in`.web.booking.WaitlistOperatorController
import com.demo.tourwave.adapter.`in`.web.customer.CustomerController
import com.demo.tourwave.adapter.`in`.web.inquiry.InquiryQueryController
import com.demo.tourwave.adapter.`in`.web.instructor.InstructorProfileController
import com.demo.tourwave.adapter.`in`.web.instructor.InstructorRegistrationController
import com.demo.tourwave.adapter.`in`.web.organization.OrganizationOperatorController
import com.demo.tourwave.adapter.`in`.web.organization.OrganizationPublicController
import com.demo.tourwave.adapter.`in`.web.operations.OperatorRemediationQueueController
import com.demo.tourwave.adapter.`in`.web.occurrence.OccurrenceOperatorController
import com.demo.tourwave.adapter.`in`.web.occurrence.OccurrencePublicController
import com.demo.tourwave.adapter.`in`.web.participant.ParticipantRosterController
import com.demo.tourwave.adapter.`in`.web.payment.PaymentOperatorController
import com.demo.tourwave.adapter.`in`.web.payment.PaymentWebhookController
import com.demo.tourwave.adapter.`in`.web.reporting.OrganizationReportController
import com.demo.tourwave.adapter.`in`.web.review.ReviewController
import com.demo.tourwave.adapter.`in`.web.tour.TourOperatorController
import com.demo.tourwave.adapter.`in`.web.tour.TourPublicController
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import kotlin.test.assertEquals

class DocumentationBaselineTest {
    @Test
    fun `drift-prone controller mappings stay aligned with current truth docs`() {
        assertGetMapping(BookingRefundPreviewController::class.java, "getRefundPreview", "/bookings/{bookingId}/refund-preview")
        assertPostMapping(WaitlistOperatorController::class.java, "promote", "/bookings/{bookingId}/waitlist/promote")
        assertPostMapping(WaitlistOperatorController::class.java, "skip", "/bookings/{bookingId}/waitlist/skip")
        assertGetMapping(InquiryQueryController::class.java, "listInquiryMessages", "/inquiries/{inquiryId}/messages")
        assertGetMapping(ParticipantRosterController::class.java, "exportRoster", "/occurrences/{occurrenceId}/participants/roster/export")
        assertPostMapping(OrganizationOperatorController::class.java, "createOrganization", "/operator/organizations")
        assertGetMapping(AnnouncementController::class.java, "listPublicAnnouncements", "/public/announcements")
        assertPostMapping(AnnouncementController::class.java, "createAnnouncement", "/organizations/{organizationId}/announcements")
        assertPatchMapping(AnnouncementController::class.java, "updateAnnouncement", "/announcements/{announcementId}")
        assertDeleteMapping(AnnouncementController::class.java, "deleteAnnouncement", "/announcements/{announcementId}")
        assertGetMapping(OrganizationReportController::class.java, "getBookingReport", "/organizations/{organizationId}/reports/bookings")
        assertGetMapping(OrganizationReportController::class.java, "getOccurrenceOpsReport", "/organizations/{organizationId}/reports/occurrences")
        assertGetMapping(OperatorRemediationQueueController::class.java, "listQueue", "/operator/operations/remediation-queue")
        assertPostMapping(OperatorRemediationQueueController::class.java, "remediate", "/operator/operations/remediation-queue/{sourceType}/{sourceKey}")
        assertGetMapping(ReviewController::class.java, "getTourSummary", "/tours/{tourId}/reviews/summary")
        assertGetMapping(ReviewController::class.java, "getInstructorSummary", "/instructors/{instructorProfileId}/reviews/summary")
        assertGetMapping(ReviewController::class.java, "getPublicOrganizationSummary", "/organizations/{organizationId}/reviews/summary")
        assertGetMapping(ReviewController::class.java, "getOperatorOrganizationSummary", "/operator/organizations/{organizationId}/reviews/summary")
        assertGetMapping(OrganizationPublicController::class.java, "getPublicOrganization", "/organizations/{organizationId}")
        assertPostMapping(OrganizationPublicController::class.java, "acceptInvitation", "/organizations/{organizationId}/memberships/accept")
        assertPostMapping(InstructorRegistrationController::class.java, "apply", "/instructor-registrations")
        assertGetMapping(InstructorRegistrationController::class.java, "listByOrganization", "/organizations/{organizationId}/instructor-registrations")
        assertPostMapping(InstructorRegistrationController::class.java, "approve", "/instructor-registrations/{registrationId}/approve")
        assertGetMapping(InstructorProfileController::class.java, "getMyProfile", "/me/instructor-profile")
        assertPatchMapping(InstructorProfileController::class.java, "updateMyProfile", "/me/instructor-profile")
        assertGetMapping(InstructorProfileController::class.java, "getPublicProfile", "/instructors/{instructorProfileId}")
        assertPostMapping(TourOperatorController::class.java, "createTour", "/organizations/{organizationId}/tours")
        assertPostMapping(OccurrenceOperatorController::class.java, "createOccurrence", "/tours/{tourId}/occurrences")
        assertPatchMapping(OccurrenceOperatorController::class.java, "updateOccurrence", "/occurrences/{occurrenceId}")
        assertPostMapping(OccurrenceOperatorController::class.java, "rescheduleOccurrence", "/occurrences/{occurrenceId}/reschedule")
        assertGetMapping(TourPublicController::class.java, "listTours", "/tours")
        assertGetMapping(TourPublicController::class.java, "getTour", "/tours/{tourId}")
        assertGetMapping(TourPublicController::class.java, "listOccurrences", "/tours/{tourId}/occurrences")
        assertPutMapping(TourOperatorController::class.java, "updateTourContent", "/tours/{tourId}/content")
        assertGetMapping(TourPublicController::class.java, "getPublicContent", "/tours/{tourId}/content")
        assertGetMapping(OccurrencePublicController::class.java, "getAvailability", "/occurrences/{occurrenceId}/availability")
        assertGetMapping(OccurrencePublicController::class.java, "getQuote", "/occurrences/{occurrenceId}/quote")
        assertGetMapping(OccurrencePublicController::class.java, "search", "/search/occurrences")
        assertPostMapping(AssetController::class.java, "issueUpload", "/assets/uploads")
        assertPostMapping(AssetController::class.java, "completeUpload", "/assets/{assetId}/complete")
        assertPutMapping(AssetController::class.java, "attachOrganizationAssets", "/operator/organizations/{organizationId}/assets")
        assertPutMapping(AssetController::class.java, "attachTourAssets", "/tours/{tourId}/assets")
        assertGetMapping(CustomerController::class.java, "listMyBookings", "/me/bookings")
        assertGetMapping(CustomerController::class.java, "bookingCalendar", "/bookings/{bookingId}/calendar.ics")
        assertGetMapping(CustomerController::class.java, "occurrenceCalendar", "/occurrences/{occurrenceId}/calendar.ics")
        assertPostMapping(CustomerController::class.java, "favoriteTour", "/tours/{tourId}/favorite")
        assertDeleteMapping(CustomerController::class.java, "unfavoriteTour", "/tours/{tourId}/favorite")
        assertGetMapping(CustomerController::class.java, "listFavorites", "/me/favorites")
        assertGetMapping(CustomerController::class.java, "listNotifications", "/me/notifications")
        assertPostMapping(CustomerController::class.java, "markNotificationRead", "/me/notifications/{notificationId}/read")
        assertPostMapping(CustomerController::class.java, "markAllNotificationsRead", "/me/notifications/read-all")
        assertPostMapping(PaymentWebhookController::class.java, "receiveProviderWebhook", "/payments/webhooks/provider")
        assertGetMapping(PaymentOperatorController::class.java, "listRefundOpsQueue", "/operator/payments/refunds/ops")
        assertPostMapping(PaymentOperatorController::class.java, "retryBookingRefund", "/operator/payments/bookings/{bookingId}/refund-retry")
        assertGetMapping(PaymentOperatorController::class.java, "listDailySummaries", "/operator/finance/reconciliation/daily")
        assertPostMapping(PaymentOperatorController::class.java, "refreshDailySummary", "/operator/finance/reconciliation/daily/{summaryDate}/refresh")
        assertGetMapping(PaymentOperatorController::class.java, "exportDailySummariesCsv", "/operator/finance/reconciliation/daily/export")
    }

    private fun assertGetMapping(controller: Class<*>, methodName: String, expectedPath: String) {
        val annotation = controller.declaredMethods.single { it.name == methodName }.getAnnotation(GetMapping::class.java)
        requireNotNull(annotation) { "Expected @GetMapping on ${controller.simpleName}.$methodName" }
        assertEquals(expectedPath, annotation.paths().singlePath())
    }

    private fun assertPostMapping(controller: Class<*>, methodName: String, expectedPath: String) {
        val annotation = controller.declaredMethods.single { it.name == methodName }.getAnnotation(PostMapping::class.java)
        requireNotNull(annotation) { "Expected @PostMapping on ${controller.simpleName}.$methodName" }
        assertEquals(expectedPath, annotation.paths().singlePath())
    }

    private fun assertPatchMapping(controller: Class<*>, methodName: String, expectedPath: String) {
        val annotation = controller.declaredMethods.single { it.name == methodName }.getAnnotation(PatchMapping::class.java)
        requireNotNull(annotation) { "Expected @PatchMapping on ${controller.simpleName}.$methodName" }
        assertEquals(expectedPath, annotation.paths().singlePath())
    }

    private fun assertPutMapping(controller: Class<*>, methodName: String, expectedPath: String) {
        val annotation = controller.declaredMethods.single { it.name == methodName }.getAnnotation(PutMapping::class.java)
        requireNotNull(annotation) { "Expected @PutMapping on ${controller.simpleName}.$methodName" }
        assertEquals(expectedPath, annotation.paths().singlePath())
    }

    private fun assertDeleteMapping(controller: Class<*>, methodName: String, expectedPath: String) {
        val annotation = controller.declaredMethods.single { it.name == methodName }.getAnnotation(DeleteMapping::class.java)
        requireNotNull(annotation) { "Expected @DeleteMapping on ${controller.simpleName}.$methodName" }
        val paths = if (annotation.path.isNotEmpty()) annotation.path else annotation.value
        assertEquals(expectedPath, paths.singlePath())
    }

    private fun GetMapping.paths(): Array<String> = if (path.isNotEmpty()) path else value
    private fun PostMapping.paths(): Array<String> = if (path.isNotEmpty()) path else value
    private fun PatchMapping.paths(): Array<String> = if (path.isNotEmpty()) path else value
    private fun PutMapping.paths(): Array<String> = if (path.isNotEmpty()) path else value

    private fun Array<String>.singlePath(): String {
        assertEquals(1, size, "Expected a single mapping path")
        return single()
    }
}
```

변경 요약:
- 제거: `api status matrix ...` 테스트 메서드 (line 36-95), `spec and catalog docs ...` 테스트 메서드 (line 97-153), `readProjectFile` / `assertContains` 헬퍼, `java.nio.file.Files` / `java.nio.file.Path` / `kotlin.test.assertTrue` import
- 유지: `drift-prone controller mappings ...` 테스트 메서드 전체 로직 (본 Step의 코드 블록), 모든 `assert*Mapping` 헬퍼, `paths()` / `singlePath()` 확장 함수

- [ ] **Step 3: 테스트 실행**

Run: `./gradlew test --tests 'com.demo.tourwave.agent.DocumentationBaselineTest'`
Expected: PASS (단일 테스트 메서드)

- [ ] **Step 4: Commit**

```bash
git add src/test/kotlin/com/demo/tourwave/agent/DocumentationBaselineTest.kt
git commit -m "test(docs): shrink DocumentationBaselineTest to controller mapping assertions only

향후 agent/ 폴더 정리 시 삭제·통합될 markdown 문서들에 대한
assert 블록을 제거하고, 실질적 drift 가드인 컨트롤러 mapping
assertion만 남긴다. 문서 변경 시에도 견고하게 그린 유지."
```

---

## Task 2: README.md 작성 (루트 신규)

**Files:**
- Create: `README.md` (프로젝트 루트)

**흡수 원본:** `agent/00_overview.md` (88줄). 긴 설명은 버리고 필수 포인터만.

- [ ] **Step 1: README.md 생성, 아래 섹션 순서대로 채움**

```markdown
# Tourwave

투어/액티비티 운영사를 위한 예약 플랫폼 백엔드. 고객 예약, 운영 실행, 사후 관리(출석/리뷰/환불)를 하나의 도메인으로 다룬다.

## Tech Stack

- Spring Boot 3.3.1, Kotlin 1.9.24 (JDK 17)
- Spring Data JPA, MySQL, Flyway
- Spring Security (JWT), Micrometer + Prometheus
- JUnit 5, Mockito-Kotlin, Testcontainers (MySQL)

## Repository Layout

- `src/main/kotlin/com/demo/tourwave/` — 런타임 코드 (헥사고날: `domain` / `application` / `adapter.in` / `adapter.out` / `bootstrap`)
- `src/main/kotlin/com/demo/tourwaveworker/` — 백그라운드 워커 진입점
- `src/test/kotlin/` — 단위/통합/컨트랙트 테스트
- `docs/` — 스펙 문서 (아래 표 참조)
- `scripts/` — CI/운영 스크립트

## Spec Documents (`docs/`)

| 파일 | 역할 |
|---|---|
| `docs/domain-rules.md` | 비즈니스 규칙 규범 (예약/대기열/오퍼/환불 상태 전이) |
| `docs/schema.md` | MySQL 스키마 규범 |
| `docs/openapi.yaml` | API 계약 SSOT (OpenAPI 3.x) |
| `docs/architecture.md` | 헥사고날 아키텍처 가드레일 + 구현 노트 |
| `docs/policies.md` | 권한 모델 + 운영 정책 + trust surface |
| `docs/testing.md` | 테스트 규약 + 실제 테스트 클래스 인덱스 |
| `docs/operations.md` | 런타임 토폴로지 + 운영 지표 + 환경 매트릭스 + 출시 체크리스트 |

규범 우선순위: `domain-rules.md` > `architecture.md` > `policies.md` > `schema.md` > `openapi.yaml`.

## Runtime Entry Points

- `TourwaveApplication` — API 서버
- `WorkerApplication` — 배치/스케줄러 워커

같은 Gradle 모듈에 공존하지만 운영 관점에서는 `API + background worker` 분리 구조로 취급한다.

## Build and Test

- 전체 테스트: `./gradlew test`
- OpenAPI 계약 회귀: `./gradlew test --tests 'com.demo.tourwave.agent.OpenApiContractVerificationTest'`
- 컨트롤러 drift 가드: `./gradlew test --tests 'com.demo.tourwave.agent.DocumentationBaselineTest'`
- 실 MySQL 컨테이너 회귀: `./gradlew test --tests 'com.demo.tourwave.adapter.out.persistence.jpa.RealMysqlContainerRegressionTest'`

## License

See `LICENSE`.
```

- [ ] **Step 2: 테스트 실행 (불변)**

Run: `./gradlew test`
Expected: PASS (README 추가만으로 테스트에 영향 없음)

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: add root README as entry point to spec documents

기존 agent/00_overview.md의 역할을 루트 README로 승격하고,
docs/ 아래 통합 스펙 문서로 가는 포인터 테이블만 유지한다."
```

---

## Task 3: docs/architecture.md 작성 (통합 신규)

**Files:**
- Create: `docs/architecture.md`

**흡수 원본:**
- `agent/10_architecture_hexagonal.md` (99줄) — 헥사고날 가드레일 전부
- `agent/06_implementation_notes.md` (71줄) — idempotency, audit, payment compensation, time boundary 노트

**목표 줄 수:** 약 130줄 (중복 제거 후)

- [ ] **Step 1: 원본 2개 파일을 다시 읽고 섹션 매핑 계획 수립**

Run: `wc -l agent/10_architecture_hexagonal.md agent/06_implementation_notes.md`

섹션 매핑 (순서대로):
1. Layer Definitions (원본 10번 §1)
2. Dependency Direction Rules (원본 10번 §2)
3. Package Guide (원본 10번 §3)
4. Implementation Rules (원본 10번 §4)
5. Test Discipline (원본 10번 §5, 세부는 `docs/testing.md`로 넘김)
6. Implementation Notes — idempotency / audit / payment compensation / time boundary / cross-document consistency (원본 06번 전체. 단, `docs/domain-rules.md`의 "Normative Addendum" 섹션과 겹치는 내용은 "상세는 domain-rules.md 참조" 링크로 대체)
7. Refactor Policy for Legacy Code (원본 10번 §6)
8. PR Checklist (원본 10번 §7 + 06번 체크리스트 병합, 중복 제거)

- [ ] **Step 2: `docs/architecture.md` 작성**

위 8개 섹션을 원본에서 그대로 가져오되:
- 원본 10번의 `agent/09_spec_index.md` 참조는 `README.md`로 경로 갱신
- 원본 06번에서 `agent/04_openapi.yaml`, `agent/08_operational_policy_tables.md`, `agent/02_schema_mysql.md` 참조는 각각 `docs/openapi.yaml`, `docs/policies.md`, `docs/schema.md`로 갱신
- 원본 06번의 idempotency 상세 설명이 domain-rules.md와 완전히 겹치면 "상세는 `docs/domain-rules.md` §Normative Addendum 참조" 한 줄로 대체

- [ ] **Step 3: 줄 수 및 검증**

Run: `wc -l docs/architecture.md`
Expected: 약 100~130줄 (원본 합 170줄에서 약 30~40% trim)

- [ ] **Step 4: 테스트 실행 (불변)**

Run: `./gradlew test`
Expected: PASS (새 파일만 추가, 기존 파일 그대로)

- [ ] **Step 5: Commit**

```bash
git add docs/architecture.md
git commit -m "docs: add consolidated architecture.md (hexagonal rules + impl notes)

기존 agent/10_architecture_hexagonal.md와 agent/06_implementation_notes.md를
하나의 문서로 통합한다. 두 원본은 Task 8에서 삭제된다."
```

---

## Task 4: docs/policies.md 작성 (통합 신규, 슬림화 포함)

**Files:**
- Create: `docs/policies.md`

**흡수 원본:**
- `agent/05_authz_model.md` (908줄) — 권한 매트릭스. **핵심 슬림화 대상.**
- `agent/08_operational_policy_tables.md` (164줄) — 운영 정책 테이블
- `agent/18_trust_surface_policy.md` (59줄) — trust surface no-build 결정

**목표 줄 수:** 약 400줄 (원본 합 1,131줄에서 약 65% trim)

- [ ] **Step 1: 원본 3개 파일 재독, 슬림화 전략 확정**

원본 05번 (authz 908줄)은 아래처럼 분해되어 있을 가능성이 큼:
- Role enumeration + per-role capability 설명 (텍스트가 중복되면 매트릭스로 응축)
- Resource × action × role 허용 표 (이 부분이 핵심, 유지)
- 예외 규칙/특수 케이스 (유지)
- 구현 참고 노트 (architecture.md와 겹치면 제거)

슬림화 원칙:
- 동일 규칙을 산문으로 여러 번 설명하는 경우 → 1회만
- 역할별 섹션이 같은 자원을 반복 기술하는 경우 → 자원 × 역할 매트릭스 한 표로 응축
- "why"가 domain-rules.md에 이미 있는 경우 → "Rule: ..." + "See `docs/domain-rules.md` §X" 만 남김

- [ ] **Step 2: `docs/policies.md` 작성, 섹션 순서**

1. Role Model (USER / INSTRUCTOR / ORG_MEMBER / ORG_ADMIN / ORG_OWNER) — 원본 05 요약
2. Resource × Action × Role Matrix — 원본 05의 핵심 매트릭스. 행: 자원(tour, occurrence, booking, inquiry, review, ...), 열: action(create, read, update, delete, approve, ...), 셀: 허용 역할 코드 (U/I/M/A/O)
3. Organization-Scoped Rules — 조직 범위 접근 규칙 (원본 05)
4. Operational Policy Tables — booking/offer/refund 운영 정책 (원본 08 테이블 그대로)
5. Trust Surface Policy — moderation no-build 결정과 근거 (원본 18 요약, 20줄 이내)
6. Cross-References — domain-rules.md / schema.md / openapi.yaml 링크 매핑

- [ ] **Step 3: 매트릭스 완전성 확인**

슬림화 과정에서 권한 매트릭스의 cell 하나도 누락되지 않도록:

Run: `grep -c "^|" agent/05_authz_model.md`  # 원본의 표 행 수

행 수 유지되는지 (혹은 통합 시 의도적으로 줄인 경우 주석) 확인.

- [ ] **Step 4: 줄 수 및 검증**

Run: `wc -l docs/policies.md`
Expected: 약 300~450줄

- [ ] **Step 5: 테스트 실행 (불변)**

Run: `./gradlew test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add docs/policies.md
git commit -m "docs: add consolidated policies.md (authz matrix + ops + trust surface)

기존 agent/05_authz_model.md (908줄), agent/08_operational_policy_tables.md,
agent/18_trust_surface_policy.md를 하나로 통합. authz는 resource × action × role
매트릭스로 응축해 약 65% 슬림화. 원본은 Task 8에서 삭제된다."
```

---

## Task 5: docs/testing.md 작성 (통합 신규)

**Files:**
- Create: `docs/testing.md`

**흡수 원본:**
- `agent/07_test_scenarios.md` (544줄)
- `agent/14_test_traceability_matrix.md` (129줄)

**목표 줄 수:** 약 200~250줄 (시나리오는 테스트 클래스 경로 인덱스로 대체)

- [ ] **Step 1: 원본 재독, 테스트 클래스 경로 수집**

Run: `find src/test -name "*IntegrationTest.kt" -o -name "*Test.kt" | sort`

원본 07의 각 시나리오가 어느 테스트 클래스로 구현되어 있는지 매핑.

- [ ] **Step 2: `docs/testing.md` 작성, 섹션 순서**

1. Test Layer Discipline — domain / application / adapter.in / adapter.out 별 테스트 책임 (원본 14 + 10 §5 요약)
2. Idempotency / Audit / Error Contract Rules — 원본 06/07에서 공유되는 mutation 테스트 필수 항목 요약
3. Scenario Traceability Matrix — 표 형식. 열: 시나리오, 테스트 클래스 (full path), 비고. 이게 대부분의 분량.
4. Contract / Drift Guards — `OpenApiContractVerificationTest`와 `DocumentationBaselineTest`의 역할 한 단락씩
5. Running Tests — 커맨드 표 (README와 중복되지 않도록 상세 케이스만, 기본은 README 링크)

- [ ] **Step 3: 줄 수 및 검증**

Run: `wc -l docs/testing.md`
Expected: 약 180~280줄

- [ ] **Step 4: 테스트 실행 (불변)**

Run: `./gradlew test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add docs/testing.md
git commit -m "docs: add consolidated testing.md (layer discipline + traceability)

기존 agent/07_test_scenarios.md와 agent/14_test_traceability_matrix.md를
통합. 시나리오 본문은 실제 테스트 클래스 경로를 가리키는 인덱스로
대체해 drift 가능성을 줄인다. 원본은 Task 8에서 삭제된다."
```

---

## Task 6: docs/operations.md 작성 (통합 신규)

**Files:**
- Create: `docs/operations.md`

**흡수 원본:**
- `agent/12_runtime_topology_and_operations.md` (150줄)
- `agent/19_launch_ops_baseline.md` (101줄)
- `docs/launch-readiness-checklist.md` (52줄)
- `docs/profile-env-matrix.md` (91줄)

**목표 줄 수:** 약 250줄

- [ ] **Step 1: `docs/operations.md` 작성, 섹션 순서**

1. Runtime Topology — API / Worker 진입점, 분산 락, actuator 경로 (원본 12)
2. Observability & Alerts — Prometheus 지표 이름, 핵심 알림 기준 (원본 19)
3. Profile Matrix — local / test / alpha / beta / real 의 config 차이와 secret 목록 (원본 `docs/profile-env-matrix.md`)
4. Launch Readiness Checklist — 출시 전 확인 항목 (원본 `docs/launch-readiness-checklist.md`)
5. Incident Runbook Pointers — 주요 장애 케이스와 대응 위치 (원본 19)

원본 간 중복 제거: 12번과 19번이 동일 actuator 지표를 설명하면 한 번만.

- [ ] **Step 2: 줄 수 및 검증**

Run: `wc -l docs/operations.md`
Expected: 약 200~300줄

- [ ] **Step 3: 테스트 실행 (불변)**

Run: `./gradlew test`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add docs/operations.md
git commit -m "docs: add consolidated operations.md (runtime + alerts + profile + launch)

기존 agent/12_runtime_topology, agent/19_launch_ops_baseline,
docs/launch-readiness-checklist, docs/profile-env-matrix를 통합한다.
원본은 Task 8에서 삭제된다."
```

---

## Task 7: 3개 파일 git mv + OpenApiContractVerificationTest 경로 갱신 (동일 커밋)

**Files:**
- Rename: `agent/01_domain_rules.md` → `docs/domain-rules.md`
- Rename: `agent/02_schema_mysql.md` → `docs/schema.md`
- Rename: `agent/04_openapi.yaml` → `docs/openapi.yaml`
- Modify: `src/test/kotlin/com/demo/tourwave/agent/OpenApiContractVerificationTest.kt`

**왜 동일 커밋**: `OpenApiContractVerificationTest`가 `Path.of("agent/04_openapi.yaml")`를 하드코딩하므로 `git mv`만 먼저 하면 테스트가 빨개진다. 이동과 테스트 경로 변경을 원자 단위로 묶는다.

- [ ] **Step 1: git mv 3회 실행**

```bash
git mv agent/01_domain_rules.md docs/domain-rules.md
git mv agent/02_schema_mysql.md docs/schema.md
git mv agent/04_openapi.yaml docs/openapi.yaml
```

- [ ] **Step 2: OpenApiContractVerificationTest.kt 경로 갱신**

파일: `src/test/kotlin/com/demo/tourwave/agent/OpenApiContractVerificationTest.kt`

`Path.of("agent/04_openapi.yaml")` 4곳을 전부 `Path.of("docs/openapi.yaml")`로 교체.

확인용:

Run: `grep -n 'agent/04_openapi.yaml' src/test/kotlin/com/demo/tourwave/agent/OpenApiContractVerificationTest.kt`
Expected (after edit): (빈 출력, 매치 없음)

Run: `grep -n 'docs/openapi.yaml' src/test/kotlin/com/demo/tourwave/agent/OpenApiContractVerificationTest.kt`
Expected (after edit): line 14, 30, 46, 62 (또는 원본 근처)

- [ ] **Step 3: OpenApi 컨트랙트 테스트 실행**

Run: `./gradlew test --tests 'com.demo.tourwave.agent.OpenApiContractVerificationTest'`
Expected: PASS (4개 테스트 메서드 모두)

- [ ] **Step 4: 전체 테스트 실행**

Run: `./gradlew test`
Expected: PASS 전부

- [ ] **Step 5: Commit**

```bash
git add agent/01_domain_rules.md docs/domain-rules.md
git add agent/02_schema_mysql.md docs/schema.md
git add agent/04_openapi.yaml docs/openapi.yaml
git add src/test/kotlin/com/demo/tourwave/agent/OpenApiContractVerificationTest.kt
git commit -m "refactor(docs): move domain-rules, schema, openapi into docs/

git mv로 이동해 히스토리를 보존한다. OpenApiContractVerificationTest의
하드코딩된 Path를 새 경로로 갱신하여 테스트를 그린으로 유지."
```

참고: `git add agent/01_...` 는 rename 상태를 스테이징하기 위한 관용 (새 경로/구 경로 둘 다 add해야 rename으로 인식). 만약 `git status`에서 이미 rename으로 표시되면 `git add docs/domain-rules.md` 만으로 충분할 수 있음.

---

## Task 8: 원본 흡수/삭제 대상 파일 일괄 git rm

**Files (git rm):**
- `agent/00_overview.md`
- `agent/03_api_catalog.md`
- `agent/05_authz_model.md`
- `agent/06_implementation_notes.md`
- `agent/07_test_scenarios.md`
- `agent/08_operational_policy_tables.md`
- `agent/09_spec_index.md`
- `agent/10_architecture_hexagonal.md`
- `agent/11_current_implementation_status.md`
- `agent/12_runtime_topology_and_operations.md`
- `agent/13_api_status_matrix.md`
- `agent/14_test_traceability_matrix.md`
- `agent/15_next_development_backlog.md`
- `agent/16_product_delivery_roadmap.md`
- `agent/17_release_gap_execution_plan.md`
- `agent/18_trust_surface_policy.md`
- `agent/19_launch_ops_baseline.md`
- `docs/launch-readiness-checklist.md`
- `docs/openapi-gap-report.md`
- `docs/profile-env-matrix.md`

**Preconditions check:** Task 1에서 `DocumentationBaselineTest`가 이 파일들을 더 이상 참조하지 않아야 한다. Task 2~6에서 이 파일들의 내용이 새 통합 문서로 흡수됐어야 한다. Task 7이 완료돼 `docs/openapi.yaml` 등 3개 이동 파일이 새 경로에 있어야 한다.

- [ ] **Step 1: 삭제 전 grep으로 잔존 참조 최종 확인**

Run: `grep -rn "agent/" src/ --include="*.kt"`
Expected: `OpenApiContractVerificationTest`의 **패키지 선언** `package com.demo.tourwave.agent` 등만 보여야 함. **파일 경로** `agent/XX_*.md` / `agent/04_openapi.yaml` 참조는 0건이어야 한다.

Run: `grep -rn "docs/launch-readiness-checklist\|docs/openapi-gap-report\|docs/profile-env-matrix" src/ --include="*.kt"`
Expected: (빈 출력)

실패 시: 해당 참조를 먼저 새 경로/통합 문서로 갱신한 뒤 본 Task 재개.

- [ ] **Step 2: git rm 실행**

```bash
git rm agent/00_overview.md \
       agent/03_api_catalog.md \
       agent/05_authz_model.md \
       agent/06_implementation_notes.md \
       agent/07_test_scenarios.md \
       agent/08_operational_policy_tables.md \
       agent/09_spec_index.md \
       agent/10_architecture_hexagonal.md \
       agent/11_current_implementation_status.md \
       agent/12_runtime_topology_and_operations.md \
       agent/13_api_status_matrix.md \
       agent/14_test_traceability_matrix.md \
       agent/15_next_development_backlog.md \
       agent/16_product_delivery_roadmap.md \
       agent/17_release_gap_execution_plan.md \
       agent/18_trust_surface_policy.md \
       agent/19_launch_ops_baseline.md \
       docs/launch-readiness-checklist.md \
       docs/openapi-gap-report.md \
       docs/profile-env-matrix.md
```

- [ ] **Step 3: agent/ 폴더가 비었는지 확인**

Run: `ls -la agent/ 2>/dev/null || echo "agent/ directory removed"`
Expected: `agent/ directory removed` (Git은 빈 디렉토리를 트래킹하지 않으므로 실제 파일시스템에도 없어야 함)

만약 남은 파일이 있으면 해당 파일의 처리가 계획에서 누락된 것 — Step 2를 중단하고 원인 분석.

- [ ] **Step 4: 전체 테스트 실행**

Run: `./gradlew test`
Expected: PASS 전부

- [ ] **Step 5: Commit**

```bash
git commit -m "refactor(docs): remove absorbed and obsolete spec files

agent/ 폴더 17개 파일과 docs/ 루트 3개 파일을 삭제한다.

흡수된 문서 (통합 대상):
- agent/05, 08, 18 → docs/policies.md
- agent/06, 10 → docs/architecture.md
- agent/07, 14 → docs/testing.md
- agent/12, 19, docs/launch-readiness-checklist, docs/profile-env-matrix → docs/operations.md
- agent/00 → README.md

삭제 (대체 가능):
- agent/03 (OpenAPI와 중복)
- agent/09 (메타 네비게이션, README로 대체)
- agent/11 (git log로 충분)
- agent/13 (OpenAPI + 테스트로 충분)
- agent/15 (이슈 트래커 역할)
- agent/16 (완료된 Sprint 기록)
- agent/17 (재출발 시 outdated)
- docs/openapi-gap-report (일회성 리포트)

agent/ 폴더 완전 제거."
```

---

## Task 9: 최종 검증 (grep 잔존 + 전체 테스트 + 줄 수 확인)

**Files:** (읽기만)

- [ ] **Step 1: agent/ 경로 잔존 참조 최종 검색**

Run: `grep -rn "agent/" . --include="*.md" --include="*.kt" --include="*.yml" --include="*.yaml" --exclude-dir=".git" --exclude-dir="build" --exclude-dir=".gradle"`

Expected: 0건 또는 다음 카테고리만 (허용):
- 본 spec/plan 문서 내 "이전 경로 agent/…" 같은 과거 언급 (본 plan 문서 자체 등)
- 테스트 파일의 `package com.demo.tourwave.agent` 패키지 선언(식별자, 폴더 아님)

실제 파일 경로 참조(`agent/XX.md`, `agent/04_openapi.yaml`)가 코드에 남아 있으면 해당 커밋으로 되돌아가 수정.

- [ ] **Step 2: `./gradlew test` 최종 그린 확인**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 목표 줄 수 확인**

Run: `wc -l docs/domain-rules.md docs/schema.md docs/architecture.md docs/policies.md docs/testing.md docs/operations.md README.md`

Expected: 총 약 1,500~2,000줄 (spec의 Success Criteria 기준). 초과 시 가장 큰 파일부터 추가 trim 가능하지만 필수 아님 — 규범 내용 보존이 우선.

- [ ] **Step 4: 디렉토리 구조 스냅샷**

Run: `ls docs/`
Expected:
```
architecture.md
domain-rules.md
openapi.yaml
operations.md
policies.md
schema.md
superpowers
testing.md
```

Run: `ls agent/ 2>/dev/null; echo exit=$?`
Expected: `exit=1` 혹은 `ls: cannot access`. (agent/ 폴더 없음)

- [ ] **Step 5: CI 워크플로 확인**

Run: `grep -n "com.demo.tourwave.agent" .github/workflows/ci.yml`
Expected: `./gradlew test --tests 'com.demo.tourwave.agent.OpenApiContractVerificationTest'` 한 줄이 그대로 남아 있어야 한다. (Kotlin 패키지 유지 전제)

- [ ] **Step 6: 검증만 수행한 task이므로 신규 커밋 없음**

모든 변경은 Task 1~8에서 커밋됐다. 이 task의 산출물은 "모든 Success Criteria 충족 확인"이다.

---

## Self-Review Summary

- **Spec coverage**: Success Criteria 6개 항목 전부 Task 8/9에서 검증됨. Goals 5개 항목 모두 Task로 연결. Non-Goals 위반 없음(런타임 코드 변경 없음).
- **Placeholder scan**: 각 Task는 실제 파일 경로, 실제 커맨드, 실제 커밋 메시지, (코드 변경 task는) 실제 Kotlin 코드를 포함. Task 3~6의 통합 문서 작성은 "섹션 목록 + 원본 매핑 + 슬림화 원칙 + 목표 줄 수"로 실행 가능한 가이드 제공.
- **Type consistency**: Kotlin 코드(Task 1, 7)에서 사용한 helper 이름 `assertGetMapping` 등이 Task 1 코드 블록과 원본 파일에서 동일. 참조 경로 `docs/openapi.yaml` / `docs/domain-rules.md` 등이 File Structure, Task 7, Task 9에서 일관.
- **Commit boundary**: 각 Task는 독립 커밋, `./gradlew test` 그린 유지. Task 7은 "git mv + test path 갱신"을 반드시 묶어 원자성 보장 명시.
