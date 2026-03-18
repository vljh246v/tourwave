# Trust Surface Policy

이 문서는 Sprint 19 기준으로 review trust surface, moderation 범위, favorites/notifications UX 규칙을 고정한다.

## 1. Review Aggregation Policy

- aggregation refresh 방식은 materialized projection이 아니라 query-time 계산이다.
- source of truth는 `reviews` 원본과 `occurrences` / `tours` / `instructor_profiles` 연결 상태다.
- public scope는 현재 공개 surface에 맞춰 계산한다.
  - `GET /tours/{tourId}/reviews/summary`: published tour 하위 occurrence의 `TOUR` review만 집계한다.
  - `GET /instructors/{instructorProfileId}/reviews/summary`: active instructor profile이면서 published tour 하위 occurrence의 `INSTRUCTOR` review만 집계한다.
  - `GET /organizations/{organizationId}/reviews/summary`: published tour 하위 occurrence의 `TOUR` / `INSTRUCTOR` review를 organization 단위로 집계한다.
- operator scope는 운영 truth를 그대로 본다.
  - `GET /operator/organizations/{organizationId}/reviews/summary`: organization operator가 같은 organization의 모든 occurrence review를 집계해서 본다.
- summary payload는 `count`, `averageRating`, `aggregationMode=QUERY_TIME`를 기준으로 유지한다.

## 2. Moderation Decision

- 2026-03-19 기준 MVP 결정은 `no-build`다.
- 이유:
  - 현재 public UGC surface는 attendance 기반 review로 제한돼 있고, free-form community/post/comment surface가 없다.
  - organization operator가 review 원문을 직접 수정하거나 숨기는 정책은 trust surface 왜곡 위험이 있다.
  - launch 전까지 필요한 것은 platform moderation console이 아니라 review aggregation consistency와 운영 정책 명확화다.
- 따라서 현재 runtime / OpenAPI / current-truth 문서에는 moderation endpoint를 포함하지 않는다.
- 예외 대응은 운영 절차로 처리한다.
  - 법적 이슈, 명백한 abuse, 개인정보 노출은 Jira + 운영자 수동 조치로 대응한다.
  - product contract에 moderation API를 노출하지 않는다.
- 재검토 트리거:
  - public community surface 추가
  - review 신고/appeal 요구가 반복
  - 규제/법무 요구로 운영 audit trail이 API 수준에서 필요

## 3. Favorites UX Rules

- `GET /me/favorites`는 현재 `createdAt DESC` 정렬을 표준으로 본다.
- favorite 대상은 published tour만 허용한다.
- favorite list filter는 MVP에서 추가하지 않는다.
- unpublished / deleted equivalent tour는 목록 응답에서 제외한다.
- follow-up backlog 후보:
  - organization filter
  - recently updated sort
  - favorite count projection

## 4. Notifications UX Rules

- `GET /me/notifications`는 현재 `createdAt DESC` 정렬을 표준으로 본다.
- unread 상태는 `readAt == null` 로 계산한다.
- MVP에서는 unread-count 전용 endpoint를 추가하지 않는다.
- MVP에서는 cursor pagination을 추가하지 않는다.
  - 현재 notification volume은 단일 list read model로 감당 가능한 범위로 가정한다.
- noisy notification suppression 규칙:
  - inquiry self-message는 customer notification을 만들지 않는다.
  - 같은 audit event는 같은 resource/action/user 조합으로 하나의 notification만 만든다.
  - outbound delivery retry는 delivery log 레벨에서 처리하고, in-app notification read model을 중복 생성하지 않는다.
- follow-up backlog 후보:
  - unread count endpoint
  - cursor pagination
  - type filter
  - digest/bundling 정책
