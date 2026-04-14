---
name: review-cycle-copilot
description: Address Copilot review feedback in loop
---

Use this skill to iterate on feedback from GitHub Copilot until all comments are addressed or explained. This skill is designed to run autonomously without requiring user input for each cycle.

## Steps

1.  **Request Copilot Review**
    - Verify PR exists and is in draft state using `gh pr view`
    - Request Copilot review using `gh pr edit --add-reviewer`
    - Wait for initial review to complete before entering the feedback loop

2.  **Review Feedback Loop**
    **Start an autonomous loop to address all open Copilot review comments:**

    > [!IMPORTANT]
    > Do NOT create implementation plans or request user review for each individual cycle. Proceed directly with fixes or explanations.

    a. **Assess Comment**: For each individual comment, perform a critical technical assessment, considering:
    - **Correctness**: Is the suggestion technically sound and idiomatic for this codebase?
    - **Necessity**: Does this fix a real issue, or is it a stylistic preference or hallucination?
    - **Trade-offs**: Is our existing solution actually better for reasons Copilot might not see (context, performance, constraints)?

    b. **Decide: Fix or Reject**:
    - **IF FIX REQUIRED**: Implement the requested change following the `/gh-implement` principles.
    - **IF NO FIX REQUIRED**: Reply to the review comment using `gh pr comment` outlining the technical rationale for rejecting the suggestion. Be professional but firm about why the current implementation is preferred.

    c. **Commit and Push** (invoke the `/semantic-commit-messages` skill):
    - `git add .`
    - Write a user-facing commit message describing the _actual fix_, NOT "address copilot review comments"
    - Example: `fix(security): use case-insensitive header comparison`
    - `git push origin HEAD`

    d. **Re-request Review**: Re-request a review from Copilot to trigger a new scan.

    e. **Wait for Copilot Review**: Poll for new review comments. Copilot typically responds within 1-3 minutes, but can take up to 5 minutes on large diffs.

    > [!IMPORTANT]
    > Do NOT use a fixed `sleep`. Instead, **poll** for new review comments using the following strategy:
    >
    > 1. Record the current thread count by calling `gh pr view` with review comments BEFORE re-requesting the review. Count only threads from `copilot-pull-request-reviewer`.
    > 2. After re-requesting, poll every 30 seconds by checking review comments again and comparing the thread count.
    > 3. Stop polling when new threads from `copilot-pull-request-reviewer` appear (thread count increases), OR when Copilot submits a review with no new comments.
    > 4. **Timeout**: If no new activity appears after **5 minutes** (10 polls), proceed anyway — Copilot may have encountered an error.
    >
    > Between polls, use `sleep 30` to wait.

    ```bash
    sleep 30
    ```

    f. **Loop**: Return to step (a) and repeat until the termination conditions are met.

3.  **Update Walkthrough & Mark Ready**
    - The loop terminates when:
        - (a) All Copilot comments have been addressed with fixes and verified.
        - (b) All remaining comments have been responded to with valid explanations for why they won't be fixed.
    - Update `walkthrough.md` to document all fixes and explanations provided during the Copilot review cycle.
    - Mark PR as ready for review (`draft: false`) using `gh pr ready`.
    - Report completion: "Copilot review cycle complete. PR marked ready for review."
