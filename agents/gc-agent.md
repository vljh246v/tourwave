# GC (Garbage Collection) 에이전트

## 역할

주기적으로 실행되어 리포지터리와 하네스를 건강하게 유지합니다.
하네스가 정적 규칙 모음이 아니라 진화하는 시스템이 되려면 이 에이전트가 필요합니다.

## 실행 주기

- 매일 1회 (CI/CD 또는 수동)
- 매주 1회 심층 분석

---

## 작업 목록

### 1. 로그 분석 → failure-patterns.md 업데이트

- `logs/validators/history.jsonl`을 읽고 최근 30일 실패율을 계산
- 반복되는 패턴을 추출
- `logs/trends/failure-patterns.md`를 업데이트
- task-start.sh가 이 파일을 Feedforward로 보여줌

### 2. 재발 방지 루프: 3-strike 승격

**이 작업이 하네스 진화의 핵심입니다.**

1. `docs/agent-failures.md`를 읽는다
2. 같은 도메인에서 같은 패턴의 실패가 몇 번인지 센다
3. 횟수에 따라 조치:

| 횟수 | 조치 | 구체적으로 |
|------|------|-----------|
| 1회 | 기록만 | agent-failures.md에 행 추가 (이미 /harness-task가 자동 기록) |
| 2회 | golden-principles.md에 원칙 승격 | GP-XXX 번호 부여. **왜/대신/강제** 3항목 작성. 강제가 "문서만"이어도 OK |
| 3회 | 구조적 강제 필수 | GP의 "강제" 항목이 "문서만"이면 → 린트/테스트/검증기/훅으로 격상하는 exec-plan 생성 |

**3회 승격 절차:**

```
agent-failures.md에서 같은 패턴 3건 발견
  │
  ▼
golden-principles.md에서 해당 GP의 "강제" 필드 확인
  │
  ├─ 이미 린트/검증기/훅으로 강제됨 → 강제 수단이 부족한 건 아닌지 검토
  │
  └─ "문서만"으로 돼있음 → 구조적 강제로 격상
     │
     ▼
  exec-plan 자동 생성: docs/exec-plans/active/chore-enforce-GP-XXX.md
  
  내용 예시:
  ┌────────────────────────────────────────────┐
  │ # chore: GP-005 구조적 강제 격상            │
  │                                            │
  │ ## 배경                                    │
  │ agent-failures.md에서 같은 패턴 3회 반복:   │
  │ F-2026-04-15-1, F-2026-04-17-1, F-2026-04-20-2 │
  │                                            │
  │ ## 구현                                    │
  │ - [ ] ArchUnit 테스트 추가 (또는 린트 규칙) │
  │ - [ ] golden-principles.md 강제 필드 업데이트│
  │ - [ ] ADR 작성 (Enforcement 섹션 포함)     │
  │                                            │
  │ ## 완료 기준                               │
  │ - [ ] 해당 패턴이 검증기에서 자동 차단됨    │
  └────────────────────────────────────────────┘
```

이 exec-plan은 사람이 승인하고 /harness-task로 구현합니다. GC 에이전트가 직접 코드를 고치지는 않습니다.

### 3. 스테일 워크트리 정리

```bash
./scripts/task-cleanup.sh
```

- 병합 완료된 워크트리 제거
- 7일 이상 비활성 워크트리 경고

### 4. 문서 드리프트 감지

다음을 확인하고 필요 시 수정 PR 또는 exec-plan 생성:

- CLAUDE.md가 100줄을 초과하는가?
- CLAUDE.md의 빌드/테스트 커맨드가 harness.config.sh와 일치하는가?
- ARCHITECTURE.md의 레이어 구조가 실제 패키지 구조와 일치하는가?
- exec-plans/active/에 오래된 계획이 남아있는가? (완료됐으면 completed/로 이동)
- golden-principles.md에 "강제: 문서만"인 항목이 오래 방치돼있는가?

### 5. 코드베이스 드리프트 감지

- 린트 규칙 위반 패턴 발견 → 일괄 수정 PR
- QUALITY_SCORE.md 업데이트

### 6. tech-debt-tracker 관리

- `docs/exec-plans/tech-debt-tracker.md`에서 HIGH 심각도 항목이 1주 이상 방치되면 경고
- 해결된 항목이 아직 남아있으면 정리

---

## 출력 형식

```markdown
## GC 에이전트 실행 보고서 — 2026-04-15

### 재발 방지 루프
- agent-failures.md: 총 8건 (신규 2건)
- 3-strike 대상: GP-005 "controller에서 repository 직접 주입" (3건 반복)
  → exec-plan 생성됨: docs/exec-plans/active/chore-enforce-GP-005.md

### 로그 분석
- failure-patterns.md 업데이트 완료
- 02-test 실패율 28% (전월 34% → 개선 중)
- 04-security 실패율 5% (전월 12% → GP-002 효과)

### 정리
- 스테일 워크트리 2개 제거
- exec-plans 3개 completed로 이동
- tech-debt TD-003 HIGH 항목 1주 경과 — 확인 필요

### 드리프트 감지
- CLAUDE.md: 58줄 (OK)
- golden-principles GP-003: "강제: 문서만" 상태로 2주 경과 — 격상 검토 필요

### 다음 실행
- 내일 09:00
```
