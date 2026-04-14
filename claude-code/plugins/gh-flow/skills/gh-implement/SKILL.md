---
name: gh-implement
description: Execute implementation plan with TDD and atomic commits
---

Use this skill to execute the approved plan.

## Steps

1. **Context Loading**
    - Check if `implementation_plan.md` exists.
    - **IF MISSING**: Fetch the PR body using `gh pr view` and extract the plan from the description.
    - Load `task.md`.

2. **Execution Loop**
   **Iterate through every unchecked item in `task.md`:**

    a. **Implement**: Write code for the sub-task.

    b. **Verify (Self-Correction)**:
    - For Node/JS: Run `pnpm lint`, `pnpm build`, `pnpm test`.
    - For Java/Maven: Run the project's formatter if configured (e.g., `mvn spotless:apply`), then `mvn test`.
    - **IF FAILURE**: Fix code and re-run until green.

    c. **Commit** (invoke the `/semantic-commit-messages` skill):
    - `git add .`
    - Write a user-facing commit: `git commit -m "<type>(<optional-scope>): <description>"`

    d. **Update Artifact**: Mark sub-task complete in `task.md`.

3. **Done**
    - Verify all tests pass one final time.
    - Report completion: "Implementation complete. All tests passing. Run `/gh-create-pr` to push and open a draft PR."
