# BE 감사: organization

마지막 감사: 2026-04-18

## 요약
- 구현 완성도: ✅
- 테스트 완성도: ✅
- OpenAPI path 수: 6개 (태그: Organizations)
- SSOT 참조: openapi.yaml 태그 Organizations

## Domain 레이어
### 엔티티
- `Organization` — slug(unique), name, description, publicDescription, contactEmail, contactPhone, websiteUrl, businessName, businessRegistrationNumber, timezone(IANA), attachmentAssetIds(List), status(ACTIVE/INACTIVE), createdAt/updatedAt(UTC)
- `OrganizationMembership` — organizationId+userId, role(MEMBER/ADMIN/OWNER), status(INVITED/ACTIVE/INACTIVE), createdAt/updatedAt(UTC)

### 상태 머신
- **Organization.status**: ACTIVE (초기, 비활성화 로직 없음, 비-터미널)
- **OrganizationMembership.status**: INVITED → ACTIVE → INACTIVE (재활용 가능)

### 값 객체
- `OrganizationRole` enum: MEMBER, ADMIN, OWNER (canManageMembers() 메서드)

### 도메인 서비스
- Organization: updateProfile(), updateAttachments(), Factory create()
- OrganizationMembership: activate(), deactivate(), changeRole(), Factory invite()/active()

### 도메인 이벤트
- 명시적 도메인 이벤트 없음 (감사는 application 레이어에서 기록)

## Application 레이어
### 서비스
- `OrganizationCommandService` — createOrganization(), updateOrganizationProfile()
- `OrganizationQueryService` — getPublicOrganization(), getOperatorOrganization(), getMembershipsForUser()
- `OrganizationMembershipService` — inviteMembers(), acceptInvitation(), changeRole(), deactivateMembers(), listMemberships()
- `OrganizationInvitationDeliveryService` — sendInvitation(), consumeInvitationToken()
- `OrganizationAccessGuard` — requireMembership(), requireOperator(), requireOwner()

### Port 인터페이스
- `OrganizationRepository` — save(), findById(), findBySlug()
- `OrganizationMembershipRepository` — save(), findByUserId(), findByOrganizationIdAndUserId(), findByOrganizationId()

## Adapter.in.web
### 컨트롤러
- `OrganizationOperatorController` (9개 엔드포인트)
  - POST /operator/organizations (201) — createOrganization + OWNER 멤버십
  - GET /operator/organizations/{organizationId} (200) — getOperatorOrganization
  - PATCH /operator/organizations/{organizationId} (200) — updateOrganizationProfile
  - GET /operator/organizations/{organizationId}/members (200) — listMemberships
  - POST /operator/organizations/{organizationId}/members/invite (201) — inviteMembers
  - PATCH /operator/organizations/{organizationId}/members/{membershipId}/role (200) — changeRole
  - DELETE /operator/organizations/{organizationId}/members/{membershipId} (204) — deactivateMembers
  - GET /public/organizations/{organizationSlug} (200) — getPublicOrganization
  - GET /me/memberships (200) — getMembershipsForUser

## Adapter.out.persistence
### JPA 엔티티
- `OrganizationJpaEntity` — id, slug(UNIQUE), name, description, publicDescription, contactEmail, contactPhone, websiteUrl, businessName, businessRegistrationNumber, timezone, attachmentAssetIds(JSON), status, createdAtUtc, updatedAtUtc
- `OrganizationMembershipJpaEntity` — id, organizationId, userId, role, status, createdAtUtc, updatedAtUtc

### 어댑터 구현
- `JpaOrganizationRepositoryAdapter`
- `JpaOrganizationMembershipRepositoryAdapter`

## Tests
### 단위
- `OrganizationCommandServiceTest` ✅ — createOrganization, updateOrganizationProfile
- `OrganizationMembershipServiceTest` ✅ — inviteMembers, changeRole, deactivateMembers

### 통합
- `OrganizationControllerIntegrationTest` ✅ — Testcontainers(MySQL), 전체 CRUD + 멤버십 흐름

### 실패 중
- 없음 (모두 통과)

## 관찰된 문제
1. ~~**감사 이벤트 미기록**: 조직 생성/수정/멤버 초대 시 AuditEvent 기록 없음~~ → T-904 (2026-04-26) 해결: ORGANIZATION_CREATED/PROFILE_UPDATED + ORGANIZATION_MEMBER_INVITED/ROLE_CHANGED/DEACTIVATED 발행, OrganizationCommandAuditTest + OrganizationMembershipAuditTest 커버
2. **Idempotency-Key 미사용**: 멤버 초대 중복 요청 시 멱등성 미보장
3. **slug 정규화 미완전**: 소문자만 처리, 특수문자/공백 검증 부족
4. **타임존 검증 없음**: IANA tz db 검증 미실행

## 스키마 검증
✅ 모든 timestamp UTC (_at_utc)
✅ 외래키 제약
⚠ attachmentAssetIds (JSON) — 정규화 필요 가능성
