---
name: ADR-0001 Worktree Isolation Principle
verification_status: active
last_verified: 2026-04-15
owner: team
---

# ADR-0001: Worktree Isolation Principle

## Context

When both agents and humans work directly on the develop/main branch,
mistakes immediately contaminate the default branch.
The risk of conflicts also increases during parallel work.

## Decision

All work is performed in a git worktree created by `task-start.sh`.
Direct modification of default branches is forbidden.

## Consequences

**Pros**:
- Blast radius of mistakes is confined to the worktree
- Parallel work is possible (independent worktrees)
- Failed worktrees are preserved for debugging

**Cons**:
- Overhead of worktree creation/deletion
- Increased disk usage

## Enforcement

- **Hook**: `.claude/hooks/pre-tool-use.sh` — warns on src/ edits outside a worktree
- **Script**: `task-finish.sh` — blocks merge without passing validators
- **Documentation**: `docs/design-docs/core-beliefs.md` #1
- **golden-principles**: GP-001
