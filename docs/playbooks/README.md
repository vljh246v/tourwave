# Playbooks — Recipes for Recurring Tasks

Step-by-step procedures for high-risk or repetitive tasks.
Each checklist item originates from actual failures in `agent-failures.md`.

## List

| Playbook | Target Task |
|----------|-------------|
| (add per your project) | |

## Writing a New Playbook

1. Create `docs/playbooks/<kebab-case-name>.md`
2. Use the template below
3. Add a row to this README

## Template

```markdown
---
name: Playbook Name
last_verified: YYYY-MM-DD
related_failures: F-XXXX (agent-failures.md ID)
---

# Playbook: Title

## When to Use

Conditions that trigger this playbook.

## Prerequisites

- [ ] Condition 1
- [ ] Condition 2

## Procedure

### Step 1: ...

- [ ] Check 1
- [ ] Check 2

### Step 2: ...

- [ ] Check 1

## Common Mistakes

| Mistake | Consequence | Correct Approach |
|---------|-------------|-----------------|
| ... | ... | ... |

## Verification

- [ ] verify-task.sh passes
- [ ] Related tests pass
```
