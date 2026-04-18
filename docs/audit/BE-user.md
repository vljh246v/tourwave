# BE 감사: user

마지막 감사: 2026-04-18

## 요약
- 구현 완성도: 🟡
- 테스트 완성도: 🟡
- OpenAPI path 수: 4개 (태그: Me)
- SSOT 참조: openapi.yaml 태그 Me

## Domain 레이어
### 엔티티
- [x] `User` — 사용자 (이메일, 비밀번호, 상태, 이메일 검증 시각) `domain/user/User.kt`

### 상태 머신
- [x] `UserStatus` — 상태: `ACTIVE`, `DEACTIVATED`, `SUSPENDED`, `DELETED`

### 값 객체
- 없음

### 도메인 서비스
- 없음

### 도메인 이벤트
- 없음 (감사 이벤트는 auth, organization 에서 발행)

## Application 레이어
### 서비스
- [x] `MeService`:
  - `getCurrentUser(userId): User` — 인증된 사용자 조회 (401 if not found)
  - `getCurrentUserMemberships(userId): List<OrganizationMembership>` — 사용자의 조직 멤버십 목록
  - `updateCurrentUser(userId, displayName): User` — 프로필 업데이트 (displayName)

- [x] `UserCommandService`:
  - `registerUser(name, email): User` — 사용자 등록 (임시 비밀번호로 초기화) [**사용 미확인**]

### Port 인터페이스
- [x] `UserRepository` — findById, findByEmail, save

## Adapter.in.web
### 컨트롤러
- [x] `MeController`:
  - `GET /me` → MeResponse (user + memberships)
  - `PATCH /me` — displayName 변경
  - `POST /me/deactivate` — 계정 비활성화

## Adapter.out.persistence
### JPA 엔티티
- [x] `UserJpaEntity` — 사용자 저장소
- [x] `UserJpaRepository` (Spring Data)

### 어댑터 구현
- [x] `JpaUserRepositoryAdapter` — UserRepository 구현

## Tests
### 단위
- [x] `UserCommandServiceTest` — registerUser (FakeRepositories 기반)

### 통합
- [x] `UserQueryAdapterTest` — findById, findByEmail (Spring Context + Testcontainers MySQL)

### 실패 중
- 없음

## 관찰된 문제

1. **ApplicationService 부재** — user 도메인에 전용 ApplicationService 없음. 프로필 관리는 auth 도메인의 MeService에서만 처리. 조직/멤버십은 organization 도메인에 의존.

2. **UserCommandService 미활용** — registerUser() 메서드가 존재하나 애플리케이션의 주요 흐름(signup은 AuthCommandService 담당)에서 사용되지 않음.

3. **임시 비밀번호 부재** — registerUser()에서 "temporary-password" 하드코딩. 실제 임시 비밀번호 생성/이메일 발송 로직 없음.

4. **SUSPENDED, DELETED 상태 미구현** — User enum에 정의했으나 비활성화는 DEACTIVATED만 사용. SUSPENDED 용도 불명, DELETED는 소프트 딜리트 미지원.

5. **사용자 삭제 계획 부재** — deactivate()는 상태만 변경. 개인정보 처리 방침상 영구 삭제/익명화 처리 로직 없음.

6. **MeService와 auth 강결합** — MeService.updateCurrentUser()가 auth 도메인의 validation 함수(requireValidDisplayName) 직접 호출. 계층 간 의존성 역전 위험.
