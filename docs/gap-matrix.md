# Gap Matrix — Phase 2

마지막 갱신: 2026-04-18

이 문서는 Phase 1 감사 결과 + SSOT 문서 대비 현재 구현의 갭을 태스크 ID로 매핑한 것이다.

- ✅ = 완전 구현 (추가 작업 없음)
- 🟡 = 부분 구현 (일부 갭 존재)
- ❌ = 미구현
- 각 🟡/❌ 셀에는 해당 갭을 채우는 태스크 ID 링크
- N/A = 해당 없음

## 범례: 태스크 ID 범위

- **M1 인증·탐색** (T-001~099): auth, user(Me), tour(public read), organization(public read), search, policies
- **M2 예약·결제** (T-100~199): booking, occurrence, calendar, waitlist, payment, participant
- **M3 운영·CS** (T-200~299): operations, inquiry, announcement, review, instructor, organization(operator), asset
- **M4 리포팅·정책** (T-300~399): reports, finance, favorites UX, notifications UX, calendar export
- **크로스 컷팅** (T-900~999): detekt/ktlint, AuditEventTest, Idempotency 일관성, 실패 테스트 고치기, 감사 이벤트 커버리지, FE 크로스컷팅

---

## 표 1: BE 갭 (도메인 × 레이어)

| 도메인 | Domain | Application | Adapter.in | Adapter.out | Tests | Docs |
|---|---|---|---|---|---|---|
| announcement | ✅ | 🟡 [T-201](T-201) | ✅ [T-202](T-202) Idempotency — T-903 | ✅ | ❌ [T-901](T-901) | 🟡 [T-203](T-203) |
| asset | 🟡 [T-204](T-204) Content-Type | ✅ | ✅ | ✅ | 🟡 [T-205](T-205) | ✅ |
| auth | ✅ | ✅ | ✅ | 🟡 [T-207](T-207) Email adapter | 🟡 [T-903](T-903) | ✅ |
| booking | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| common | ✅ | ✅ | N/A | ✅ | 🟡 [T-904](T-904) | 🟡 [T-003](T-003) TimeWindow 상수 |
| customer | ✅ | 🟡 [T-206](T-206) Notification filtering | ✅ | 🟡 [T-207](T-207) Email adapter | 🟡 [T-208](T-208) iCal RFC | 🟡 [T-209](T-209) |
| inquiry | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ [T-210](T-210) OpenAPI 계약 |
| instructor | 🟡 [T-211](T-211) Status 검증 | 🟡 [T-212](T-212) Audit events | ✅ [T-213](T-213) Idempotency — T-903 | 🟡 [T-214](T-214) Role 검증 | 🟡 [T-905](T-905) | 🟡 [T-215](T-215) |
| occurrence | ✅ | 🟡 [T-100](T-100) Race condition | 🟡 [T-101](T-101) Idempotency reschedule | 🟡 [T-102](T-102) Status guard | ✅ | 🟡 [T-103](T-103) Timezone 중앙화 |
| operations | ✅ | 🟡 [T-216](T-216) Audit events | ✅ [T-217](T-217) Idempotency — T-903 | ✅ | ✅ | 🟡 [T-218](T-218) |
| organization | ✅ | 🟡 [T-219](T-219) Audit events | ✅ [T-220](T-220) Idempotency — T-903 | ✅ | ✅ | 🟡 [T-221](T-221) slug normalize |
| participant | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| payment | 🟡 [T-104](T-104) Provider port | 🟡 [T-105](T-105) Capture flow | 🟡 [T-106](T-106) Webhook provider | ✅ | ✅ | 🟡 [T-107](T-107) |
| review | ✅ | ✅ | ✅ | ✅ | ✅ | 🟡 [T-222](T-222) Audit events |
| tour | ✅ | 🟡 [T-223](T-223) Audit events | 🟡 [T-224](T-224) Idempotency | ✅ | 🟡 [T-225](T-225) | 🟡 [T-226](T-226) |
| user | 🟡 [T-004](T-004) ApplicationService | 🟡 [T-005](T-005) Status enum | ✅ | ✅ | 🟡 [T-006](T-006) | 🟡 [T-007](T-007) Soft delete |

---

## 표 2: FE 갭 (도메인 × UI 계층)

| OpenAPI 태그 | API 클라이언트 | 리스트 | 상세 | 폼 | 액션 | 테스트 |
|---|---|---|---|---|---|---|
| Announcements | ❌ [T-910](T-910) | ❌ [T-226](T-226) | ❌ [T-227](T-227) | ❌ [T-228](T-228) | ❌ [T-229](T-229) | ❌ [T-914](T-914) |
| Assets | ❌ [T-910](T-910) | ❌ [T-230](T-230) | N/A | ❌ [T-231](T-231) | ❌ [T-232](T-232) | ❌ [T-914](T-914) |
| Auth | ❌ [T-010](T-010) | N/A | N/A | ❌ [T-011](T-011) | ❌ [T-012](T-012) | ❌ [T-914](T-914) |
| Bookings | ❌ [T-910](T-910) | ❌ [T-108](T-108) | ❌ [T-109](T-109) | ❌ [T-110](T-110) | ❌ [T-111](T-111) | ❌ [T-914](T-914) |
| Calendar | ❌ [T-910](T-910) | N/A | ❌ [T-233](T-233) .ics export | N/A | N/A | ❌ [T-914](T-914) |
| Favorites | ❌ [T-910](T-910) | ❌ [T-234](T-234) | N/A | N/A | ❌ [T-235](T-235) | ❌ [T-914](T-914) |
| Finance | ❌ [T-910](T-910) | ❌ [T-300](T-300) | N/A | N/A | ❌ [T-301](T-301) | ❌ [T-914](T-914) |
| Inquiries | ❌ [T-910](T-910) | ❌ [T-112](T-112) | ❌ [T-113](T-113) | ❌ [T-114](T-114) | ❌ [T-115](T-115) | ❌ [T-914](T-914) |
| Instructors | ❌ [T-910](T-910) | ❌ [T-236](T-236) | ❌ [T-237](T-237) | ❌ [T-238](T-238) | ❌ [T-239](T-239) | ❌ [T-914](T-914) |
| Me | ❌ [T-010](T-010) | N/A | ❌ [T-013](T-013) | ❌ [T-014](T-014) | ❌ [T-015](T-015) | ❌ [T-914](T-914) |
| Notes | ❌ [T-910](T-910) | N/A | N/A | ❌ [T-240](T-240) | ❌ [T-240](T-240) | ❌ [T-914](T-914) |
| Notifications | ❌ [T-910](T-910) | ❌ [T-241](T-241) | N/A | N/A | ❌ [T-242](T-242) | ❌ [T-914](T-914) |
| Occurrences | ❌ [T-910](T-910) | ❌ [T-116](T-116) | ❌ [T-117](T-117) | ❌ [T-118](T-118) | ❌ [T-119](T-119) | ❌ [T-914](T-914) |
| Operations | ❌ [T-910](T-910) | ❌ [T-302](T-302) | N/A | N/A | ❌ [T-303](T-303) | ❌ [T-914](T-914) |
| Organizations | ❌ [T-910](T-910) | ❌ [T-016](T-016) | ❌ [T-017](T-017) | ❌ [T-018](T-018) | ❌ [T-019](T-019) | ❌ [T-914](T-914) |
| Participants | ❌ [T-910](T-910) | ❌ [T-120](T-120) | N/A | N/A | ❌ [T-121](T-121) | ❌ [T-914](T-914) |
| Payments | ❌ [T-910](T-910) | ❌ [T-122](T-122) | N/A | ❌ [T-123](T-123) | ❌ [T-124](T-124) | ❌ [T-914](T-914) |
| Policies | ❌ [T-020](T-020) | N/A | ❌ [T-021](T-021) | N/A | N/A | ❌ [T-914](T-914) |
| Reports | ❌ [T-910](T-910) | ❌ [T-304](T-304) | N/A | N/A | ❌ [T-305](T-305) CSV | ❌ [T-914](T-914) |
| Reviews | ❌ [T-910](T-910) | ❌ [T-125](T-125) | ❌ [T-126](T-126) | ❌ [T-127](T-127) | ❌ [T-128](T-128) | ❌ [T-914](T-914) |
| Search | ❌ [T-010](T-010) | ❌ [T-022](T-022) | N/A | N/A | N/A | ❌ [T-914](T-914) |
| Tours | ❌ [T-010](T-010) | ❌ [T-023](T-023) | ❌ [T-024](T-024) | ❌ [T-025](T-025) | ❌ [T-026](T-026) | ❌ [T-914](T-914) |
| Waitlist | ❌ [T-910](T-910) | ❌ [T-129](T-129) | N/A | N/A | ❌ [T-130](T-130) | ❌ [T-914](T-914) |

---

## 표 3: 크로스 컷팅

| 항목 | 상태 | 태스크 |
|---|---|---|
| detekt/ktlint 설정 (코드 스타일 게이트) | ❌ | [T-900](T-900) |
| AuditEventTest 부재 | ❌ | [T-901](T-901) |
| CommunicationReportingIntegrationTest 실패 (main) | ❌ | [T-902](T-902) |
| Idempotency-Key 일관성 (announcement/operations/organization/instructor) | ✅ | [T-903](T-903) — done 2026-04-28 |
| 감사 이벤트 커버리지 (announcement/organization/instructor/tour) | ✅ | [T-904](T-904) — done 2026-04-26 |
| OccurrenceCatalogControllerIntegrationTest 실패 (main) | ❌ | [T-905](T-905) |
| InstructorAndTourControllerIntegrationTest 분리 필요 | 🟡 | [T-906](T-906) |
| FE API 클라이언트 fetch 래퍼 | ❌ | [T-910](T-910) |
| FE OpenAPI 타입 생성 (schema.ts) | ❌ | [T-911](T-911) |
| FE 인증 모듈 (httpOnly cookie) | ❌ | [T-912](T-912) |
| FE 상태 관리 라이브러리 선택·도입 (Zustand/Redux) | ❌ | [T-913](T-913) |
| FE 테스트 러너 (vitest) 도입 | ❌ | [T-914](T-914) |
| FE 라우트 가드 (middleware.ts) | ❌ | [T-915](T-915) |
| FE 디자인 시스템 기본 컴포넌트 (Button/Input/Modal) | ❌ | [T-916](T-916) |
| orchestrator Phase 0/7.6/8.5 신설 (문서·정합성 가드) | 🟡 | [T-907](T-907) — in-progress 2026-04-27 |

---

## 부록: 마일스톤별 태스크 수 예상

| 마일스톤 | BE 태스크 | FE 태스크 | 합계 |
|---|---|---|---|
| M1 (인증·탐색) | 7 | 17 | 24 |
| M2 (예약·결제) | 8 | 26 | 34 |
| M3 (운영·CS) | 27 | 54 | 81 |
| M4 (리포팅·정책) | 6 | 12 | 18 |
| 크로스 컷팅 | - | - | 17 |
| **합계** | **48** | **109** | **157** |

---

## 태스크 ID 인덱스 (전체 목록)

### M1 (인증·탐색) — T-001~027

#### Auth & User
- T-001 [BE] auth — Password reset 이메일 발송 구현 (NotificationChannelPort)
- T-002 [BE] auth — Refresh token 로테이션 race condition 해결 (version field)
- T-003 [BE] common — TimeWindowPolicyService 상수 설정화
- T-004 [BE] user — User ApplicationService 도입 (계층 격리)
- T-005 [BE] user — UserStatus enum 완성 (SUSPENDED, DELETED 구현)
- T-006 [BE] user — User 프로필 테스트 강화
- T-007 [BE] user — User soft-delete 정책 문서화

#### Tours & Organization Public
- T-008 [FE] Auth — 로그인 폼 컴포넌트
- T-009 [FE] Auth — 회원가입 폼 컴포넌트
- T-010 [FE] API Client — API 클라이언트 + 에러 처리
- T-011 [FE] Auth — 이메일 검증 플로우 UI
- T-012 [FE] Auth — 비밀번호 리셋 플로우 UI
- T-013 [FE] Me — 사용자 프로필 페이지
- T-014 [FE] Me — 프로필 수정 폼
- T-015 [FE] Me — 계정 비활성화 확인 모달
- T-016 [FE] Organizations — 조직 목록 (공개)
- T-017 [FE] Organizations — 조직 상세 페이지
- T-018 [FE] Organizations — 조직 생성 폼 (운영자)
- T-019 [FE] Organizations — 멤버 관리 UI (운영자)
- T-020 [FE] Policies — 정책 페이지 (읽기 전용)
- T-021 [FE] Policies — 정책 상세 페이지
- T-022 [FE] Search — 전역 검색 UI
- T-023 [FE] Tours — 투어 목록 (공개)
- T-024 [FE] Tours — 투어 상세 페이지
- T-025 [FE] Tours — 투어 생성/수정 폼 (운영자)
- T-026 [FE] Tours — 투어 발행 액션 (운영자)
- T-027 [FE] Search — 검색 결과 페이지

### M2 (예약·결제) — T-100~130

#### Booking & Occurrence
- T-100 [BE] occurrence — Race condition 해결 (행 락 먼저 획득)
- T-101 [BE] occurrence — Reschedule Idempotency-Key 구현
- T-102 [BE] occurrence — 터미널 상태 추가 전이 금지
- T-103 [BE] occurrence — TimeWindow 타임존 중앙화 (util)
- T-104 [BE] payment — PaymentProviderPort 구현 (Stripe/Tosspayments)
- T-105 [BE] payment — Capture 프로세스 구현 (AUTHORIZED → CAPTURED)
- T-106 [BE] payment — 제공자별 webhook 서명 검증 테스트
- T-107 [BE] payment — 결제 문서화 (provider integration)
- T-108 [FE] Bookings — 예약 생성 폼
- T-109 [FE] Bookings — 예약 상세 페이지
- T-110 [FE] Bookings — 예약 상태 전이 UI (approve/reject/cancel)
- T-111 [FE] Bookings — 오퍼 수락/거절 UI
- T-112 [FE] Inquiries — 문의 생성 폼
- T-113 [FE] Inquiries — 문의 상세/메시지 스레드
- T-114 [FE] Inquiries — 메시지 작성 폼
- T-115 [FE] Inquiries — 문의 종료 액션
- T-116 [FE] Occurrences — 회차 목록 (공개 + 운영자)
- T-117 [FE] Occurrences — 회차 상세 (예약 가능 여부 표시)
- T-118 [FE] Occurrences — 회차 생성 폼 (운영자)
- T-119 [FE] Occurrences — 회차 수정/취소/완료 액션 (운영자)
- T-120 [FE] Participants — 참여자 목록/초대 UI
- T-121 [FE] Participants — 초대 수락/거절 UI
- T-122 [FE] Payments — 결제 내역 조회
- T-123 [FE] Payments — 환불 폼
- T-124 [FE] Payments — 결제 상태 표시
- T-125 [FE] Reviews — 리뷰 생성 폼
- T-126 [FE] Reviews — 리뷰 목록/평가 요약
- T-127 [FE] Reviews — 투어 리뷰 보기
- T-128 [FE] Reviews — 강사 리뷰 보기
- T-129 [FE] Waitlist — 대기열 목록 조회
- T-130 [FE] Waitlist — 대기열 승격 UI (운영자)

### M3 (운영·CS) — T-201~305

#### Announcement & Asset
- T-201 [BE] announcement — Idempotency-Key 부분 구현 검토 및 보강
- T-202 [BE] announcement — POST/PATCH/DELETE Idempotency-Key 헤더 필수
- T-203 [BE] announcement — 공지사항 문서화 (상태 머신 정의)
- T-204 [BE] asset — Content-Type 검증 (화이트리스트)
- T-205 [BE] asset — Asset 통합 테스트 (presigned URL flow)
- T-226 [FE] Announcements — 공지사항 목록 페이지
- T-227 [FE] Announcements — 공지사항 상세 페이지
- T-228 [FE] Announcements — 공지사항 생성/수정 폼 (운영자)
- T-229 [FE] Announcements — 공지사항 발행/비공개 액션

#### Instructor & Organization
- T-206 [BE] customer — Notification 필터링 (감사 이벤트 남발 방지)
- T-207 [BE] customer — NotificationChannelPort 이메일 어댑터 (SendGrid/SES)
- T-208 [BE] customer — iCal RFC 5545 준수 검증
- T-209 [BE] customer — Customer 도메인 문서화
- T-210 [BE] inquiry — OpenAPI 계약 테스트 (InquiryControllerIntegrationTest)
- T-211 [BE] instructor — Instructor profile status 검증 강화
- ~~T-212 [BE] instructor — Instructor 감사 이벤트 기록 (registration action)~~ → T-904로 흡수 완료 (2026-04-26)
- T-213 [BE] instructor — Instructor Idempotency-Key 구현
- T-214 [BE] instructor — Instructor registration 거부 사유 필수 검증
- T-215 [BE] instructor — Instructor 문서화
- ~~T-219 [BE] organization — Organization 감사 이벤트 기록 (create/update/invite)~~ → T-904로 흡수 완료 (2026-04-26)
- T-220 [BE] organization — Organization Idempotency-Key 구현 (invite)
- T-221 [BE] organization — Organization slug 정규화 강화 (특수문자/공백)
- ~~T-223 [BE] tour — Tour 감사 이벤트 기록 (create/update/publish)~~ → T-904로 흡수 완료 (2026-04-26)
- T-224 [BE] tour — Tour 생성 Idempotency-Key 구현
- T-225 [BE] tour — Tour 통합 테스트 분리 (InstructorAndTourControllerIntegrationTest 분리)
- T-226 [BE] tour — Tour 문서화 (발행 이후 수정 정책)
- T-230 [FE] Assets — 자산 업로드 UI
- T-231 [FE] Assets — 자산 첨부/배열 변경 UI
- T-232 [FE] Assets — 자산 삭제 액션
- T-236 [FE] Instructors — 강사 프로필 페이지
- T-237 [FE] Instructors — 강사 등록 폼
- T-238 [FE] Instructors — 강사 등록 승인/거부 UI (운영자)
- T-239 [FE] Instructors — 강사 프로필 수정 폼

#### Operations & Common
- T-216 [BE] operations — Operations 감사 이벤트 기록 (remediation action)
- T-217 [BE] operations — Operations POST Idempotency-Key 구현
- T-218 [BE] operations — Operations 문서화 (remediation flow)
- T-222 [BE] review — Review 감사 이벤트 기록 여부 확인 및 구현
- T-240 [FE] Notes — 운영 노트 추가 UI (occurrence)
- T-241 [FE] Notifications — 알림 목록 페이지
- T-242 [FE] Notifications — 알림 읽음 표시 액션
- T-234 [FE] Favorites — 즐겨찾기 목록 페이지
- T-235 [FE] Favorites — 투어 즐겨찾기 토글 (추가/제거)
- T-233 [FE] Calendar — 투어/예약 iCal 다운로드

### M4 (리포팅·정책) — T-300~305

#### Reports & Finance
- T-300 [FE] Finance — 일일 대사 현황 보기
- T-301 [FE] Finance — 결제 대사 갱신 액션
- T-302 [FE] Operations — 실패 처리 큐 조회
- T-303 [FE] Operations — 재시도/해결 액션
- T-304 [FE] Reports — 예약/회차 리포트 페이지
- T-305 [FE] Reports — CSV 내보내기 액션

### 크로스 컷팅 — T-900~916

#### BE Infrastructure & Tests
- T-900 [BE] common — detekt/ktlint 설정 (git pre-commit hook)
- T-901 [BE] common — AuditEventTest 생성 (audit event coverage)
- T-902 [BE] common — CommunicationReportingIntegrationTest 고치기 (main branch fail)
- T-903 [BE] common — Idempotency-Key 일관성 검토 (announcement/operations/organization/instructor)
- T-904 [BE] common — 감사 이벤트 커버리지 강화 (모든 write 엔드포인트)
- T-905 [BE] common — OccurrenceCatalogControllerIntegrationTest 고치기 (main branch fail)
- T-906 [BE] common — InstructorAndTourControllerIntegrationTest 분리 (테스트 독립성)

#### FE Infrastructure & Cross-Cutting
- T-910 [FE] common — API 클라이언트 fetch 래퍼 (Bearer token, 에러 처리, 재시도)
- T-911 [FE] common — OpenAPI 타입 생성 (openapi-generator or similar)
- T-912 [FE] common — 인증 모듈 (JWT storage abstraction, token refresh, httpOnly cookie)
- T-913 [FE] common — 상태 관리 라이브러리 선택 및 도입 (Zustand 권장)
- T-914 [FE] common — 테스트 러너 도입 (vitest + MSW)
- T-915 [FE] common — 라우트 가드 middleware (역할 기반 redirect)
- T-916 [FE] common — 디자인 시스템 기본 컴포넌트 (Button/Input/Modal/Card)

---

## 주요 갭 요약

### 🔴 고위험 (즉시 해결 필수)
1. **BE: Asset Content-Type 검증 없음** (T-204) — 악성 파일 업로드 가능
2. **BE: Refresh token 로테이션 race condition** (T-002) — 분산 환경 취약
3. **BE: Occurrence 용량 동시성 제어** (T-100) — 오버부킹 위험

### 🟡 중위험 (M1/M2 착수 전 해결)
1. **BE: Idempotency-Key 부분 미구현** (T-903) — announcement, operations, organization, instructor
2. **BE: 감사 이벤트 미기록** (T-904) — announcement, organization, instructor, tour, review
3. **BE: PaymentProviderPort 미구현** (T-104) — 실제 결제 연동 불가
4. **FE: 크로스컷팅 프리쿼지트** (T-910~916) — 모든 FE 기능의 선행 조건

### 🟢 저위험 (M3/M4에서 처리)
1. **BE: Test 분리 및 정리** (T-906, T-905, T-902)
2. **BE: 설정화 및 정규화** (T-003, T-221)
3. **FE: UI 컴포넌트 및 페이지** (T-226~305)

---

## 구현 순서 권장

1. **Week 1-2: 크로스컷팅 (T-900~916)** — FE 기반 구축, BE 정책 게이트
2. **Week 3-4: M1 (T-001~027)** — 인증·로그인 플로우
3. **Week 5-6: M2 (T-100~130)** — 예약·결제 핵심 흐름
4. **Week 7-10: M3 (T-201~305)** — 운영자 대시보드·CS
5. **Week 11-12: M4 (T-300~305)** — 리포팅·분석

---

*이 매트릭스는 Phase 3에서 상세 카드(card template)로 전개된다.*
