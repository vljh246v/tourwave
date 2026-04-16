# Tech Debt Tracker

즉시 수정하지 않고 보류한 기술 부채를 추적합니다.
해결 시 해당 행을 삭제하고 커밋 메시지에 `refs: TD-XXX`를 포함합니다.

## 활성 부채

| ID | 설명 | 심각도 | 등록일 | 관련 파일 |
|----|------|--------|--------|-----------|
| TD-001 | AuditEventTest 미존재 — 감사 커버리지가 통합 테스트에만 부분적으로 존재 | MEDIUM | 2026-04-17 | `application/booking/*` |
| TD-002 | CommunicationReportingIntegrationTest, OccurrenceCatalogControllerIntegrationTest main 브랜치에서 실패 중 | HIGH | 2026-04-17 | `adapter.in.web.*` |

## 심각도 기준

| 등급 | 의미 | 대응 |
|------|------|------|
| HIGH | 장애 위험 또는 보안 취약점 | 1주 내 해결 |
| MEDIUM | 개발 생산성 저하 | 1개월 내 해결 |
| LOW | 코드 품질 개선 | 분기 내 해결 |
