# Constraint-Based TDD

Use Test-Driven Development as an **alignment guardrail** at the PR level, not just a code quality tool.

## Core Discipline

### 1. Write Acceptance Tests First

- At the start of a PR, write a set of **failing tests** that encode the acceptance criteria.
- These tests define what "done" looks like — they should fail initially and pass when the PR is correctly implemented.
- Commit them early (red) before writing any implementation code.

### 2. Implement Toward Green

- All implementation work is motivated by making the acceptance tests pass.
- Every code change should be traceable back to a failing test.
- When all acceptance tests pass, the PR is functionally complete.

### 3. Treat the Test Layer as Near-Immutable

Once acceptance tests are committed, they should **rarely change**. A test may only be modified if:

1. **Factual error**: The test itself is wrong (e.g., asserts the wrong expected value).
2. **Requirements evolved**: Our understanding of the requirement changed, and we explicitly acknowledge this.

If neither condition is met, **do not modify the test**. If the implementation doesn't match the test, the implementation is wrong — not the test.

### 4. Escalate Before Modifying Tests

- If you feel the urge to change an acceptance test during implementation or review, **STOP**.
- Flag it to the developer with a clear explanation of why the test seems wrong.
- Wait for explicit approval before modifying any acceptance test.
- Silently adjusting tests to make the implementation "pass" defeats the entire purpose.

## Why This Matters

- If acceptance tests are stable, they act as a **constraint** ensuring the final code matches the original intent.
- If they change frequently, you lose all guarantees — the tests become a rubber stamp, not a verification layer.
- During review cycles (e.g., addressing Copilot feedback), it's especially easy to drift from the original goal. Stable tests prevent that drift.
