# Agent Folder Slimming — Design Spec

**Date:** 2026-04-15
**Status:** Approved for planning
**Scope:** `agent/` 폴더와 `docs/` 폴더의 스펙 문서 구조 재정비. 코드 변경 없음.

---

## 1. Context

Tourwave 저장소의 `agent/` 폴더에는 19개 markdown + 1개 OpenAPI yaml 파일이 누적되어 있다(총 5,245줄). `docs/`에도 운영 문서 3개가 추가로 있다. 다음과 같은 문제가 관찰됐다.

- **같은 사실의 다중 복제**: API 계약은 `04_openapi.yaml`, `03_api_catalog.md`, `13_api_status_matrix.md` 3곳에 흩어져 있다.
- **과거 기록이 현재 문서와 섞임**: Sprint 7~14 구현 기록(`16_product_delivery_roadmap.md`, 474줄)과 그 이후 계획(`17_release_gap_execution_plan.md`, 380줄)이 현재 상태 문서들과 같은 폴더에 존재한다.
- **현재 상태 문서의 중복**: `00_overview`, `11_current_implementation_status`, `12_runtime_topology`, `13_api_status_matrix`가 각기 다른 관점에서 같은 현황을 기술한다.
- **과대 문서**: `05_authz_model.md` 908줄, `02_schema_mysql.md` 682줄, `07_test_scenarios.md` 544줄로 유지보수가 어려운 규모.
- **메타 네비게이션 오버헤드**: `09_spec_index.md`가 "어느 문서를 읽어야 하는지"를 설명하기 위해 따로 존재한다. 문서 구조가 단순하면 필요 없다.

이 정리 작업의 **상위 목표**는 이후 세션에서 시작할 "Customer Frontend MVP 설계"가 깨끗한 스펙 위에서 진행되도록 하는 것이다. 프론트엔드 설계는 **본 spec의 범위가 아니며**, 별도 brainstorming 세션에서 진행한다.

## 2. Goals

- 신규 에이전트/개발자가 `README.md` + `agent/` 내 6개 파일(markdown 5 + openapi.yaml 1)만 읽고 프로젝트를 파악할 수 있어야 한다.
- `agent/` 파일 수 20 → 6, 줄 수 5,245 → 약 1,500~2,000줄(60~70% 감소).
- 삭제된 모든 정보는 (a) 통합 문서에 흡수되었거나, (b) git history / 코드 자체로 대체 가능해야 한다.
- 코드 동작은 변경하지 않는다. 테스트/빌드 그린 상태를 유지한다.

## 3. Non-Goals (Out of Scope)

- 백엔드 코드 수정, 리팩터링, 패키지 재정리.
- 프론트엔드 프로젝트 생성/설계 — 별도 세션에서 다룬다.
- `build.gradle.kts`의 Gradle 멀티모듈 분리.
- 도메인 규칙 자체의 변경 — 문서 표현만 다듬되 규범 내용은 보존한다.
- OpenAPI 계약 변경.

## 4. Principles

다음 3가지 원칙이 모든 결정을 지배한다.

1. **Single Source of Truth (SSOT)**: 같은 사실은 한 곳에만 적는다. 충돌 원인을 제거한다.
2. **과거 기록은 git이 한다**: 완료된 sprint/roadmap/gap 문서는 지운다. 커밋 히스토리에 맡긴다.
3. **규범 > 현재 상태 > 보조 3층 구조**: 남는 모든 문서는 이 3층 중 하나에 속해야 한다.

## 5. File-by-File Plan

### 5.1 유지 (Keep, 이름 유지)

| 파일 | 이유 |
|---|---|
| `agent/01_domain_rules.md` | 비즈니스 규칙 규범 문서. 유지. |
| `agent/02_schema_mysql.md` | DB 스키마 규범. 내용 유지, 표현만 trim 허용. |
| `agent/04_openapi.yaml` | API 계약의 SSOT. 유지. |

### 5.2 개명 (Rename)

| 현재 | 새 이름 | 이유 |
|---|---|---|
| `agent/10_architecture_hexagonal.md` | `agent/architecture.md` | 번호 prefix 제거. `06_implementation_notes.md` 내용 흡수. |

### 5.3 통합 (Merge, 새 파일 생성)

| 새 파일 | 흡수 대상 | 비고 |
|---|---|---|
| `agent/policies.md` | `05_authz_model.md` (908줄), `08_operational_policy_tables.md`, `18_trust_surface_policy.md` | 권한·운영 정책·trust surface를 하나의 문서로. 과대한 authz 내용은 슬림화. |
| `agent/testing.md` | `07_test_scenarios.md`, `14_test_traceability_matrix.md` | 실제 테스트 규약과 범위. 각 케이스는 테스트 코드를 가리키는 인덱스 형태로. |
| `docs/operations.md` | `12_runtime_topology_and_operations.md`, `19_launch_ops_baseline.md`, `docs/launch-readiness-checklist.md`, `docs/profile-env-matrix.md` | 실제 운영에 필요한 alert/health/profile 정보만 유지. |
| `README.md` (루트) | `00_overview.md` | 3~4줄 제품 한 줄 소개 + 저장소 구조 + 필독 문서 링크. 긴 설명은 제거. |

### 5.4 삭제 (Delete)

| 파일 | 이유 |
|---|---|
| `agent/00_overview.md` | README로 흡수 후 삭제. |
| `agent/03_api_catalog.md` | OpenAPI와 중복. |
| `agent/06_implementation_notes.md` | `architecture.md`에 흡수 후 삭제. |
| `agent/09_spec_index.md` | 메타 네비게이션은 README가 대체. |
| `agent/11_current_implementation_status.md` | git log/코드가 사실. 문서는 즉시 낡는다. |
| `agent/13_api_status_matrix.md` | OpenAPI와 테스트 스위트가 사실. |
| `agent/15_next_development_backlog.md` | 39줄. 이슈 트래커의 역할. |
| `agent/16_product_delivery_roadmap.md` | 완료된 Sprint 7~14 기록. git log로 충분. |
| `agent/17_release_gap_execution_plan.md` | Sprint 15~20 계획. 재출발 시 outdated. |
| `docs/openapi-gap-report.md` | 일회성 리포트. |

### 5.5 흡수 후 삭제

| 파일 | 흡수 대상 |
|---|---|
| `agent/05_authz_model.md` | → `agent/policies.md` |
| `agent/06_implementation_notes.md` | → `agent/architecture.md` |
| `agent/07_test_scenarios.md` | → `agent/testing.md` |
| `agent/08_operational_policy_tables.md` | → `agent/policies.md` |
| `agent/12_runtime_topology_and_operations.md` | → `docs/operations.md` |
| `agent/14_test_traceability_matrix.md` | → `agent/testing.md` |
| `agent/18_trust_surface_policy.md` | → `agent/policies.md` |
| `agent/19_launch_ops_baseline.md` | → `docs/operations.md` |
| `docs/launch-readiness-checklist.md` | → `docs/operations.md` |
| `docs/profile-env-matrix.md` | → `docs/operations.md` |

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
└── profile-env-matrix.md
```
(20 files in agent/, 3 files in docs/, ≈5,245 lines of markdown)

**After:**

```
README.md                          # 제품 한 줄 요약 + 필독 문서 링크 (신규/갱신)
agent/
├── 01_domain_rules.md             # 유지 (비즈니스 규칙)
├── 02_schema_mysql.md             # 유지 (DB 스키마)
├── 04_openapi.yaml                # 유지 (API 계약 SSOT)
├── architecture.md                # 기존 10번 + 06번 흡수
├── policies.md                    # 기존 05 + 08 + 18 흡수, 슬림화
└── testing.md                     # 기존 07 + 14 흡수
docs/
└── operations.md                  # 기존 12 + 19 + launch-readiness + profile-env 흡수
```
(5 files in agent/, 1 file in docs/, 1 README, 목표 ≈1,500~2,000 lines)

## 7. Execution Steps

1. **신설/통합 문서 초안 작성** (순서 중요)
   - `agent/architecture.md` — 기존 `10_architecture_hexagonal.md` + `06_implementation_notes.md` 본문 결합, 중복 제거
   - `agent/policies.md` — 기존 `05` + `08` + `18` 본문 결합, authz 908줄 대상으로 슬림화(역할/권한 매트릭스 형식을 유지하되 설명 반복 제거)
   - `agent/testing.md` — `07` + `14`. 각 시나리오는 실제 테스트 클래스 경로로 가리키기(인덱스화)
   - `docs/operations.md` — `12` + `19` + `docs/launch-readiness-checklist.md` + `docs/profile-env-matrix.md` 흡수
   - `README.md` — 제품 한 줄 요약, 기술 스택, 디렉토리 구조, `agent/` 5개 파일 역할 한 줄씩, 빌드/테스트 커맨드

2. **리뷰 루프 (self-check)**
   - 신설 문서만 읽고 규범/현재 상태/운영 지식이 이해 가능한가?
   - 기존 문서에만 있는 사실 중 누락된 것이 있는가? → 있으면 보강
   - 중복/반복 표현이 여전히 있는가? → 추가 trim

3. **삭제**
   - 모든 통합이 끝난 뒤 `git rm`으로 기존 파일 제거
   - **단일 커밋**으로 묶어 히스토리에서 역추적 가능하게 함

4. **코드 내부 참조 점검**
   - `grep -r "agent/0[0-9]_"` 등으로 코드/테스트/주석에서 이전 경로 참조 검색
   - 발견되면 새 경로로 수정 (또는 해당 참조가 규범성이 없으면 제거)

5. **검증**
   - `./gradlew test` 그린 유지 (문서 변경이므로 사실상 영향 없음, 안전 확인용)
   - `agent/` 폴더 이름은 유지 (기존 에이전트 자동 로드 경로 보존)

## 8. Success Criteria

- [ ] `agent/` 디렉토리: 20 → 6 파일 (markdown 5 + openapi.yaml 1)
- [ ] `docs/` 디렉토리: 3 → 1 파일
- [ ] 전체 markdown 총 줄 수 60~70% 감소 (5,245 → 목표 1,500~2,000)
- [ ] 삭제된 모든 파일의 내용이 (a) 통합 문서에 흡수됐거나, (b) git/코드로 대체 가능함을 커밋 메시지에 명시
- [ ] `./gradlew test` 통과
- [ ] 코드/테스트 내의 이전 문서 경로 참조가 전부 갱신되었거나 제거됨

## 9. Follow-up (이번 spec 이후)

이 정리가 끝나면 다음 세션에서 **Customer Frontend MVP 설계**를 시작한다. 합의된 기본 방향은 다음과 같으나, 세부 설계는 별도 brainstorming 세션에서 진행한다.

- 기술 스택: Next.js + TypeScript
- 레포 구조: 같은 저장소 내 `/backend` (현재 gradle) + `/frontend` 신설
- 1차 범위: Customer surface 우선 (catalog 검색, 상세, 예약, 내 예약)
- 백엔드는 기존 JWT API를 그대로 사용(코드 유지)

## 10. Risk & Mitigation

| 위험 | 완화 |
|---|---|
| 통합 과정에서 규범적 사실 누락 | 각 통합 전 원본 파일 재독 → 신설 문서 목차에 체크리스트로 매핑 |
| 이전 문서 경로를 참조하는 테스트/코드/주석 존재 | 삭제 전 `grep` 수행, 발견 시 갱신 |
| `05_authz_model` 908줄 슬림화 중 권한 매트릭스 누락 | 권한 행렬(role × resource × action)을 표 형식으로 강제, 설명 텍스트만 축약 |
| 리뷰어(사용자)가 특정 삭제 대상을 살리고 싶어함 | 본 spec 승인 시점에 확정. 이후 새 요청은 별도 수정 |

## 11. Open Questions

- 본 spec 실행 시점의 `README.md`: 루트에 이미 `LICENSE`는 있고 `README.md`는 없다. 신규 작성한다.
- 기존 `agent/` 폴더 이름 변경 여부: **유지** 권고 (에이전트 자동 로드 경로). 사용자 최종 확인 필요 시 추가 질문.
