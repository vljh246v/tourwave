# BE 감사: instructor

마지막 감사: 2026-04-18

## 요약
- 구현 완성도: ✅
- 테스트 완성도: 🟡
- OpenAPI path 수: 7개 (태그: Instructors)
- SSOT 참조: openapi.yaml 태그 Instructors

## Domain 레이어
### 엔티티
- `InstructorProfile` — userId+organizationId, headline, bio, languages(List), specialties(List), certifications(List), yearsOfExperience, internalNote, status(ACTIVE/INACTIVE), approvedAt(UTC), createdAt/updatedAt(UTC)
- `InstructorRegistration` — organizationId+userId, headline, bio, languages(List), specialties(List), status(PENDING/APPROVED/REJECTED), rejectionReason, reviewedByUserId, reviewedAt(UTC), createdAt/updatedAt(UTC)

### 상태 머신
- **InstructorProfile.status**: ACTIVE ↔ INACTIVE (가역)
- **InstructorRegistration.status**: PENDING → APPROVED | REJECTED → (PENDING 재제출 가능)

### 값 객체
- `InstructorProfileStatus` enum: ACTIVE, INACTIVE
- `InstructorRegistrationStatus` enum: PENDING, APPROVED, REJECTED

### 도메인 서비스
- InstructorProfile: `update()`, Factory 메서드 `create()`
- InstructorRegistration: `resubmit()`, `approve()`, `reject()`, Factory 메서드 `create()`

### 도메인 이벤트
- 명시적 도메인 이벤트 없음 (감사는 application 레이어에서 기록)

## Application 레이어
### 서비스
- `InstructorProfileService` — getMyProfile(), createMyProfile(), updateMyProfile(), getPublicProfile()
- `InstructorRegistrationService` — submitRegistration(), approveRegistration(), rejectRegistration()

### Port 인터페이스
- `InstructorProfileRepository` — save(), findById(), findByUserIdAndOrganizationId()
- `InstructorRegistrationRepository` — save(), findById(), findPendingByOrganizationId(), findByUserIdAndOrganizationId()

## Adapter.in.web
### 컨트롤러
- `InstructorProfileController` (4개 엔드포인트)
  - GET /me/instructor-profile (200) — getMyProfile (본인 only)
  - POST /me/instructor-profile (201) — createMyProfile
  - PATCH /me/instructor-profile (200) — updateMyProfile
  - GET /instructors/{instructorProfileId} (200) — getPublicProfile

- `InstructorRegistrationController` (4개 엔드포인트)
  - POST /instructor-registrations (201) — submitRegistration
  - GET /instructor-registrations/{registrationId} (200) — getRegistration
  - POST /instructor-registrations/{registrationId}/approve (200) — approveRegistration (OWNER only)
  - POST /instructor-registrations/{registrationId}/reject (200) — rejectRegistration (OWNER only)

## Adapter.out.persistence
### JPA 엔티티
- `InstructorProfileJpaEntity` — id, userId, organizationId, headline, bio, languages(JSON), specialties(JSON), certifications(JSON), yearsOfExperience, internalNote, status, approvedAtUtc, createdAtUtc, updatedAtUtc
- `InstructorRegistrationJpaEntity` — id, organizationId, userId, headline, bio, languages(JSON), specialties(JSON), status, rejectionReason, reviewedByUserId, reviewedAtUtc, createdAtUtc, updatedAtUtc

### 어댑터 구현
- `JpaInstructorProfileRepositoryAdapter`
- `JpaInstructorRegistrationRepositoryAdapter`

## Tests
### 단위
- `InstructorRegistrationServiceTest` ✅ — submitRegistration, approveRegistration, rejectRegistration, resubmit flow

### 통합
- `InstructorAndTourControllerIntegrationTest` 🟡 — instructor + tour endpoint (결합 테스트, 분리 필요)

### 실패 중
- 없음 (모두 통과)

## 관찰된 문제
1. ~~**감사 이벤트 미기록**: 등록 제출/승인/거부 시 AuditEvent 기록 없음~~ → T-904 (2026-04-26) 해결: INSTRUCTOR_REGISTRATION_SUBMITTED/APPROVED/REJECTED + INSTRUCTOR_PROFILE_CREATED/UPDATED 발행 (approve 시 신규 프로필 생성 분기는 PROFILE_CREATED 동시 발행), InstructorAuditTest 커버
2. **Idempotency-Key 미사용**: 중복 제출 검증 미보장
3. **승인자 역할 검증 부족**: approveRegistration() 호출 시 OWNER 체크 있나 확인 필요
4. **통합 테스트 분리 필요**: InstructorAndTourControllerIntegrationTest → InstructorControllerIntegrationTest + TourControllerIntegrationTest 분리 권장
5. **거부 사유 필수 검증 없음**: rejectionReason이 null인 거부 허용 가능

## 스키마 검증
✅ 모든 timestamp UTC
✅ UNIQUE 제약: (organizationId, userId) 조합
⚠ languages, specialties, certifications 데이터 타입 (JSON) — 쿼리 성능 영향 검토 필요
