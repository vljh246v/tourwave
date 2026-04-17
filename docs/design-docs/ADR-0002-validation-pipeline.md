---
name: ADR-0002 5-Stage Validation Pipeline
verification_status: active
last_verified: 2026-04-15
owner: team
---

# ADR-0002: 5-Stage Validation Pipeline

## Context

If pre-merge validation is optional, the agent may skip it or
merge problematic code with a "fix it later" attitude.

## Decision

`task-finish.sh` runs 5 validators in sequence.
If any validator fails, the merge is blocked.

| Order | Validator | Config Variable |
|-------|-----------|-----------------|
| 1 | 01-build.sh (build) | `BUILD_CMD` |
| 2 | 02-test.sh (test) | `TEST_CMD` |
| 3 | 03-lint.sh (lint) | `LINT_CMD` |
| 4 | 04-security.sh (security) | `SECURITY_SCAN_CMD` |
| 5 | 05-docs-freshness.sh (docs) | — |

Each validator reads settings from `harness.config.sh`,
so it works regardless of project language/framework.

## Consequences

**Pros**:
- Cannot bypass validation (task-finish.sh is the only merge path)
- Includes agent-friendly error messages → enables self-correction
- Can validate repeatedly during implementation (`verify-task.sh`)

**Cons**:
- Full validation takes time
- Possible bugs in the validators themselves

## Enforcement

- **Script**: `task-finish.sh` — all must pass to merge
- **Script**: `verify-task.sh` — run validation separately (no merge)
- **Config**: `harness.config.sh` — individual toggles via `ENABLE_*_CHECK`
- **Documentation**: `docs/design-docs/core-beliefs.md` #2
- **golden-principles**: GP-004
