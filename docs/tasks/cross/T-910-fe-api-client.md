---
id: T-910
title: "[FE] common — API 클라이언트 fetch 래퍼 (Bearer token, 에러 처리, 재시도) + OpenAPI 타입 sync"
status: done
scope: FE-only
depends_on: []
blocks: []
github_issue: null
completed_at: "2026-04-28"
---

## 목표

gap-matrix T-910: FE API 클라이언트 기반 구축.
핵심 구현(client.ts, errors.ts, schema.ts)은 이미 develop에 존재했음.
두 가지 갭 해소:
1. BE T-903에서 추가된 Idempotency-Key 헤더가 FE openapi/openapi.yaml에 미반영 → sync + schema.ts 재생성
2. lib/api/ 모듈 단위 테스트 전무 → 25개 추가

## 완료 기준

- [x] openapi/openapi.yaml T-903 Idempotency-Key 4도메인 sync
- [x] schema.ts 재생성 (openapi-typescript)
- [x] errors.test.ts 신규 (9개)
- [x] client.test.ts 신규 (16개)
- [x] verify-task.sh PASS 6/6
- [x] develop 병합 + push

## 변경 파일

| 파일 | 변경 |
|---|---|
| `openapi/openapi.yaml` | 수정 (51 insertions) |
| `src/lib/api/schema.ts` | 재생성 (73↑/17↓) |
| `src/lib/api/__tests__/errors.test.ts` | 신규 |
| `src/lib/api/__tests__/client.test.ts` | 신규 |

## 검증

- FE verify-task.sh: PASS 6/6
- 테스트: 64개 PASS (기존 39 + 신규 25)
- typecheck: PASS / build: PASS / security: CLEAN
