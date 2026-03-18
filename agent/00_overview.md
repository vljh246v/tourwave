# Tourwave Overview

## Goal

Tourwave는 투어/액티비티 운영사가 실제로 판매하고 운영할 수 있는 예약 플랫폼 백엔드를 만드는 프로젝트다. 제품 관점의 핵심 흐름은 다음 3개다.

- 고객 예약: search, booking, waitlist, offer, refund
- 운영 실행: organization, instructor, tour, occurrence, participant, inquiry
- 사후 관리: attendance, review, payment reconciliation, worker automation

## Business Positioning

현재 코드는 "예약 플랫폼 백엔드 MVP"에 가깝다. 예약/대기열/참가자/문의/리뷰/환불 계산의 핵심 도메인은 존재하고, organization/instructor/tour/occurrence authoring과 customer self-service도 일부 올라와 있다. 다만 실제 판매용 제품 기준으로는 중앙 인증 강제, account lifecycle, 실결제/실스토리지/실알림 연동, 운영 alerting이 아직 충분하지 않다.

실제 프로덕트 출시까지 반드시 필요한 축:

- auth/account: signup, login, refresh, `me`, email verification, password reset, perimeter auth enforcement
- operator console: organization member management, instructor onboarding, tour/occurrence authoring
- customer surface: public catalog, availability, favorites, notifications, calendar, review aggregation
- payment ops: external PG callback, refund failure handling, reconciliation, provider secret rotation
- platform ops: metrics, alerting, distributed lock, contract verification, launch runbook

## Current Product Snapshot

Sprint 1~6 범위에서 이미 확보된 것은 다음과 같다.

- booking lifecycle와 waitlist/offer 상태 전이
- participant invitation, attendance, review eligibility
- inquiry thread와 operator manual action
- refund preview, refund retry, payment ledger model
- MySQL/JPA/Flyway 기반 persistence와 concurrency guard
- worker 진입점과 만료/재시도 계열 잡

아직 제품 비전에 남아 있는 것은 다음과 같다.

- Spring Security perimeter enforcement와 사용자 계정 라이프사이클 마감
- email verification / password reset / account deactivation 정책의 실제 API와 delivery flow
- real asset storage / real payment provider / outbound notification delivery
- public review summary by tour / instructor / organization
- announcements / operator reporting / moderation 정책 결정 및 필요 시 구현
- alert routing / dashboard / SLO / broader real MySQL verification

## Delivery Principle

이 저장소의 다음 단계 개발은 "도메인 완성"보다 "제품 전달 가능성"을 우선한다.

- 먼저 문서와 실제 코드를 동기화한다.
- 그 다음 계정/운영/결제/관측성처럼 출시 차단 항목을 해결한다.
- 마지막으로 부가 기능을 확장한다.

## Key Concepts

- `Tour`: 판매용 템플릿
- `Occurrence`: 실제 운영 회차
- `Organization`: 투어 운영 주체
- `Booking`: leader가 만든 예약
- `BookingParticipant`: leader 포함 참가자 엔티티
- `Waitlist Offer`: 좌석이 열렸을 때 제공되는 한시적 제안
- `Inquiry`: booking 또는 occurrence 문맥의 상담 스레드
- `Attendance`: participant 단위 출석 상태
- `Review`: attendance 기반 후기 작성

## Runtime Shape

현재 코드는 같은 코드베이스 안에 두 개의 실행 진입점을 가진다.

- API: `TourwaveApplication`
- Worker: `WorkerApplication`

Gradle 멀티모듈로 아직 분리되지는 않았지만, 운영 관점에서는 이미 `API 서비스 + 백그라운드 워커` 형태로 생각해야 한다.

## Important Reading Rule

다른 에이전트가 바로 개발을 시작하려면 아래 순서가 가장 빠르다.

1. `09_spec_index.md`
2. `11_current_implementation_status.md`
3. `13_api_status_matrix.md`
4. `16_product_delivery_roadmap.md`
5. `17_release_gap_execution_plan.md`
6. `12_runtime_topology_and_operations.md`
7. `01_domain_rules.md`
8. `10_architecture_hexagonal.md`
9. `14_test_traceability_matrix.md`

## Notes

- `04_openapi.yaml`은 목표 계약 문서다.
- 현재 실제 구현 상태는 `13_api_status_matrix.md`가 더 정확하다.
- Sprint 14까지의 구현 완료 기록은 `16_product_delivery_roadmap.md`를 따른다.
- 현재 제품 갭과 출시 차단 항목의 우선순위는 `17_release_gap_execution_plan.md`를 따른다.
