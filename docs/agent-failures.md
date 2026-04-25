# Agent Failures — 에이전트 실패 로그

에이전트가 범한 실수와 그 구조적 대응을 기록합니다.

## 승격 정책

- 같은 패턴 **2회 반복** → `golden-principles.md`에 원칙 추가
- 같은 패턴 **3회 반복** → 린트/테스트로 **구조적 불가능화** 필수

## 실패 기록

| ID | 날짜 | 증상 | 근본 원인 | 강화책 | 도메인 |
|----|------|------|-----------|--------|--------|
| F-2026-04-26-1 | 2026-04-26 | T-001 orchestrator가 구현 완료 보고 후 task-finish.sh 1차 실행 시 빈 merge("Already up to date"), 워크트리 cleanup 실패. 실제로는 구현 파일이 모두 워크트리에 미커밋 상태로 잔존 | tdd-backend 서브에이전트가 Write로 파일 생성 후 git commit 누락. verify 검증기는 작업트리 파일을 직접 읽어 PASS, orchestrator/Phase 8도 워크트리 git status 미확인 | (1) task-finish.sh 시작 시 `git status --porcelain` 가드 추가 권고 — 미커밋 시 즉시 abort. (2) orchestrator.md Phase 8.5 "워크트리 git status 청결 확인" 단계 추가 검토. (3) 반복 시 golden-principles 승격 | 워크플로우 |

## 기록 방법

- **ID**: `F-YYYY-MM-DD-N`
- **증상**: 외부에서 관찰 가능한 현상
- **근본 원인**: 구조적 사유
- **강화책**: ADR, 린트, 검증기, 문서 링크
- **도메인**: 워크플로우 / 보안 / 아키텍처 / 테스트 / 문서 등
