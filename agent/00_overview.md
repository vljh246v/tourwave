# Tourwave Overview

## Goal

Tourwave는 투어/액티비티 예약 백엔드를 만드는 프로젝트다. 핵심 제품 흐름은 다음 4개다.

- booking / waitlist / offer / refund
- participant invitation / attendance / review eligibility
- inquiry ticket conversation
- operator workflow + worker jobs

## Current Product Snapshot

Sprint 1~6 범위까지 구현이 완료된 상태다.

- participant, invitation, attendance, review eligibility 구현됨
- booking detail / inquiry detail-list / roster / waitlist operator flow 구현됨
- refund policy, refund preview, payment ledger, refund retry 구현됨
- role 기반 actor context, topology minimum model, worker jobs 구현됨
- MySQL 기준 JPA/Flyway 영속 계층과 동시성 가드가 추가됨

아직 제품 비전에 남아 있는 영역:

- auth / JWT / me lifecycle
- org/member management full CRUD
- assets / favorites / announcements / reports
- calendar export
- public review aggregation by tour / instructor / organization
- external payment webhook / callback

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

Gradle 멀티모듈로 아직 분리되지는 않았지만, 문서와 구현 모두 `같은 코드베이스, 다른 실행 모드`를 목표로 정리되어 있다.

## Important Reading Rule

다른 에이전트가 바로 개발을 시작하려면 아래 순서가 가장 빠르다.

1. `09_spec_index.md`
2. `11_current_implementation_status.md`
3. `12_runtime_topology_and_operations.md`
4. `01_domain_rules.md`
5. `10_architecture_hexagonal.md`
6. `08_operational_policy_tables.md`
7. `13_api_status_matrix.md`
8. `14_test_traceability_matrix.md`

## Notes

- `04_openapi.yaml`은 목표 계약 문서다.
- 현재 실제 구현 상태는 `13_api_status_matrix.md`가 더 정확하다.
- 테스트 진실원은 `14_test_traceability_matrix.md`와 실제 `src/test` 코드다.
