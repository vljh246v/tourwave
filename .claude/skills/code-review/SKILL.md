---
name: code-review
description: PR code review checklist — use when a code review is needed
type: review
---

# Code Review Skill

Use this skill for PR reviews or code review requests.

## Review Checklist

### 1. Architecture Conformance
- [ ] Does the dependency direction match ARCHITECTURE.md?
- [ ] Are new classes/modules in the appropriate layer?
- [ ] Are there unnecessary cross-boundary dependencies?

### 2. Code Quality
- [ ] Does each function/method have a single responsibility?
- [ ] Is there duplicate code? (DRY principle)
- [ ] Do variable/function names clearly express intent?
- [ ] Are magic numbers/strings extracted to constants?

### 3. Safety
- [ ] Is null/undefined handling appropriate?
- [ ] Is exception handling appropriate? (No overly broad catches?)
- [ ] Are secrets/passwords not hardcoded?
- [ ] Are there SQL/Command Injection vulnerabilities?

### 4. Tests
- [ ] Are there tests for new features?
- [ ] Are edge cases covered?
- [ ] Do tests verify the implementation? (Not just padding coverage)

### 5. Documentation
- [ ] Are complex logic sections commented?
- [ ] Are related docs updated for API changes?
- [ ] Does QUALITY_SCORE.md need updating?

## Review Output Format

```
## Code Review Result

### Must Fix (MUST)
- [file:line] Description + how to fix

### Recommended (SHOULD)
- [file:line] Description

### Nice to Have (NICE TO HAVE)
- Description

### Approval Status
- [ ] APPROVED
- [x] CHANGES REQUESTED
```

## Notes

- When reviewing your own work, maintain an independent perspective
- Distinguish between "it works" and "it's well-built"
- Reference ARCHITECTURE.md and docs/design-docs/core-beliefs.md
