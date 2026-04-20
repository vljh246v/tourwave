---
id: T-900
title: "T-900 — [BE] detekt/ktlint 설정 (git pre-commit hook)"
aliases: [T-900]

repo: tourwave
area: be
milestone: cross
domain: infra
layer: tooling
size: M
status: done

depends_on: []
blocks: []
sub_tasks: []

github_issue: 4
exec_plan: ""

created: 2026-04-18
updated: 2026-04-19
---

#status/done #area/be

# T-900 — [BE] detekt/ktlint 설정 (git pre-commit hook)

## 파일 소유권
WRITE:
  - `build.gradle.kts` (detekt/ktlint 플러그인 추가)
  - `config/detekt/detekt.yml` (신규, 규칙 세트)
  - `config/ktlint.yml` (신규, Kotlin 스타일)
  - `.git/hooks/pre-commit` (신규, 자동 검증)
  - `scripts/validators/03-lint.sh` (수정, 라인 19-20 LINT_CMD 갱신)
  - `harness.config.sh` (LINT_CMD 정의 추가)

READ:
  - `build.gradle.kts` (현재 플러그인 목록)
  - `scripts/validators/03-lint.sh` (현재 lint 프레임워크)

DO NOT TOUCH:
  - `CLAUDE.md` (T-904에서 관리)
  - `docs/golden-principles.md` (T-900 완료 후 추가)

## SSOT 근거
- 감사 관찰 `BE-common.md` 미기록
- `CLAUDE.md` "알려진 미커버 항목" — lint 인프라 부재
- `escalation-policy.md` 6번 "빌드/배포" — build.gradle.kts 변경은 사람 승인 대상 가능

## 현재 상태 (갭)
- [ ] detekt 플러그인 미설정 (정적 분석 오류 무시)
- [ ] ktlint 플러그인 미설정 (Kotlin 스타일 검증 부재)
- [ ] 구성 파일 부재 (`config/`)
- [ ] pre-commit hook 미설정 (로컬 검증 자동화 없음)
- [ ] `scripts/validators/03-lint.sh` LINT_CMD 상수값 없음

## 구현 지침
1. `build.gradle.kts`에 다음 플러그인 추가:
   ```kotlin
   id("io.gitlab.arturbosch.detekt") version "1.23.6"
   id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
   ```
2. detekt 설정:
   - `config/detekt/detekt.yml` 생성 (baseline rule set)
   - `build.gradle.kts`에 `detekt { config = files("config/detekt/detekt.yml") }` 추가
3. ktlint 설정:
   - `config/ktlint.yml` 생성 (editorconfig 호환)
   - Gradle 태스크: `./gradlew ktlintFormat` / `ktlintCheck`
4. pre-commit hook 생성:
   - `./gradlew ktlintCheck && ./gradlew detekt`
   - 실패 시 commit 차단
5. `harness.config.sh`:
   - `LINT_CMD="./gradlew ktlintCheck detekt"`

## Acceptance Criteria
- [ ] `./gradlew ktlintCheck` 성공 (기존 코드베이스)
- [ ] `./gradlew detekt` 성공 (기존 코드베이스)
- [ ] git pre-commit hook 설치 확인
- [ ] `./scripts/verify-task.sh T-900` 통과

## Verification
`./scripts/verify-task.sh T-900`
- gradle 플러그인 로드 검증
- config 파일 존재 확인
- pre-commit hook 실행 테스트 (dummy commit)
- `harness.config.sh` LINT_CMD 값 검증

## Rollback
- `git checkout build.gradle.kts harness.config.sh`
- `rm -rf config/detekt config/ktlint.yml`
- `rm .git/hooks/pre-commit`

## Notes
- **사람 승인 필요 가능성**: escalation-policy.md 6번 — build.gradle.kts 플러그인 major 버전 변경은 팀 리뷰 권장. 현재 버전 선택지는 stable (1.23.6, 12.1.0).
- detekt 규칙은 "기존 코드베이스 통과"를 기본 설정으로. false-positive는 주석으로 억제 가능.
- ktlint는 Gradle 플러그인 기본 설정 (2024 style) 사용.
