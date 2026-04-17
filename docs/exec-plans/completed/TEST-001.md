---
task_id: TEST-001
type: chore
phase: develop
base_branch: develop
status: in-progress
created: 2026-04-17T14:44:56Z
owner: vljh246v
---

# chore: 하네스 동작 확인용 샘플 태스크 (TEST-001)

## 배경 및 목표

harness.config.sh, ARCHITECTURE.md, escalation-policy.md 설정 완료 후
/harness-task 전체 플로우(계획 → 워크트리 → 구현 → 검증 → PR)가 정상 동작하는지 확인한다.

## 구현 단계

- [ ] 1. 영향 범위 파악 (변경 없음 — 동작 확인 목적)
- [ ] 2. README.md에 하네스 섹션 추가 (샘플 변경)
- [ ] 3. verify-task.sh로 검증 (빌드/테스트/린트/보안/문서)
- [ ] 4. origin/feature/TEST-001 push → PR 안내

## 완료 기준

- [ ] 검증기 5개 전부 통과
- [ ] 기존 테스트 통과

## 참고

- ARCHITECTURE.md
- docs/golden-principles.md
- logs/trends/failure-patterns.md
