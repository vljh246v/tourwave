---
id: T-904
title: "T-904 — [BE] 감사 이벤트 커버리지 강화 (announcement/organization/instructor/tour 모든 write)"
aliases: [T-904]

repo: tourwave
area: be
milestone: cross
domain: infra
layer: test + implementation
size: L
status: in-progress

depends_on: ['T-901']
blocks: []
sub_tasks: []

github_issue: 8
exec_plan: ""

created: 2026-04-18
updated: 2026-04-26
---

#status/in-progress #area/be

# T-904 — [BE] 감사 이벤트 커버리지 강화 (announcement/organization/instructor/tour 모든 write)

## 파일 소유권
WRITE:
  - `src/main/kotlin/com/demo/tourwave/application/announcement/AnnouncementService.kt` (수정)
  - `src/main/kotlin/com/demo/tourwave/application/organization/OrganizationCommandService.kt` (수정)
  - `src/main/kotlin/com/demo/tourwave/application/organization/OrganizationMembershipService.kt` (수정)
  - `src/main/kotlin/com/demo/tourwave/application/instructor/InstructorProfileService.kt` (수정)
  - `src/main/kotlin/com/demo/tourwave/application/instructor/InstructorRegistrationService.kt` (수정)
  - `src/main/kotlin/com/demo/tourwave/application/tour/TourCommandService.kt` (수정, T-907 병합)
  - `src/test/kotlin/com/demo/tourwave/application/tour/TourCommandServiceTest.kt` (수정, T-907 병합)
  - 해당 서비스 단위 테스트 (모든 write 케이스에 감사 이벤트 검증 추가)

READ:
  - `docs/domain-rules.md` "감사 이벤트는 예약 상태 변경, offer 생명주기, occurrence cancel/finish, 결제 액션마다 반드시 기록"
  - 기존 booking/occurrence 서비스의 감사 이벤트 패턴 (참고용)
  - 감사: `BE-announcement.md`, `BE-organization.md`, `BE-instructor.md`, `BE-tour.md`

DO NOT TOUCH:
  - domain 레이어 (감사 로직은 application에만)
  - booking/occurrence 서비스 (이미 구현)

## SSOT 근거
- `docs/domain-rules.md` — "감사 이벤트(append-only)는 예약 상태 변경, offer 생명주기, occurrence cancel/finish, 결제 액션마다 반드시 기록"
- 감사 관찰들:
  - `BE-announcement.md` "감사 이벤트 미기록: create/update/delete 시 감사 로그 기록 안 됨"
  - `BE-organization.md` "감사 이벤트 미기록: 조직 생성/수정/멤버 초대 시 AuditEvent 기록 없음"
  - `BE-instructor.md` "감사 이벤트 미기록: 등록 제출/승인/거부 시 AuditEvent 기록 없음"

## 현재 상태 (갭)
- [ ] AnnouncementService.create/update/delete 에서 AuditEventPort.append() 호출 없음
- [ ] OrganizationCommandService.createOrganization/updateOrganizationProfile 미기록
- [ ] OrganizationMembershipService.inviteMembers/changeRole/deactivateMembers 미기록
- [ ] InstructorProfileService.createMyProfile/updateMyProfile 미기록
- [ ] InstructorRegistrationService.submitRegistration/approveRegistration/rejectRegistration 미기록
- [ ] TourCommandService.createTour/updateTour/publishTour 미기록 (존재 확인 필요)
- [ ] 단위 테스트에 "감사 이벤트 호출 검증" 케이스 부재

## 구현 지침
1. **각 서비스 write 메서드에 감사 기록 추가**:
   - `@Transactional` 끝에서 `auditEventPort.append(AuditEventCommand(...))`
   - actorUserId, action (ANNOUNCEMENT_CREATED, ORGANIZATION_PROFILE_UPDATED 등), aggregateType, aggregateId, details
2. **action enum 정의** (도메인에서 미리 정의 확인):
   - ANNOUNCEMENT_CREATED, ANNOUNCEMENT_UPDATED, ANNOUNCEMENT_DELETED
   - ORGANIZATION_CREATED, ORGANIZATION_PROFILE_UPDATED
   - ORGANIZATION_MEMBER_INVITED, ORGANIZATION_MEMBER_ROLE_CHANGED, ORGANIZATION_MEMBER_DEACTIVATED
   - INSTRUCTOR_PROFILE_CREATED, INSTRUCTOR_PROFILE_UPDATED
   - INSTRUCTOR_REGISTRATION_SUBMITTED, INSTRUCTOR_REGISTRATION_APPROVED, INSTRUCTOR_REGISTRATION_REJECTED
   - TOUR_CREATED, TOUR_UPDATED, TOUR_PUBLISHED, TOUR_ARCHIVED (필요한 것들)
3. **단위 테스트 패치**:
   - FakeAuditEventPort 주입 (또는 mock)
   - each write test에 `verify(auditEventPort).append(...)` 추가
   - action, actorUserId, aggregateId 검증
4. **통합 테스트**:
   - 각 컨트롤러 통합 테스트에 "감사 이벤트 로그 저장됨" 검증 추가

## Acceptance Criteria
- [ ] 위 6개 서비스의 모든 write 메서드에 AuditEventPort.append() 호출 추가
- [ ] 각 서비스 단위 테스트에 감사 이벤트 검증 케이스 포함 (최소 각 action 당 1개)
- [ ] `./gradlew test --tests "*ServiceTest"` 통과 (announcement/organization/instructor/tour)
- [ ] 통합 테스트에서 감사 레코드 실제 저장 확인

## Verification
`./scripts/verify-task.sh T-904`
- 감사 관련 단위 테스트 실행
- 감사 관련 통합 테스트 실행

## Rollback
`git checkout -- src/main/kotlin/com/demo/tourwave/application/`

## Notes
- booking/occurrence 서비스에서 감사 구현 패턴 참고 필수.
- **T-907 병합됨**: TourCommandService 감사는 이 태스크에서 함께 구현 (createTour/updateTour/publishTour/archiveTour).
- action enum은 `domain/common/AuditEvent.kt`에서 중앙 관리. new action 추가 시 enum 확장 필요.
- tour 관련 action: TOUR_CREATED, TOUR_UPDATED, TOUR_PUBLISHED, TOUR_ARCHIVED (T-907 병합으로 포함)
