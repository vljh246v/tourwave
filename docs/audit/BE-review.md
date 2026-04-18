# BE 감사: review

마지막 감사: 2026-04-18

## 요약
- 구현 완성도: ✅
- 테스트 완성도: ✅
- OpenAPI path 수: 7개 (태그: Reviews)
- SSOT 참조: openapi.yaml 태그 Reviews

## Domain 레이어
### 엔티티
- `Review` — occurrenceId, reviewerUserId, type(ReviewType), rating(1-5 범위), comment(nullable), createdAt(UTC)
  - init block 검증: rating ∈ [1, 5]

### 상태 머신
- **Review**: 상태 머신 없음 (불변, append-only)

### 값 객체
- `ReviewType` enum: TOUR, INSTRUCTOR

### 도메인 서비스
- Review: Factory 메서드 없음 (직접 생성자 호출)
- Domain 검증: rating 범위만 (init block에서)

### 도메인 이벤트
- 명시적 도메인 이벤트 없음

## Application 레이어
### 서비스
- `ReviewCommandService` — createTourReview(), createInstructorReview()
  - Idempotency-Key 기반 중복 요청 방지 ✅
  - 아이덤포턴시 충돌 시 409/422 상태코드 반환 ✅
  
- `ReviewQueryService` — getOccurrenceSummary(), getTourSummary(), getInstructorSummary()
  - 평균 rating, 리뷰 수, 유형별 분포 조회

### Port 인터페이스
- `ReviewRepository` — save(), findByOccurrenceId(), findByReviewerUserIdAndOccurrenceId(), findByTourId(), findByInstructorProfileId()
- `IdempotencyRepository` — findByKeyAndUserId(), save() (아이덤포턴시 저장소)

## Adapter.in.web
### 컨트롤러
- `ReviewController` (7개 엔드포인트)
  - POST /occurrences/{occurrenceId}/reviews/tour (200/201) — createTourReview (Idempotency-Key 필수)
  - POST /occurrences/{occurrenceId}/reviews/instructor (200/201) — createInstructorReview (Idempotency-Key 필수)
  - GET /occurrences/{occurrenceId}/reviews/summary (200) — getOccurrenceSummary
  - GET /tours/{tourId}/reviews/summary (200) — getTourSummary
  - GET /instructors/{instructorProfileId}/reviews/summary (200) — getInstructorSummary
  - GET /me/reviews (200) — getMyReviews
  - GET /tours/{tourId}/reviews (200) — listTourReviews

## Adapter.out.persistence
### JPA 엔티티
- `ReviewJpaEntity` — id, occurrenceId, reviewerUserId, type, rating, comment, createdAtUtc
- `IdempotencyJpaEntity` — id, userId, idempotencyKey, requestId, resultStatus, resultBody, createdAtUtc

### 어댑터 구현
- `JpaReviewRepositoryAdapter`
- `JpaIdempotencyRepositoryAdapter`

## Tests
### 단위
- `ReviewQueryServiceTest` ✅ — getOccurrenceSummary(), getTourSummary(), getInstructorSummary()

### 통합
- `ReviewControllerIntegrationTest` ✅ — 아이덤포턴시 재시도 검증 포함
  - 동일 Idempotency-Key 재요청 시 200 반환 (201이 아님)
  - 서로 다른 payload 재요청 시 422 IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD

### 실패 중
- 없음 (모두 통과)

## 관찰된 문제
1. **ReviewType 확장성**: 현재 TOUR, INSTRUCTOR만 지원 — 향후 ORGANIZATION, GUIDE 등 추가 시 계획 필요
2. **리뷰 삭제/수정 미지원**: 리뷰는 쓰기 전용 (GDPR/개인정보 삭제 요청 시 전략 필요)
3. **다중 리뷰 방지**: 동일 사용자가 동일 occurrence에 TOUR + INSTRUCTOR 리뷰 중복 작성 가능 (일반적이나 정책 재확인 필요)
4. **성능**: getTourSummary() 쿼리 시 모든 리뷰 조회 → 대규모 투어는 느릴 수 있음 (인덱스/NGRX 검토 권장)
5. **감사 이벤트**: Review 생성 시 AuditEvent 기록 여부 미확인 (domain-rules.md 규정 재확인)

## 스키마 검증
✅ 모든 timestamp UTC
✅ Idempotency-Key 저장 및 중복 검증
✅ rating 범위 제약 (CHECK CONSTRAINT 필요한지 확인)
⚠ comment 길이 제한 없음 (최대 길이 정의 권장)

## 아키텍처 체크
✅ adapter.in → application → domain 단방향
✅ Idempotency-Key 구현 완전함 (200/201/422 상태코드 정확)
✅ 아이덤포턴시 저장소 별도 포트 분리
⚠ RequestId 사용 목적 확인 필요 (로깅/추적?)

## 문제 없음 요약
- 리뷰 도메인은 상태머신이 없어 단순하고 명확함
- Idempotency-Key 구현이 CLAUDE.md 규정을 정확히 준수함
- 테스트 커버리지 우수 (단위 + 통합)
