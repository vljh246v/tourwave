# Spec Docs Consolidation — Design Spec

**Date:** 2026-04-15
**Status:** Approved for planning
**Scope:** `agent/` 폴더와 `docs/` 폴더의 스펙 문서 구조를 하나의 `docs/` 아래로 통합·정리하고, 문서 drift 가드 테스트를 함께 업데이트한다. 백엔드 코드 동작은 변경하지 않는다.

---

## 1. Context

Tourwave 저장소의 스펙 문서는 두 폴더에 분산돼 있다.

- `agent/` (총 20개 파일): `00_overview` ~ `19_launch_ops_baseline` + `04_openapi.yaml`, 총 5,245줄
- `docs/` (총 3개 파일): `launch-readiness-checklist`, `openapi-gap-report`, `profile-env-matrix`, 약 199줄

다음과 같은 문제가 관찰됐다.

- **두 폴더로 분리된 이유 불명확**: `agent/`는 이 저장소가 agent-driven 개발로 출발한 역사적 네이밍. 지금은 일반 스펙 문서와 큰 차이가 없다. 폴더 하나로 통합하는 것이 표준적이다.
- **같은 사실의 다중 복제**: API 계약은 `04_openapi.yaml`, `03_api_catalog.md`, `13_api_status_matrix.md` 3곳에 흩어져 있다.
- **과거 기록이 현재 문서와 섞임**: Sprint 7~14 구현 기록(`16_product_delivery_roadmap.md`, 474줄)과 그 이후 계획(`17_release_gap_execution_plan.md`, 380줄)이 현재 상태 문서들과 같은 폴더에 존재한다.
- **현재 상태 문서의 중복**: `00_overview`, `11_current_implementation_status`, `12_runtime_topology`, `13_api_status_matrix`가 각기 다른 관점에서 같은 현황을 기술한다.
- **과대 문서**: `05_authz_model.md` 908줄, `02_schema_mysql.md` 682줄, `07_test_scenarios.md` 544줄로 유지보수가 어려운 규모.
- **메타 네비게이션 오버헤드**: `09_spec_index.md`가 "어느 문서를 읽어야 하는지"를 설명하기 위해 따로 존재한다. 문서 구조가 단순하면 필요 없다.
- **문서 drift 가드 테스트가 삭제 대상 문서를 참조**: `src/test/kotlin/com/demo/tourwave/agent/DocumentationBaselineTest.kt`와 `OpenApiContractVerificationTest.kt`가 `agent/04_openapi.yaml`, `agent/13_api_status_matrix.md` 등 본 정리에서 삭제·통합 대상인 파일들을 파일시스템에서 직접 읽어 검증한다. 문서 정리와 테스트 업데이트는 **반드시 한 번에** 이뤄져야 한다.

이 정리 작업의 **상위 목표**는 이후 세션에서 시작할 "Customer Frontend MVP 설계"가 깨끗한 스펙 위에서 진행되도록 하는 것이다. 프론트엔드 설계는 **본 spec의 범위가 아니며**, 별도 brainstorming 세션에서 진행한다.

## 2. Goals

- 신규 에이전트/개발자가 `README.md` + `docs/` 내 7개 파일(markdown 6 + openapi.yaml 1)만 읽고 프로젝트를 파악할 수 있어야 한다.
- `agent/` + `docs/` 루트에 있던 문서 파일 수 23 → 7, 줄 수 5,245+199 → 약 1,500~2,000줄(약 65~70% 감소).
- `agent/` 폴더는 완전히 제거하고 모든 문서는 `docs/` 아래에 둔다.
- 삭제된 모든 정보는 (a) 통합 문서에 흡수되었거나, (b) git history / 코드 자체로 대체 가능해야 한다.
- 문서 drift 가드 테스트(`DocumentationBaselineTest`, `OpenApiContractVerificationTest`)를 새 문서 구조에 맞춰 업데이트하여 `./gradlew test` 그린 상태를 유지한다.

## 3. Non-Goals (Out of Scope)

- 백엔드 **런타임** 코드 수정, 리팩터링, 패키지 재정리 (테스트 코드만 drift 가드 업데이트를 위해 터치함).
- 프론트엔드 프로젝트 생성/설계 — 별도 세션에서 다룬다.
- `build.gradle.kts`의 Gradle 멀티모듈 분리.
- 도메인 규칙 자체의 변경 — 문서 표현만 다듬되 규범 내용은 보존한다.
- OpenAPI 계약 내용 변경 (경로만 `agent/04_openapi.yaml` → `docs/openapi.yaml`로 이동).
- 테스트 **Kotlin 패키지 이름** 변경 (`com.demo.tourwave.agent` 유지). 폴더 이름과는 별개의 식별자로 취급한다.

## 4. Principles

다음 3가지 원칙이 모든 결정을 지배한다.

1. **Single Source of Truth (SSOT)**: 같은 사실은 한 곳에만 적는다. 충돌 원인을 제거한다.
2. **과거 기록은 git이 한다**: 완료된 sprint/roadmap/gap 문서는 지운다. 커밋 히스토리에 맡긴다.
3. **규범 > 현재 상태 > 보조 3층 구조**: 남는 모든 문서는 이 3층 중 하나에 속해야 한다.

보조 원칙:

- 파일 이름에서 번호 prefix(`01_`, `02_`, …)를 제거하고 kebab-case를 쓴다. 문서가 적어지면 읽기 순서는 이름으로 충분히 드러난다.

## 5. File-by-File Plan

모든 최종 산출물은 `docs/` 아래에 둔다. `agent/` 폴더는 완전히 제거한다.

### 5.1 이동 + 개명만 (내용 변경 없음 / 표현 trim만 허용)

| 현재 | 새 경로 | 비고 |
|---|---|---|
| `agent/01_domain_rules.md` | `docs/domain-rules.md` | 비즈니스 규칙 규범. 내용 유지. |
| `agent/02_schema_mysql.md` | `docs/schema.md` | DB 스키마 규범. 표현만 trim 허용. |
| `agent/04_openapi.yaml` | `docs/openapi.yaml` | API 계약 SSOT. 내용 동일. |

### 5.2 통합 (새 파일 생성, 여러 기존 파일 흡수)

| 새 파일 | 흡수 대상 | 비고 |
|---|---|---|
| `docs/architecture.md` | `agent/10_architecture_hexagonal.md` + `agent/06_implementation_notes.md` | 헥사고날 가드레일 + 구현 노트 통합. |
| `docs/policies.md` | `agent/05_authz_model.md` (908줄) + `agent/08_operational_policy_tables.md` + `agent/18_trust_surface_policy.md` | 권한·운영 정책·trust surface 통합. authz는 역할×자원×액션 매트릭스로 슬림화하되 규범 내용 보존. |
| `docs/testing.md` | `agent/07_test_scenarios.md` + `agent/14_test_traceability_matrix.md` | 각 시나리오는 실제 테스트 클래스 경로 인덱스로. |
| `docs/operations.md` | `agent/12_runtime_topology_and_operations.md` + `agent/19_launch_ops_baseline.md` + `docs/launch-readiness-checklist.md` + `docs/profile-env-matrix.md` | alert/health/profile/launch 체크리스트 통합. |
| `README.md` (루트 신규) | `agent/00_overview.md` | 제품 한 줄 요약 + 기술 스택 + `docs/` 파일 역할 한 줄씩 + 빌드/테스트 커맨드. 긴 설명 제거. |

### 5.3 삭제 (어디에도 흡수되지 않음)

| 파일 | 이유 |
|---|---|
| `agent/03_api_catalog.md` | OpenAPI와 중복. |
| `agent/09_spec_index.md` | 메타 네비게이션. README로 대체. |
| `agent/11_current_implementation_status.md` | git log/코드가 사실. 즉시 낡는 문서. |
| `agent/13_api_status_matrix.md` | OpenAPI와 테스트 스위트가 사실. |
| `agent/15_next_development_backlog.md` | 39줄. 이슈 트래커의 역할. |
| `agent/16_product_delivery_roadmap.md` | 완료된 Sprint 7~14 기록. git log로 충분. |
| `agent/17_release_gap_execution_plan.md` | Sprint 15~20 계획. 재출발 시 outdated. |
| `docs/openapi-gap-report.md` | 일회성 리포트. |

### 5.4 테스트 및 CI 업데이트 (필수, 문서 정리와 같은 커밋에)

| 파일 | 변경 |
|---|---|
| `src/test/kotlin/com/demo/tourwave/agent/OpenApiContractVerificationTest.kt` | `Path.of("agent/04_openapi.yaml")` 4곳 → `Path.of("docs/openapi.yaml")`. 나머지 로직/assert 유지. |
| `src/test/kotlin/com/demo/tourwave/agent/DocumentationBaselineTest.kt` | 삭제 대상 문서(`13_api_status_matrix`, `09_spec_index`, `03_api_catalog`, `18_trust_surface_policy`, `19_launch_ops_baseline`, `docs/openapi-gap-report`, `docs/launch-readiness-checklist`, `agent/14_test_traceability_matrix`)에 대한 `readProjectFile` + assertContains 블록을 모두 제거. **컨트롤러 mapping assertion(`drift-prone controller mappings stay aligned with current truth docs`)은 유지한다** — 이건 drift 방지에 실질적 가치가 있다. 이름은 그대로 유지하거나 `ControllerMappingContractTest` 등 역할에 맞게 재네이밍 가능. 테스트 Kotlin 패키지(`com.demo.tourwave.agent`)는 건드리지 않는다. |
| `.github/workflows/ci.yml` | `./gradlew test --tests 'com.demo.tourwave.agent.OpenApiContractVerificationTest'` 줄은 그대로 둔다(패키지 이름 유지 전제). 만약 테스트 클래스명을 바꿨다면 CI의 `--tests` 인자도 함께 수정. |

테스트 축소 기준:

- **유지**: OpenAPI paths 존재/부재 검증, 컨트롤러 어노테이션 경로 검증, security scheme 검증.
- **제거**: 특정 markdown 문서에 특정 문구가 들어있는지 확인하는 모든 assertion. 이는 SSOT 원칙에 반하고 문서 편집 시마다 깨진다.

## 6. Target Structure (Before → After)

**Before:**

```
agent/
├── 00_overview.md
├── 01_domain_rules.md
├── 02_schema_mysql.md
├── 03_api_catalog.md
├── 04_openapi.yaml
├── 05_authz_model.md
├── 06_implementation_notes.md
├── 07_test_scenarios.md
├── 08_operational_policy_tables.md
├── 09_spec_index.md
├── 10_architecture_hexagonal.md
├── 11_current_implementation_status.md
├── 12_runtime_topology_and_operations.md
├── 13_api_status_matrix.md
├── 14_test_traceability_matrix.md
├── 15_next_development_backlog.md
├── 16_product_delivery_roadmap.md
├── 17_release_gap_execution_plan.md
├── 18_trust_surface_policy.md
└── 19_launch_ops_baseline.md
docs/
├── launch-readiness-checklist.md
├── openapi-gap-report.md
├── profile-env-matrix.md
└── superpowers/
    └── specs/
        └── 2026-04-15-agent-folder-slimming-design.md
```
(20 files in agent/, 3 files in docs/ root, ≈5,444 lines of markdown)

**After:**

```
README.md                              # 제품 한 줄 요약 + 필독 문서 링크 (신규)
docs/
├── domain-rules.md                    # 기존 agent/01 (이동+개명)
├── schema.md                          # 기존 agent/02 (이동+개명)
├── openapi.yaml                       # 기존 agent/04 (이동+개명)
├── architecture.md                    # 기존 agent/10 + 06 흡수
├── policies.md                        # 기존 agent/05 + 08 + 18 흡수, 슬림화
├── testing.md                         # 기존 agent/07 + 14 흡수
├── operations.md                      # 기존 agent/12 + 19 + docs/launch-readiness + docs/profile-env 흡수
└── superpowers/
    └── specs/
        └── 2026-04-15-agent-folder-slimming-design.md   # 본 spec 유지
```
(7 files in docs/ root, 1 README, `agent/` 폴더 완전 삭제. 목표 ≈1,500~2,000 lines)

## 7. Execution Steps

1. **신설/통합 문서 초안 작성** (순서 중요)
   - `docs/architecture.md` — 기존 `agent/10_architecture_hexagonal.md` + `agent/06_implementation_notes.md` 본문 결합, 중복 제거.
   - `docs/policies.md` — 기존 `agent/05` + `08` + `18` 본문 결합. authz 908줄은 역할×자원×액션 매트릭스 표 형식으로 재구성하며 설명 반복 제거.
   - `docs/testing.md` — `agent/07` + `14`. 각 시나리오는 실제 테스트 클래스 경로로 가리키기.
   - `docs/operations.md` — `agent/12` + `19` + `docs/launch-readiness-checklist.md` + `docs/profile-env-matrix.md` 흡수.
   - `README.md` (루트) — 제품 한 줄 요약, 기술 스택, 디렉토리 구조, `docs/` 6개 파일 역할 한 줄씩, 빌드/테스트 커맨드.

2. **단순 이동 수행**
   - `git mv agent/01_domain_rules.md docs/domain-rules.md`
   - `git mv agent/02_schema_mysql.md docs/schema.md`
   - `git mv agent/04_openapi.yaml docs/openapi.yaml`

3. **테스트 업데이트**
   - `OpenApiContractVerificationTest.kt`: `Path.of("agent/04_openapi.yaml")` → `Path.of("docs/openapi.yaml")` 4곳.
   - `DocumentationBaselineTest.kt`: 삭제 대상 문서 관련 assert 블록 제거. 컨트롤러 mapping assertion만 유지.
   - 파일시스템에서 문서를 읽는 모든 하드코딩 경로 확인: `grep -rn "Path.of(\"agent/" src/ docs/` / `grep -rn "agent/0" src/`.

4. **리뷰 루프 (self-check)**
   - 신설 문서만 읽고 규범/현재 상태/운영 지식이 이해 가능한가?
   - 기존 문서에만 있는 사실 중 누락된 것이 있는가? → 있으면 보강.
   - 중복/반복 표현이 여전히 있는가? → 추가 trim.
   - 테스트의 assert 문구에서 이전 경로/파일명을 참조하는 것이 더 이상 없는가?

5. **삭제**
   - 모든 통합/이동이 끝난 뒤 `git rm`으로 기존 파일 제거.
   - `agent/` 폴더가 비면 Git이 자동으로 트래킹 해제한다.
   - 삭제, 생성, 이동, 테스트 업데이트를 **하나의 단일 커밋**으로 묶는다.

6. **검증**
   - `./gradlew test` 로컬 그린 확인.
   - 구체적 회귀 타깃: `./gradlew test --tests 'com.demo.tourwave.agent.OpenApiContractVerificationTest'`, `./gradlew test --tests 'com.demo.tourwave.agent.DocumentationBaselineTest'` (또는 재네이밍 시 새 이름).
   - `grep -rn "agent/" . --include="*.kt" --include="*.yml" --include="*.md"` 로 잔존 참조 최종 확인.

## 8. Success Criteria

- [ ] `agent/` 디렉토리 완전 삭제.
- [ ] `docs/` 루트: 정확히 7 파일 — `domain-rules.md`, `schema.md`, `openapi.yaml`, `architecture.md`, `policies.md`, `testing.md`, `operations.md`. `docs/superpowers/specs/`는 본 spec 보관용으로 유지.
- [ ] 루트에 `README.md` 신규 작성.
- [ ] 전체 스펙 markdown 총 줄 수 약 65~70% 감소 (5,245+199 → 목표 1,500~2,000).
- [ ] 삭제된 모든 파일의 내용이 (a) 통합 문서에 흡수됐거나, (b) git/코드로 대체 가능함을 커밋 메시지에 명시.
- [ ] `./gradlew test` 통과. 특히 `OpenApiContractVerificationTest`와 `DocumentationBaselineTest`가 새 경로 기준으로 그린.
- [ ] `grep` 결과 코드/테스트/CI/주석에서 `agent/` 경로 참조 0건 (Kotlin 패키지 `com.demo.tourwave.agent`는 예외, 식별자로 취급).

## 9. Follow-up (이번 spec 이후)

이 정리가 끝나면 다음 세션에서 **Customer Frontend MVP 설계**를 시작한다. 합의된 기본 방향은 다음과 같으나, 세부 설계는 별도 brainstorming 세션에서 진행한다.

- 기술 스택: Next.js + TypeScript
- 레포 구조: 같은 저장소 내 `/backend`(현재 Gradle 코드를 이 디렉토리로 이동) + `/frontend`(신설)
- 1차 범위: Customer surface 우선 (catalog 검색, 상세, 예약, 내 예약)
- 백엔드는 기존 JWT API를 그대로 사용 (런타임 코드 변경 없음)

주의: 위 `/backend` 디렉토리 이동은 **이번 spec의 범위가 아니다**. 프론트엔드 도입 시점에 별도 작업 항목으로 다룬다.

## 10. Risk & Mitigation

| 위험 | 완화 |
|---|---|
| 통합 과정에서 규범적 사실 누락 | 각 통합 전 원본 파일 재독 → 신설 문서 목차에 체크리스트로 매핑. 리뷰 루프(Execution Step 4)에서 한 번 더 확인. |
| 테스트가 삭제 대상 문서를 참조하므로 정리 PR이 바로 빨간 빌드를 만듦 | 문서 변경과 테스트 업데이트를 **같은 커밋**으로 묶는다. 로컬에서 `./gradlew test` 확인 후 푸시. |
| `05_authz_model` 908줄 슬림화 중 권한 매트릭스 누락 | 권한 행렬(role × resource × action)을 표 형식으로 강제, 설명 텍스트만 축약. 슬림화 전후 룰 수를 세서 비교. |
| 파일 이동에 `git mv` 대신 삭제+추가를 쓰면 히스토리 끊김 | 이동 대상은 반드시 `git mv` 사용 (표현 trim은 이동 커밋 이후 별도 단계). |
| CI가 `com.demo.tourwave.agent.OpenApiContractVerificationTest`를 명시적으로 지목 | 테스트 Kotlin 패키지는 유지(Non-Goals). 클래스명 변경 시 CI `--tests` 인자 동시 갱신. |
| 리뷰어가 특정 삭제 대상을 살리고 싶어함 | 본 spec 승인 시점에 확정. 이후 추가 요청은 별도 spec 또는 수정. |

## 11. Open Questions (Resolved)

- ~~기존 `agent/` 폴더 이름 변경 여부~~ → **해결**: `agent/` 제거, 모든 문서 `docs/` 아래로 통합.
- ~~`README.md`: 루트에 없음~~ → **해결**: 신규 작성.
- ~~테스트 Kotlin 패키지 `com.demo.tourwave.agent`를 바꿀 것인가~~ → **해결**: 유지. 폴더 이름과 혼동되지만 복잡도 최소화 우선.
