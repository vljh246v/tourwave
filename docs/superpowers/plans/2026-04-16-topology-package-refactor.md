# topology 패키지 정렬 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `application/topology`(+`persistence/topology`) 패키지를 domain과 일치하도록 `organization`, `instructor`, `tour`, `occurrence` 4개로 분리하고, `application/communication`을 `application/announcement`로 정렬한다.

**Architecture:** 코드 로직은 전혀 변경하지 않는다. `package` 선언과 `import` 문만 수정한다. 각 파일의 이동은 (1) 새 경로에 파일 생성 (package 변경 포함), (2) 해당 파일을 참조하는 모든 파일의 import 수정, (3) 구 파일 삭제 순서로 진행한다.

**Tech Stack:** Kotlin, Spring Boot 3.3.1, Gradle

---

## 실행 순서 (의존성 그래프)

```
Task 1 (organization) ──────────────────────────────┐
                                                     ▼
Task 2 (instructor) ─── [Task 1 완료 후] ──────────┐ │
Task 3 (tour)       ─── [Task 1 완료 후] ──────────┤ │
                                                   ▼ ▼
Task 4 (occurrence) ─── [Tasks 1+2+3 완료 후] ────▶ Phase 1 Compile Check
                                                         │
             ┌───────────────────────────────────────────┘
             ▼
Tasks 5+6+7 (persistence, PARALLEL) ──────▶ Phase 2 Compile Check
             │
Task 8 (JpaJsonCodec) [Task 7과 병렬 가능]
             │
             ▼
Task 9 (bootstrap split) ──────────────────▶
             │
Tasks 10+11 (web adapter + other bootstrap, PARALLEL)
             │
Task 12 (communication → announcement) [독립, 언제든 실행 가능]
             │
Task 13 (test files) ──────────────────────▶ Final Compile + Test
```

---

## Phase 1: application/topology 분리

### Task 1: application/organization 패키지 생성

**의존성:** 없음 (독립 실행 가능)

**새로 만들 디렉토리:**
- `src/main/kotlin/com/demo/tourwave/application/organization/`
- `src/main/kotlin/com/demo/tourwave/application/organization/port/`

**이동할 파일 (9개):**

| 구 경로 (application/topology/) | 신 경로 (application/organization/) |
|---|---|
| `OrganizationAccessGuard.kt` | `organization/OrganizationAccessGuard.kt` |
| `OrganizationCommandService.kt` | `organization/OrganizationCommandService.kt` |
| `OrganizationInvitationDeliveryService.kt` | `organization/OrganizationInvitationDeliveryService.kt` |
| `OrganizationMembershipService.kt` | `organization/OrganizationMembershipService.kt` |
| `OrganizationModels.kt` | `organization/OrganizationModels.kt` |
| `OrganizationQueryService.kt` | `organization/OrganizationQueryService.kt` |
| `OrganizationValidation.kt` | `organization/OrganizationValidation.kt` |
| `port/OrganizationRepository.kt` | `organization/port/OrganizationRepository.kt` |
| `port/OrganizationMembershipRepository.kt` | `organization/port/OrganizationMembershipRepository.kt` |

---

- [ ] **Step 1-1: OrganizationRepository.kt 이동**

`src/main/kotlin/com/demo/tourwave/application/organization/port/OrganizationRepository.kt` 생성.
파일 내용은 `application/topology/port/OrganizationRepository.kt`와 동일하되 첫 줄만 변경:

```
// 변경 전
package com.demo.tourwave.application.topology.port

// 변경 후
package com.demo.tourwave.application.organization.port
```

그 후 `src/main/kotlin/com/demo/tourwave/application/topology/port/OrganizationRepository.kt` 삭제.

- [ ] **Step 1-2: OrganizationMembershipRepository.kt 이동**

`src/main/kotlin/com/demo/tourwave/application/organization/port/OrganizationMembershipRepository.kt` 생성.

```
// 변경 전
package com.demo.tourwave.application.topology.port

// 변경 후
package com.demo.tourwave.application.organization.port
```

`src/main/kotlin/com/demo/tourwave/application/topology/port/OrganizationMembershipRepository.kt` 삭제.

- [ ] **Step 1-3: OrganizationModels.kt 이동**

`src/main/kotlin/com/demo/tourwave/application/organization/OrganizationModels.kt` 생성.

```
// 변경 전
package com.demo.tourwave.application.topology

// 변경 후
package com.demo.tourwave.application.organization
```

import 변경 없음 (topology import 없음). 구 파일 삭제.

- [ ] **Step 1-4: OrganizationValidation.kt 이동**

`src/main/kotlin/com/demo/tourwave/application/organization/OrganizationValidation.kt` 생성.

```
// 변경 전
package com.demo.tourwave.application.topology

// 변경 후
package com.demo.tourwave.application.organization
```

import 변경 없음. 구 파일 삭제.

- [ ] **Step 1-5: OrganizationAccessGuard.kt 이동**

`src/main/kotlin/com/demo/tourwave/application/organization/OrganizationAccessGuard.kt` 생성.

```
// 변경 전
package com.demo.tourwave.application.topology
import com.demo.tourwave.application.topology.port.OrganizationMembershipRepository
import com.demo.tourwave.application.topology.port.OrganizationRepository

// 변경 후
package com.demo.tourwave.application.organization
import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
import com.demo.tourwave.application.organization.port.OrganizationRepository
```

구 파일 삭제.

- [ ] **Step 1-6: OrganizationCommandService.kt 이동**

```
// 변경 전
package com.demo.tourwave.application.topology
import com.demo.tourwave.application.topology.port.OrganizationMembershipRepository
import com.demo.tourwave.application.topology.port.OrganizationRepository

// 변경 후
package com.demo.tourwave.application.organization
import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
import com.demo.tourwave.application.organization.port.OrganizationRepository
```

`OrganizationAccessGuard`, `OrganizationModels` 참조는 동일 패키지이므로 import 불필요. 구 파일 삭제.

- [ ] **Step 1-7: OrganizationMembershipService.kt 이동**

```
// 변경 전
package com.demo.tourwave.application.topology
import com.demo.tourwave.application.topology.port.OrganizationMembershipRepository

// 변경 후
package com.demo.tourwave.application.organization
import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
```

`OrganizationAccessGuard` 참조는 동일 패키지이므로 import 불필요. 구 파일 삭제.

- [ ] **Step 1-8: OrganizationQueryService.kt 이동**

```
// 변경 전
package com.demo.tourwave.application.topology
import com.demo.tourwave.application.topology.port.OrganizationMembershipRepository
import com.demo.tourwave.application.topology.port.OrganizationRepository

// 변경 후
package com.demo.tourwave.application.organization
import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
import com.demo.tourwave.application.organization.port.OrganizationRepository
```

구 파일 삭제.

- [ ] **Step 1-9: OrganizationInvitationDeliveryService.kt 이동**

```
// 변경 전
package com.demo.tourwave.application.topology
import com.demo.tourwave.application.topology.port.OrganizationRepository

// 변경 후
package com.demo.tourwave.application.organization
import com.demo.tourwave.application.organization.port.OrganizationRepository
```

구 파일 삭제.

- [ ] **Step 1-10: 컴파일 검증**

```bash
./gradlew compileKotlin 2>&1 | grep -E "error:|warning:" | head -20
```

`application.topology.Organization*` 관련 에러가 없어야 한다. 이 시점에서 topology 패키지에는 Instructor/Tour/Occurrence 파일만 남아있으므로, 해당 파일들에 대한 에러는 정상이다 (아직 이동 전).

- [ ] **Step 1-11: 커밋**

```bash
git add src/main/kotlin/com/demo/tourwave/application/organization/
git rm src/main/kotlin/com/demo/tourwave/application/topology/Organization*.kt
git rm src/main/kotlin/com/demo/tourwave/application/topology/port/Organization*.kt
git commit -m "refactor: move application/topology/Organization* → application/organization"
```

---

### Task 2: application/instructor 패키지 생성

**의존성:** Task 1 완료 후 실행 (OrganizationAccessGuard, OrganizationRepository 패키지가 organization으로 이동했으므로)

**이동할 파일 (6개):**

| 구 경로 | 신 경로 |
|---|---|
| `topology/InstructorModels.kt` | `instructor/InstructorModels.kt` |
| `topology/InstructorProfileService.kt` | `instructor/InstructorProfileService.kt` |
| `topology/InstructorRegistrationService.kt` | `instructor/InstructorRegistrationService.kt` |
| `topology/InstructorValidation.kt` | `instructor/InstructorValidation.kt` |
| `topology/port/InstructorProfileRepository.kt` | `instructor/port/InstructorProfileRepository.kt` |
| `topology/port/InstructorRegistrationRepository.kt` | `instructor/port/InstructorRegistrationRepository.kt` |

---

- [ ] **Step 2-1: port 파일 2개 이동**

`instructor/port/InstructorProfileRepository.kt`:
```
// 변경 전
package com.demo.tourwave.application.topology.port
// 변경 후
package com.demo.tourwave.application.instructor.port
```

`instructor/port/InstructorRegistrationRepository.kt`:
```
// 변경 전
package com.demo.tourwave.application.topology.port
// 변경 후
package com.demo.tourwave.application.instructor.port
```

구 파일 삭제.

- [ ] **Step 2-2: InstructorModels.kt, InstructorValidation.kt 이동**

두 파일 모두:
```
// 변경 전
package com.demo.tourwave.application.topology
// 변경 후
package com.demo.tourwave.application.instructor
```

import 변경 없음. 구 파일 삭제.

- [ ] **Step 2-3: InstructorProfileService.kt 이동**

```
// 변경 전
package com.demo.tourwave.application.topology
import com.demo.tourwave.application.topology.port.InstructorProfileRepository
import com.demo.tourwave.application.topology.port.InstructorRegistrationRepository

// 변경 후
package com.demo.tourwave.application.instructor
import com.demo.tourwave.application.instructor.port.InstructorProfileRepository
import com.demo.tourwave.application.instructor.port.InstructorRegistrationRepository
```

구 파일 삭제.

- [ ] **Step 2-4: InstructorRegistrationService.kt 이동**

이 파일은 cross-boundary import가 있다. 원래 같은 패키지였던 `OrganizationAccessGuard`를 사용하므로 새 import를 **추가**해야 한다.

```
// 변경 전
package com.demo.tourwave.application.topology
import com.demo.tourwave.application.topology.port.InstructorProfileRepository
import com.demo.tourwave.application.topology.port.InstructorRegistrationRepository
import com.demo.tourwave.application.topology.port.OrganizationRepository

// 변경 후
package com.demo.tourwave.application.instructor
import com.demo.tourwave.application.instructor.port.InstructorProfileRepository
import com.demo.tourwave.application.instructor.port.InstructorRegistrationRepository
import com.demo.tourwave.application.organization.OrganizationAccessGuard      // NEW (전에는 동일 패키지)
import com.demo.tourwave.application.organization.port.OrganizationRepository  // 패키지 변경
```

구 파일 삭제.

- [ ] **Step 2-5: 컴파일 + 커밋**

```bash
./gradlew compileKotlin 2>&1 | grep -E "error:" | grep -i "instructor" | head -10
```

에러 없어야 함.

```bash
git add src/main/kotlin/com/demo/tourwave/application/instructor/
git rm src/main/kotlin/com/demo/tourwave/application/topology/Instructor*.kt
git rm src/main/kotlin/com/demo/tourwave/application/topology/port/Instructor*.kt
git commit -m "refactor: move application/topology/Instructor* → application/instructor"
```

---

### Task 3: application/tour 패키지 생성

**의존성:** Task 1 완료 후 실행 (Task 2와 병렬 가능)

**이동할 파일 (5개):**

| 구 경로 | 신 경로 |
|---|---|
| `topology/TourModels.kt` | `tour/TourModels.kt` |
| `topology/TourCommandService.kt` | `tour/TourCommandService.kt` |
| `topology/TourQueryService.kt` | `tour/TourQueryService.kt` |
| `topology/TourValidation.kt` | `tour/TourValidation.kt` |
| `topology/port/TourRepository.kt` | `tour/port/TourRepository.kt` |

---

- [ ] **Step 3-1: port/TourRepository.kt 이동**

```
// 변경 전
package com.demo.tourwave.application.topology.port
// 변경 후
package com.demo.tourwave.application.tour.port
```

구 파일 삭제.

- [ ] **Step 3-2: TourModels.kt, TourValidation.kt 이동**

```
// 변경 전
package com.demo.tourwave.application.topology
// 변경 후
package com.demo.tourwave.application.tour
```

import 변경 없음. 구 파일 삭제.

- [ ] **Step 3-3: TourCommandService.kt 이동**

원래 같은 패키지였던 `OrganizationAccessGuard`를 사용하므로 새 import **추가**.

```
// 변경 전
package com.demo.tourwave.application.topology
import com.demo.tourwave.application.topology.port.OrganizationRepository
import com.demo.tourwave.application.topology.port.TourRepository

// 변경 후
package com.demo.tourwave.application.tour
import com.demo.tourwave.application.organization.OrganizationAccessGuard      // NEW
import com.demo.tourwave.application.organization.port.OrganizationRepository  // 패키지 변경
import com.demo.tourwave.application.tour.port.TourRepository                  // 패키지 변경
```

구 파일 삭제.

- [ ] **Step 3-4: TourQueryService.kt 이동**

```
// 변경 전
package com.demo.tourwave.application.topology
import com.demo.tourwave.application.topology.port.TourRepository

// 변경 후
package com.demo.tourwave.application.tour
import com.demo.tourwave.application.organization.OrganizationAccessGuard  // NEW
import com.demo.tourwave.application.tour.port.TourRepository              // 패키지 변경
```

구 파일 삭제.

- [ ] **Step 3-5: 컴파일 + 커밋**

```bash
./gradlew compileKotlin 2>&1 | grep -E "error:" | grep -i "tour" | head -10
```

```bash
git add src/main/kotlin/com/demo/tourwave/application/tour/
git rm src/main/kotlin/com/demo/tourwave/application/topology/Tour*.kt
git rm src/main/kotlin/com/demo/tourwave/application/topology/port/TourRepository.kt
git commit -m "refactor: move application/topology/Tour* → application/tour"
```

---

### Task 4: application/occurrence 패키지 생성

**의존성:** Tasks 1, 2, 3 모두 완료 후 실행

**이동할 파일 (4개):**

| 구 경로 | 신 경로 |
|---|---|
| `topology/OccurrenceModels.kt` | `occurrence/OccurrenceModels.kt` |
| `topology/OccurrenceCommandService.kt` | `occurrence/OccurrenceCommandService.kt` |
| `topology/OccurrenceValidation.kt` | `occurrence/OccurrenceValidation.kt` |
| `topology/CatalogQueryService.kt` | `occurrence/CatalogQueryService.kt` |

> Note: `OccurrenceRepository`는 이미 `application.booking.port`에 있어 이동 불필요.

---

- [ ] **Step 4-1: OccurrenceModels.kt, OccurrenceValidation.kt 이동**

```
// 변경 전
package com.demo.tourwave.application.topology
// 변경 후
package com.demo.tourwave.application.occurrence
```

import 변경 없음. 구 파일 삭제.

- [ ] **Step 4-2: OccurrenceCommandService.kt 이동**

```
// 변경 전
package com.demo.tourwave.application.topology
import com.demo.tourwave.application.topology.port.InstructorProfileRepository
import com.demo.tourwave.application.topology.port.TourRepository

// 변경 후
package com.demo.tourwave.application.occurrence
import com.demo.tourwave.application.instructor.port.InstructorProfileRepository  // 패키지 변경
import com.demo.tourwave.application.organization.OrganizationAccessGuard          // NEW
import com.demo.tourwave.application.tour.port.TourRepository                      // 패키지 변경
```

구 파일 삭제.

- [ ] **Step 4-3: CatalogQueryService.kt 이동**

```
// 변경 전
package com.demo.tourwave.application.topology
import com.demo.tourwave.application.topology.port.TourRepository

// 변경 후
package com.demo.tourwave.application.occurrence
import com.demo.tourwave.application.tour.port.TourRepository  // 패키지 변경
```

구 파일 삭제.

- [ ] **Step 4-4: 빈 topology 디렉토리 확인**

이 시점에서 `application/topology/` 디렉토리가 완전히 비어있어야 한다.

```bash
find src/main/kotlin/com/demo/tourwave/application/topology -name "*.kt" | sort
```

출력이 없어야 함.

- [ ] **Step 4-5: 컴파일 + 커밋**

```bash
./gradlew compileKotlin 2>&1 | grep -E "error:" | head -20
```

에러 없어야 함.

```bash
git add src/main/kotlin/com/demo/tourwave/application/occurrence/
git rm -r src/main/kotlin/com/demo/tourwave/application/topology/
git commit -m "refactor: move application/topology/Occurrence*+Catalog → application/occurrence; remove empty topology package"
```

---

## Phase 2: adapter.out.persistence/topology 분리

> Tasks 5, 6, 7, 8은 병렬 실행 가능. Phase 1 전체 완료 후 시작.

### Task 5: persistence/organization 분리

**이동할 파일 (8개):**

in-memory (→ `adapter/out/persistence/organization/`):
- `topology/InMemoryOrganizationRepositoryAdapter.kt`
- `topology/InMemoryOrganizationMembershipRepositoryAdapter.kt`

JPA (→ `adapter/out/persistence/jpa/organization/`):
- `jpa/topology/OrganizationJpaEntity.kt`
- `jpa/topology/OrganizationJpaRepository.kt`
- `jpa/topology/OrganizationMembershipJpaEntity.kt`
- `jpa/topology/OrganizationMembershipJpaRepository.kt`
- `jpa/topology/JpaOrganizationRepositoryAdapter.kt`
- `jpa/topology/JpaOrganizationMembershipRepositoryAdapter.kt`

---

- [ ] **Step 5-1: InMemoryOrganizationRepositoryAdapter.kt 이동**

```
// 변경 전
package com.demo.tourwave.adapter.out.persistence.topology
import com.demo.tourwave.application.topology.port.OrganizationRepository

// 변경 후
package com.demo.tourwave.adapter.out.persistence.organization
import com.demo.tourwave.application.organization.port.OrganizationRepository
```

구 파일 삭제.

- [ ] **Step 5-2: InMemoryOrganizationMembershipRepositoryAdapter.kt 이동**

```
// 변경 전
package com.demo.tourwave.adapter.out.persistence.topology
import com.demo.tourwave.application.topology.port.OrganizationMembershipRepository

// 변경 후
package com.demo.tourwave.adapter.out.persistence.organization
import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
```

구 파일 삭제.

- [ ] **Step 5-3: JPA 엔티티/레포지토리 4개 이동**

`OrganizationJpaEntity.kt`, `OrganizationJpaRepository.kt`, `OrganizationMembershipJpaEntity.kt`, `OrganizationMembershipJpaRepository.kt` — 모두:

```
// 변경 전
package com.demo.tourwave.adapter.out.persistence.jpa.topology
// 변경 후
package com.demo.tourwave.adapter.out.persistence.jpa.organization
```

내부 cross-import 없음. 구 파일 삭제.

- [ ] **Step 5-4: JpaOrganizationRepositoryAdapter.kt 이동**

```
// 변경 전
package com.demo.tourwave.adapter.out.persistence.jpa.topology
import com.demo.tourwave.application.topology.port.OrganizationRepository

// 변경 후
package com.demo.tourwave.adapter.out.persistence.jpa.organization
import com.demo.tourwave.application.organization.port.OrganizationRepository
```

또한 `TopologyJsonCodec` 참조를 `JpaJsonCodec`으로 변경 (Task 8 완료 후 또는 Task 8과 함께 진행):
```
// 변경 전
import com.demo.tourwave.adapter.out.persistence.jpa.topology.TopologyJsonCodec  // (internal, 동일 패키지라 import 없음)

// 변경 후
import com.demo.tourwave.adapter.out.persistence.jpa.JpaJsonCodec  // Task 8에서 생성
```

> TopologyJsonCodec은 같은 패키지라 import 없이 사용 중. Task 8에서 JpaJsonCodec으로 이동 후 이 파일에 import 추가 필요.

구 파일 삭제.

- [ ] **Step 5-5: JpaOrganizationMembershipRepositoryAdapter.kt 이동**

```
// 변경 전
package com.demo.tourwave.adapter.out.persistence.jpa.topology
// 변경 후
package com.demo.tourwave.adapter.out.persistence.jpa.organization
```

TopologyJsonCodec 사용 여부 확인 후 동일하게 처리. 구 파일 삭제.

- [ ] **Step 5-6: 커밋**

```bash
git add src/main/kotlin/com/demo/tourwave/adapter/out/persistence/organization/
git add src/main/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/organization/
git rm src/main/kotlin/com/demo/tourwave/adapter/out/persistence/topology/InMemoryOrganization*.kt
git rm src/main/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/topology/Organization*.kt
git rm src/main/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/topology/JpaOrganization*.kt
git commit -m "refactor: move persistence/topology/Organization* → persistence/organization"
```

---

### Task 6: persistence/instructor 분리

**이동할 파일 (6개):**

in-memory (→ `adapter/out/persistence/instructor/`):
- `topology/InMemoryInstructorProfileRepositoryAdapter.kt`
- `topology/InMemoryInstructorRegistrationRepositoryAdapter.kt`

JPA (→ `adapter/out/persistence/jpa/instructor/`):
- `jpa/topology/InstructorProfileJpaEntity.kt`
- `jpa/topology/InstructorProfileJpaRepository.kt`
- `jpa/topology/InstructorRegistrationJpaEntity.kt`
- `jpa/topology/InstructorRegistrationJpaRepository.kt`
- `jpa/topology/JpaInstructorProfileRepositoryAdapter.kt`
- `jpa/topology/JpaInstructorRegistrationRepositoryAdapter.kt`

---

- [ ] **Step 6-1: InMemory 어댑터 2개 이동**

`InMemoryInstructorProfileRepositoryAdapter.kt`:
```
// 변경 전
package com.demo.tourwave.adapter.out.persistence.topology
import com.demo.tourwave.application.topology.port.InstructorProfileRepository

// 변경 후
package com.demo.tourwave.adapter.out.persistence.instructor
import com.demo.tourwave.application.instructor.port.InstructorProfileRepository
```

`InMemoryInstructorRegistrationRepositoryAdapter.kt`:
```
// 변경 전
package com.demo.tourwave.adapter.out.persistence.topology
import com.demo.tourwave.application.topology.port.InstructorRegistrationRepository

// 변경 후
package com.demo.tourwave.adapter.out.persistence.instructor
import com.demo.tourwave.application.instructor.port.InstructorRegistrationRepository
```

구 파일 삭제.

- [ ] **Step 6-2: JPA 엔티티/레포지토리 4개 이동**

`InstructorProfileJpaEntity.kt`, `InstructorProfileJpaRepository.kt`, `InstructorRegistrationJpaEntity.kt`, `InstructorRegistrationJpaRepository.kt`:

```
// 변경 전
package com.demo.tourwave.adapter.out.persistence.jpa.topology
// 변경 후
package com.demo.tourwave.adapter.out.persistence.jpa.instructor
```

구 파일 삭제.

- [ ] **Step 6-3: JpaInstructorProfileRepositoryAdapter.kt 이동**

```
// 변경 전
package com.demo.tourwave.adapter.out.persistence.jpa.topology
import com.demo.tourwave.application.topology.port.InstructorProfileRepository

// 변경 후
package com.demo.tourwave.adapter.out.persistence.jpa.instructor
import com.demo.tourwave.application.instructor.port.InstructorProfileRepository
import com.demo.tourwave.adapter.out.persistence.jpa.JpaJsonCodec  // Task 8 완료 후 추가
```

구 파일 삭제.

- [ ] **Step 6-4: JpaInstructorRegistrationRepositoryAdapter.kt 이동**

```
// 변경 전
package com.demo.tourwave.adapter.out.persistence.jpa.topology
import com.demo.tourwave.application.topology.port.InstructorRegistrationRepository

// 변경 후
package com.demo.tourwave.adapter.out.persistence.jpa.instructor
import com.demo.tourwave.application.instructor.port.InstructorRegistrationRepository
import com.demo.tourwave.adapter.out.persistence.jpa.JpaJsonCodec  // Task 8 완료 후 추가
```

구 파일 삭제.

- [ ] **Step 6-5: 커밋**

```bash
git add src/main/kotlin/com/demo/tourwave/adapter/out/persistence/instructor/
git add src/main/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/instructor/
git rm src/main/kotlin/com/demo/tourwave/adapter/out/persistence/topology/InMemoryInstructor*.kt
git rm src/main/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/topology/Instructor*.kt
git rm src/main/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/topology/JpaInstructor*.kt
git commit -m "refactor: move persistence/topology/Instructor* → persistence/instructor"
```

---

### Task 7: persistence/tour 분리

**이동할 파일 (4개):**

in-memory (→ `adapter/out/persistence/tour/`):
- `topology/InMemoryTourRepositoryAdapter.kt`

JPA (→ `adapter/out/persistence/jpa/tour/`):
- `jpa/topology/TourJpaEntity.kt`
- `jpa/topology/TourJpaRepository.kt`
- `jpa/topology/JpaTourRepositoryAdapter.kt`

---

- [ ] **Step 7-1: InMemoryTourRepositoryAdapter.kt 이동**

```
// 변경 전
package com.demo.tourwave.adapter.out.persistence.topology
import com.demo.tourwave.application.topology.port.TourRepository

// 변경 후
package com.demo.tourwave.adapter.out.persistence.tour
import com.demo.tourwave.application.tour.port.TourRepository
```

구 파일 삭제.

- [ ] **Step 7-2: TourJpaEntity.kt, TourJpaRepository.kt 이동**

```
// 변경 전
package com.demo.tourwave.adapter.out.persistence.jpa.topology
// 변경 후
package com.demo.tourwave.adapter.out.persistence.jpa.tour
```

구 파일 삭제.

- [ ] **Step 7-3: JpaTourRepositoryAdapter.kt 이동**

```
// 변경 전
package com.demo.tourwave.adapter.out.persistence.jpa.topology
import com.demo.tourwave.application.topology.port.TourRepository

// 변경 후
package com.demo.tourwave.adapter.out.persistence.jpa.tour
import com.demo.tourwave.application.tour.port.TourRepository
import com.demo.tourwave.adapter.out.persistence.jpa.JpaJsonCodec  // Task 8 완료 후 추가
```

구 파일 삭제.

- [ ] **Step 7-4: 커밋**

```bash
git add src/main/kotlin/com/demo/tourwave/adapter/out/persistence/tour/
git add src/main/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/tour/
git rm src/main/kotlin/com/demo/tourwave/adapter/out/persistence/topology/InMemoryTour*.kt
git rm src/main/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/topology/Tour*.kt
git rm src/main/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/topology/JpaTour*.kt
git commit -m "refactor: move persistence/topology/Tour* → persistence/tour"
```

---

### Task 8: TopologyJsonCodec → JpaJsonCodec (Tasks 5-7과 병렬 가능)

`TopologyJsonCodec.kt`는 Organization, Instructor, Tour 3개의 JPA 어댑터가 공유한다. 패키지 분리 후 서로 다른 패키지가 되므로 공유 위치인 `adapter.out.persistence.jpa`로 이동한다.

---

- [ ] **Step 8-1: JpaJsonCodec.kt 생성**

`src/main/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/JpaJsonCodec.kt` 생성:

```kotlin
package com.demo.tourwave.adapter.out.persistence.jpa

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

internal object JpaJsonCodec {
    private val objectMapper = jacksonObjectMapper()

    fun writeList(values: List<String>): String = objectMapper.writeValueAsString(values)

    fun writeLongList(values: List<Long>): String = objectMapper.writeValueAsString(values)

    fun readList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return objectMapper.readValue(raw, objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java))
    }

    fun readLongList(raw: String?): List<Long> {
        if (raw.isNullOrBlank()) return emptyList()
        return objectMapper.readValue(raw, objectMapper.typeFactory.constructCollectionType(List::class.java, java.lang.Long::class.java))
    }
}
```

- [ ] **Step 8-2: TopologyJsonCodec.kt 삭제**

```bash
git rm src/main/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/topology/TopologyJsonCodec.kt
```

- [ ] **Step 8-3: 커밋**

```bash
git add src/main/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/JpaJsonCodec.kt
git commit -m "refactor: replace TopologyJsonCodec with shared JpaJsonCodec"
```

> Tasks 5-7에서 각 JPA 어댑터에 `import com.demo.tourwave.adapter.out.persistence.jpa.JpaJsonCodec`를 추가하고 `TopologyJsonCodec` → `JpaJsonCodec`으로 변경한다.

---

### Phase 2 컴파일 체크

- [ ] **Step P2-compile: 전체 persistence 컴파일 검증**

Tasks 5-8 완료 후:

```bash
./gradlew compileKotlin 2>&1 | grep -E "error:" | head -20
```

에러 없어야 함.

---

## Phase 3: bootstrap/TopologyConfig.kt 분리

**의존성:** Phase 1 완료 후

### Task 9: TopologyConfig.kt → 4개 Config로 분리

---

- [ ] **Step 9-1: OrganizationConfig.kt 생성**

`src/main/kotlin/com/demo/tourwave/bootstrap/OrganizationConfig.kt` 생성:

```kotlin
package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.organization.OrganizationCommandService
import com.demo.tourwave.application.organization.OrganizationInvitationDeliveryService
import com.demo.tourwave.application.organization.OrganizationMembershipService
import com.demo.tourwave.application.organization.OrganizationQueryService
import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.auth.UserActionTokenService
import com.demo.tourwave.application.customer.NotificationDeliveryService
import com.demo.tourwave.application.customer.NotificationTemplateFactory
import com.demo.tourwave.application.user.port.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.Duration

@Configuration
class OrganizationConfig {
    @Bean
    fun organizationAccessGuard(
        organizationRepository: OrganizationRepository,
        membershipRepository: OrganizationMembershipRepository
    ): OrganizationAccessGuard = OrganizationAccessGuard(
        organizationRepository = organizationRepository,
        membershipRepository = membershipRepository
    )

    @Bean
    fun organizationCommandService(
        organizationRepository: OrganizationRepository,
        membershipRepository: OrganizationMembershipRepository,
        userRepository: UserRepository,
        organizationAccessGuard: OrganizationAccessGuard,
        clock: Clock
    ): OrganizationCommandService = OrganizationCommandService(
        organizationRepository = organizationRepository,
        membershipRepository = membershipRepository,
        userRepository = userRepository,
        organizationAccessGuard = organizationAccessGuard,
        clock = clock
    )

    @Bean
    fun organizationMembershipService(
        membershipRepository: OrganizationMembershipRepository,
        userRepository: UserRepository,
        organizationAccessGuard: OrganizationAccessGuard,
        organizationInvitationDeliveryService: OrganizationInvitationDeliveryService,
        clock: Clock
    ): OrganizationMembershipService = OrganizationMembershipService(
        membershipRepository = membershipRepository,
        userRepository = userRepository,
        organizationAccessGuard = organizationAccessGuard,
        organizationInvitationDeliveryService = organizationInvitationDeliveryService,
        clock = clock
    )

    @Bean
    fun organizationInvitationDeliveryService(
        userRepository: UserRepository,
        organizationRepository: OrganizationRepository,
        userActionTokenService: UserActionTokenService,
        notificationDeliveryService: NotificationDeliveryService,
        notificationTemplateFactory: NotificationTemplateFactory,
        @Value("\${tourwave.app.base-url:http://localhost:3000}") appBaseUrl: String,
        @Value("\${tourwave.organization.invitation-token-ttl-seconds:604800}") invitationTokenTtlSeconds: Long,
        clock: Clock
    ): OrganizationInvitationDeliveryService = OrganizationInvitationDeliveryService(
        userRepository = userRepository,
        organizationRepository = organizationRepository,
        userActionTokenService = userActionTokenService,
        notificationDeliveryService = notificationDeliveryService,
        notificationTemplateFactory = notificationTemplateFactory,
        appBaseUrl = appBaseUrl,
        invitationTokenTtl = Duration.ofSeconds(invitationTokenTtlSeconds),
        clock = clock
    )

    @Bean
    fun organizationQueryService(
        organizationRepository: OrganizationRepository,
        membershipRepository: OrganizationMembershipRepository,
        organizationAccessGuard: OrganizationAccessGuard
    ): OrganizationQueryService = OrganizationQueryService(
        organizationRepository = organizationRepository,
        membershipRepository = membershipRepository,
        organizationAccessGuard = organizationAccessGuard
    )
}
```

- [ ] **Step 9-2: InstructorConfig.kt 생성**

`src/main/kotlin/com/demo/tourwave/bootstrap/InstructorConfig.kt` 생성:

```kotlin
package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.instructor.InstructorProfileService
import com.demo.tourwave.application.instructor.InstructorRegistrationService
import com.demo.tourwave.application.instructor.port.InstructorProfileRepository
import com.demo.tourwave.application.instructor.port.InstructorRegistrationRepository
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.user.port.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class InstructorConfig {
    @Bean
    fun instructorRegistrationService(
        registrationRepository: InstructorRegistrationRepository,
        instructorProfileRepository: InstructorProfileRepository,
        organizationRepository: OrganizationRepository,
        organizationAccessGuard: OrganizationAccessGuard,
        userRepository: UserRepository,
        clock: Clock
    ): InstructorRegistrationService = InstructorRegistrationService(
        registrationRepository = registrationRepository,
        instructorProfileRepository = instructorProfileRepository,
        organizationRepository = organizationRepository,
        organizationAccessGuard = organizationAccessGuard,
        userRepository = userRepository,
        clock = clock
    )

    @Bean
    fun instructorProfileService(
        instructorProfileRepository: InstructorProfileRepository,
        instructorRegistrationRepository: InstructorRegistrationRepository,
        userRepository: UserRepository,
        clock: Clock
    ): InstructorProfileService = InstructorProfileService(
        instructorProfileRepository = instructorProfileRepository,
        instructorRegistrationRepository = instructorRegistrationRepository,
        userRepository = userRepository,
        clock = clock
    )
}
```

- [ ] **Step 9-3: TourConfig.kt 생성**

`src/main/kotlin/com/demo/tourwave/bootstrap/TourConfig.kt` 생성:

```kotlin
package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.tour.TourCommandService
import com.demo.tourwave.application.tour.TourQueryService
import com.demo.tourwave.application.tour.port.TourRepository
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.organization.port.OrganizationRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class TourConfig {
    @Bean
    fun tourCommandService(
        tourRepository: TourRepository,
        organizationRepository: OrganizationRepository,
        organizationAccessGuard: OrganizationAccessGuard,
        clock: Clock
    ): TourCommandService = TourCommandService(
        tourRepository = tourRepository,
        organizationRepository = organizationRepository,
        organizationAccessGuard = organizationAccessGuard,
        clock = clock
    )

    @Bean
    fun tourQueryService(
        tourRepository: TourRepository,
        organizationAccessGuard: OrganizationAccessGuard
    ): TourQueryService = TourQueryService(
        tourRepository = tourRepository,
        organizationAccessGuard = organizationAccessGuard
    )
}
```

- [ ] **Step 9-4: OccurrenceConfig.kt 생성**

`src/main/kotlin/com/demo/tourwave/bootstrap/OccurrenceConfig.kt` 생성:

```kotlin
package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.occurrence.CatalogQueryService
import com.demo.tourwave.application.occurrence.OccurrenceCommandService
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.TimeWindowPolicyService
import com.demo.tourwave.application.instructor.port.InstructorProfileRepository
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.review.port.ReviewRepository
import com.demo.tourwave.application.tour.port.TourRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class OccurrenceConfig {
    @Bean
    fun occurrenceCommandService(
        occurrenceRepository: OccurrenceRepository,
        bookingRepository: BookingRepository,
        tourRepository: TourRepository,
        instructorProfileRepository: InstructorProfileRepository,
        organizationAccessGuard: OrganizationAccessGuard,
        clock: Clock
    ): OccurrenceCommandService = OccurrenceCommandService(
        occurrenceRepository = occurrenceRepository,
        bookingRepository = bookingRepository,
        tourRepository = tourRepository,
        instructorProfileRepository = instructorProfileRepository,
        organizationAccessGuard = organizationAccessGuard,
        clock = clock
    )

    @Bean
    fun catalogQueryService(
        tourRepository: TourRepository,
        occurrenceRepository: OccurrenceRepository,
        bookingRepository: BookingRepository,
        reviewRepository: ReviewRepository,
        timeWindowPolicyService: TimeWindowPolicyService
    ): CatalogQueryService = CatalogQueryService(
        tourRepository = tourRepository,
        occurrenceRepository = occurrenceRepository,
        bookingRepository = bookingRepository,
        reviewRepository = reviewRepository,
        timeWindowPolicyService = timeWindowPolicyService
    )
}
```

- [ ] **Step 9-5: TopologyConfig.kt 삭제**

```bash
git rm src/main/kotlin/com/demo/tourwave/bootstrap/TopologyConfig.kt
```

- [ ] **Step 9-6: 컴파일 + 커밋**

```bash
./gradlew compileKotlin 2>&1 | grep -E "error:" | head -20
```

```bash
git add src/main/kotlin/com/demo/tourwave/bootstrap/OrganizationConfig.kt
git add src/main/kotlin/com/demo/tourwave/bootstrap/InstructorConfig.kt
git add src/main/kotlin/com/demo/tourwave/bootstrap/TourConfig.kt
git add src/main/kotlin/com/demo/tourwave/bootstrap/OccurrenceConfig.kt
git commit -m "refactor: split TopologyConfig → OrganizationConfig + InstructorConfig + TourConfig + OccurrenceConfig"
```

---

## Phase 4: 외부 파일 import 업데이트

> Tasks 10, 11 병렬 실행 가능. Phase 1 + Task 9 완료 후.

### Task 10: web adapter import 업데이트 (9개 파일)

---

- [ ] **Step 10-1: OrganizationOperatorController.kt**

`src/main/kotlin/com/demo/tourwave/adapter/in/web/organization/OrganizationOperatorController.kt` 에서:

```
// 변경 전 (lines 4-11)
import com.demo.tourwave.application.topology.ChangeOrganizationMemberRoleCommand
import com.demo.tourwave.application.topology.CreateOrganizationCommand
import com.demo.tourwave.application.topology.DeactivateOrganizationMemberCommand
import com.demo.tourwave.application.topology.InviteOrganizationMemberCommand
import com.demo.tourwave.application.topology.OrganizationCommandService
import com.demo.tourwave.application.topology.OrganizationMembershipService
import com.demo.tourwave.application.topology.OrganizationQueryService
import com.demo.tourwave.application.topology.UpdateOrganizationProfileCommand

// 변경 후
import com.demo.tourwave.application.organization.ChangeOrganizationMemberRoleCommand
import com.demo.tourwave.application.organization.CreateOrganizationCommand
import com.demo.tourwave.application.organization.DeactivateOrganizationMemberCommand
import com.demo.tourwave.application.organization.InviteOrganizationMemberCommand
import com.demo.tourwave.application.organization.OrganizationCommandService
import com.demo.tourwave.application.organization.OrganizationMembershipService
import com.demo.tourwave.application.organization.OrganizationQueryService
import com.demo.tourwave.application.organization.UpdateOrganizationProfileCommand
```

- [ ] **Step 10-2: OrganizationPublicController.kt**

`adapter/in/web/organization/OrganizationPublicController.kt`:
`application.topology.` → `application.organization.` 일괄 치환.

- [ ] **Step 10-3: TourOperatorController.kt**

`adapter/in/web/tour/TourOperatorController.kt`:
```
// 변경 전
import com.demo.tourwave.application.topology.CreateTourCommand
import com.demo.tourwave.application.topology.PublishTourCommand
import com.demo.tourwave.application.topology.TourCommandService
import com.demo.tourwave.application.topology.TourQueryService
import com.demo.tourwave.application.topology.UpdateTourCommand
import com.demo.tourwave.application.topology.UpdateTourContentCommand

// 변경 후
import com.demo.tourwave.application.tour.CreateTourCommand
import com.demo.tourwave.application.tour.PublishTourCommand
import com.demo.tourwave.application.tour.TourCommandService
import com.demo.tourwave.application.tour.TourQueryService
import com.demo.tourwave.application.tour.UpdateTourCommand
import com.demo.tourwave.application.tour.UpdateTourContentCommand
```

- [ ] **Step 10-4: TourPublicController.kt**

`adapter/in/web/tour/TourPublicController.kt`:
`application.topology.` → `application.tour.` 일괄 치환.

- [ ] **Step 10-5: OccurrenceOperatorController.kt**

`adapter/in/web/occurrence/OccurrenceOperatorController.kt`:
```
// 변경 전
import com.demo.tourwave.application.topology.CreateOccurrenceCommand
import com.demo.tourwave.application.topology.OccurrenceCommandService
import com.demo.tourwave.application.topology.RescheduleOccurrenceCommand
import com.demo.tourwave.application.topology.UpdateOccurrenceCommand

// 변경 후
import com.demo.tourwave.application.occurrence.CreateOccurrenceCommand
import com.demo.tourwave.application.occurrence.OccurrenceCommandService
import com.demo.tourwave.application.occurrence.RescheduleOccurrenceCommand
import com.demo.tourwave.application.occurrence.UpdateOccurrenceCommand
```

- [ ] **Step 10-6: OccurrencePublicController.kt + OccurrenceWebDtos.kt**

두 파일 모두: `application.topology.` → `application.occurrence.` 일괄 치환.

- [ ] **Step 10-7: InstructorProfileController.kt**

```
// 변경 전
import com.demo.tourwave.application.topology.InstructorProfileService
import com.demo.tourwave.application.topology.UpsertInstructorProfileCommand

// 변경 후
import com.demo.tourwave.application.instructor.InstructorProfileService
import com.demo.tourwave.application.instructor.UpsertInstructorProfileCommand
```

- [ ] **Step 10-8: InstructorRegistrationController.kt**

`application.topology.` → `application.instructor.` 일괄 치환.

- [ ] **Step 10-9: 컴파일 + 커밋**

```bash
./gradlew compileKotlin 2>&1 | grep -E "error:" | grep "adapter.in.web" | head -10
```

```bash
git add src/main/kotlin/com/demo/tourwave/adapter/in/web/organization/
git add src/main/kotlin/com/demo/tourwave/adapter/in/web/tour/
git add src/main/kotlin/com/demo/tourwave/adapter/in/web/occurrence/
git add src/main/kotlin/com/demo/tourwave/adapter/in/web/instructor/
git commit -m "refactor: update web adapter imports topology → domain-aligned packages"
```

---

### Task 11: 나머지 bootstrap import 업데이트 (4개 파일)

**의존성:** Phase 1 완료 + Task 9 완료

---

- [ ] **Step 11-1: AuthConfig.kt**

`src/main/kotlin/com/demo/tourwave/bootstrap/AuthConfig.kt` 에서:
```
// 변경 전
import com.demo.tourwave.application.topology.OrganizationQueryService
// 변경 후
import com.demo.tourwave.application.organization.OrganizationQueryService
```

- [ ] **Step 11-2: UseCaseConfig.kt**

`src/main/kotlin/com/demo/tourwave/bootstrap/UseCaseConfig.kt` 에서:
```
// 변경 전
import com.demo.tourwave.application.topology.OrganizationAccessGuard
import com.demo.tourwave.application.topology.port.InstructorProfileRepository
import com.demo.tourwave.application.topology.port.TourRepository

// 변경 후
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.instructor.port.InstructorProfileRepository
import com.demo.tourwave.application.tour.port.TourRepository
```

- [ ] **Step 11-3: CommunicationReportingConfig.kt**

`src/main/kotlin/com/demo/tourwave/bootstrap/CommunicationReportingConfig.kt` 에서:
```
// 변경 전
import com.demo.tourwave.application.topology.OrganizationAccessGuard
import com.demo.tourwave.application.topology.port.TourRepository

// 변경 후
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.tour.port.TourRepository
```

- [ ] **Step 11-4: CustomerConfig.kt**

`src/main/kotlin/com/demo/tourwave/bootstrap/CustomerConfig.kt` 에서:
```
// 변경 전
import com.demo.tourwave.application.topology.OrganizationAccessGuard
import com.demo.tourwave.application.topology.port.TourRepository

// 변경 후
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.tour.port.TourRepository
```

- [ ] **Step 11-5: 전체 컴파일 검증 + 커밋**

```bash
./gradlew compileKotlin 2>&1 | grep -E "error:" | head -20
```

에러 없어야 함.

```bash
git add src/main/kotlin/com/demo/tourwave/bootstrap/
git commit -m "refactor: update bootstrap imports topology → domain-aligned packages"
```

---

## Phase 5: communication → announcement (독립, 언제든 실행 가능)

### Task 12: application/communication → application/announcement

**의존성:** 없음 (독립)

---

- [ ] **Step 12-1: AnnouncementRepository.kt 이동**

`src/main/kotlin/com/demo/tourwave/application/announcement/port/AnnouncementRepository.kt` 생성:

```
// 변경 전
package com.demo.tourwave.application.communication.port
// 변경 후
package com.demo.tourwave.application.announcement.port
```

구 파일 삭제.

- [ ] **Step 12-2: AnnouncementService.kt 이동**

`src/main/kotlin/com/demo/tourwave/application/announcement/AnnouncementService.kt` 생성:

```
// 변경 전
package com.demo.tourwave.application.communication
import com.demo.tourwave.application.communication.port.AnnouncementRepository

// 변경 후
package com.demo.tourwave.application.announcement
import com.demo.tourwave.application.announcement.port.AnnouncementRepository
```

구 파일 삭제.

- [ ] **Step 12-3: 외부 파일 import 업데이트 (5개 파일)**

다음 파일들에서 `application.communication` → `application.announcement`로 치환:

1. `bootstrap/CommunicationReportingConfig.kt`:
   - `import com.demo.tourwave.application.communication.AnnouncementService` → `application.announcement.AnnouncementService`
   - `import com.demo.tourwave.application.communication.port.AnnouncementRepository` → `application.announcement.port.AnnouncementRepository`

2. `adapter/out/persistence/jpa/announcement/JpaAnnouncementRepositoryAdapter.kt`:
   - `import com.demo.tourwave.application.communication.port.AnnouncementRepository` → `application.announcement.port.AnnouncementRepository`

3. `adapter/out/persistence/announcement/InMemoryAnnouncementRepositoryAdapter.kt`:
   - 동일하게 치환

4. `adapter/in/web/announcement/AnnouncementController.kt`:
   - `application.communication.AnnouncementService` → `application.announcement.AnnouncementService`
   - `application.communication.CreateAnnouncementCommand` → `application.announcement.CreateAnnouncementCommand`
   - `application.communication.UpdateAnnouncementCommand` → `application.announcement.UpdateAnnouncementCommand`

- [ ] **Step 12-4: 빈 communication 디렉토리 확인 + 컴파일 + 커밋**

```bash
find src/main/kotlin/com/demo/tourwave/application/communication -name "*.kt" | sort
# 출력 없어야 함
```

```bash
./gradlew compileKotlin 2>&1 | grep -E "error:" | head -10
```

```bash
git add src/main/kotlin/com/demo/tourwave/application/announcement/
git rm -r src/main/kotlin/com/demo/tourwave/application/communication/
git add src/main/kotlin/com/demo/tourwave/adapter/
git add src/main/kotlin/com/demo/tourwave/bootstrap/CommunicationReportingConfig.kt
git commit -m "refactor: rename application/communication → application/announcement"
```

---

## Phase 6: 테스트 파일 이동 및 최종 검증

### Task 13: 테스트 파일 이동

**의존성:** Phase 1-5 모두 완료

---

- [ ] **Step 13-1: application/topology 테스트 파일 이동 (5개)**

| 구 경로 (test/.../application/topology/) | 신 경로 |
|---|---|
| `OrganizationCommandServiceTest.kt` | `application/organization/OrganizationCommandServiceTest.kt` |
| `OrganizationMembershipServiceTest.kt` | `application/organization/OrganizationMembershipServiceTest.kt` |
| `InstructorRegistrationServiceTest.kt` | `application/instructor/InstructorRegistrationServiceTest.kt` |
| `TourCommandServiceTest.kt` | `application/tour/TourCommandServiceTest.kt` |
| `OccurrenceCommandServiceTest.kt` | `application/occurrence/OccurrenceCommandServiceTest.kt` |
| `CatalogQueryServiceTest.kt` | `application/occurrence/CatalogQueryServiceTest.kt` |

각 파일에서 `package com.demo.tourwave.application.topology` → 해당 bounded context 패키지로 변경.
import 중 `application.topology.*` 참조도 동일하게 업데이트.
구 파일 삭제.

- [ ] **Step 13-2: adapter/in/web/topology 테스트 파일 이동 (2개)**

`InstructorAndTourControllerIntegrationTest.kt` → `adapter/in/web/instructor/InstructorAndTourControllerIntegrationTest.kt`
`OccurrenceCatalogControllerIntegrationTest.kt` → `adapter/in/web/occurrence/OccurrenceCatalogControllerIntegrationTest.kt`

각 파일 package 선언 + topology import 업데이트. 구 파일 삭제.

- [ ] **Step 13-3: FakeRepositories.kt 업데이트**

`src/test/kotlin/com/demo/tourwave/support/FakeRepositories.kt` 에서:

```
// 변경 전
import com.demo.tourwave.application.topology.port.OrganizationMembershipRepository
import com.demo.tourwave.application.topology.port.OrganizationRepository
import com.demo.tourwave.application.topology.port.TourRepository

// 변경 후
import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.tour.port.TourRepository
```

- [ ] **Step 13-4: 나머지 테스트 파일 import 업데이트**

다음 파일들에서 `application.topology.*` / `persistence.topology.*` / `persistence.jpa.topology.*` 참조를 일괄 치환:

```bash
# topology import 남아있는 테스트 파일 확인
grep -rl "application\.topology\.\|persistence\.topology\.\|persistence\.jpa\.topology\." \
  src/test/kotlin/com/demo/tourwave/ --include="*.kt"
```

출력된 각 파일에서 패키지명 치환 적용:
- `application.topology.` → `application.organization.` / `application.instructor.` / `application.tour.` / `application.occurrence.` (클래스명 기준으로 판단)
- `persistence.topology.` → `persistence.organization.` / `persistence.instructor.` / `persistence.tour.`
- `persistence.jpa.topology.` → `persistence.jpa.organization.` / `persistence.jpa.instructor.` / `persistence.jpa.tour.`

- [ ] **Step 13-5: communication 테스트 import 업데이트**

`test/application/communication/AnnouncementServiceTest.kt`:
- package: `application.communication` → `application.announcement`
- import: `application.communication.*` → `application.announcement.*`
- 파일 이동: `test/application/announcement/AnnouncementServiceTest.kt`

`test/adapter/in/web/communication/CommunicationReportingIntegrationTest.kt`:
- `application.communication.port.AnnouncementRepository` → `application.announcement.port.AnnouncementRepository`

---

### Task 14: 최종 빌드 검증

- [ ] **Step 14-1: topology 패키지 완전 제거 확인**

```bash
find src -name "*.kt" | xargs grep -l "\.topology\." 2>/dev/null
```

출력이 없어야 함 (topology 패키지 참조 0건).

- [ ] **Step 14-2: communication 패키지 완전 제거 확인**

```bash
find src -name "*.kt" | xargs grep -l "application\.communication\." 2>/dev/null
```

출력이 없어야 함.

- [ ] **Step 14-3: 전체 빌드 + 테스트**

```bash
./gradlew compileKotlin compileTestKotlin 2>&1 | grep -E "error:" | head -20
```

컴파일 에러 0건 확인.

```bash
./gradlew test --tests "com.demo.tourwave.application.*" 2>&1 | tail -20
```

기존 통과하던 application 레이어 테스트가 모두 통과해야 함.

```bash
./gradlew test 2>&1 | grep -E "FAILED|ERROR" | head -20
```

topology 패키지 변경으로 인한 **신규 실패 없어야** 함. (기존 CLAUDE.md에 명시된 `CommunicationReportingIntegrationTest`, `OccurrenceCatalogControllerIntegrationTest`는 `main` 브랜치에서 원래 실패 중 — 이 작업 이전과 이후 동일하면 OK)

- [ ] **Step 14-4: 최종 커밋**

```bash
git add src/test/
git commit -m "refactor: update all test imports to match renamed packages"
```

---

## 요약: 파일 변경 수

| 범주 | 파일 수 |
|---|---|
| application/topology → 4개 패키지 (삭제) | 19개 |
| application/topology → 4개 패키지 (생성) | 19개 |
| persistence/topology (in-memory + JPA, 삭제) | 16개 |
| persistence/topology (in-memory + JPA, 생성) | 16개 |
| TopologyJsonCodec → JpaJsonCodec | 1개 삭제 + 1개 생성 |
| bootstrap/TopologyConfig.kt → 4개 Config | 1개 삭제 + 4개 생성 |
| 외부 import 수정 (web, bootstrap, FakeRepositories) | 15개 수정 |
| communication → announcement (삭제) | 2개 |
| communication → announcement (생성) | 2개 |
| 테스트 파일 이동 + import 수정 | ~18개 |
| **총계** | **~95개 파일 작업** |
