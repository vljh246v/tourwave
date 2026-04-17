---
name: debugging
description: Systematically track and fix bugs or test failures
type: debugging
---

# Debugging Skill

Use when encountering bugs, test failures, or unexpected behavior.

## Systematic Debugging Flow

```
1. Reproduce — find a minimal, consistent reproduction case
2. Isolate — narrow down the problem scope
3. Hypothesize — form a theory about the cause
4. Verify — test the hypothesis with a test
5. Fix — fix the root cause (not the symptom)
6. Confirm — verify with the reproduction case after fixing
```

## Before Starting

```bash
# Check session log for events before the failure
cat logs/sessions/<task-name>/session.jsonl | python3 -m json.tool | grep "error\|fail"

# Check recent validator failure patterns
cat logs/trends/failure-patterns.md

# Search validator history for similar failures
cat logs/validators/history.jsonl | python3 -c "
import sys, json
for line in sys.stdin:
  d = json.loads(line)
  if d.get('result') == 'fail':
    print(d)
"
```

## Cause Classification

### Compile/Build Errors
- Type mismatch, undeclared variable → fix the file/line directly
- Missing dependency → check the build file
- Layer violation → refer to ARCHITECTURE.md

### Runtime Errors
- NullPointerException → add null input cases, use Optional
- ClassCastException → add type checking logic
- StackOverflow → check recursion termination condition

### Test Failures
- Assertion failure → compare expected vs actual values
- Setup issue → check @BeforeEach, fixtures
- Timing issue → check async/await, remove sleep

## Log Analysis Patterns

```bash
# Search for specific error patterns
grep -r "ERROR\|Exception\|FAIL" logs/sessions/ | tail -20

# Correlate file changes with failures
cat logs/sessions/<task>/session.jsonl | python3 -c "
import sys, json
events = [json.loads(l) for l in sys.stdin]
for i, e in enumerate(events):
  if e.get('event') == 'validator' and e.get('result') == 'fail':
    # Print 5 events before the failure
    for prev in events[max(0,i-5):i]:
      print('BEFORE:', prev)
    print('FAIL:', e)
"
```

## Never Do This

- Hide symptoms (swallowing exceptions with try-catch)
- Hardcode fixes for specific test cases
- Modify tests to match failing behavior (fix the implementation instead)
- Leave `// TODO: fix later` and move on

## Post-Fix Checklist

- [ ] Verified fix against the reproduction case
- [ ] Checked for similar cases elsewhere (same bug in other places?)
- [ ] Added tests to prevent recurrence
- [ ] Fix details automatically logged in session.jsonl
