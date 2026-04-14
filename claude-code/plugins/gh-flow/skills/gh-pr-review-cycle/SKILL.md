---
name: gh-pr-review-cycle
description: Address GitHub PR review comments in loop
---

Use this skill to iterate on feedback from GitHub PR review comments until all comments are resolved or explained. This skill handles comments from any reviewer (human or bot) and runs autonomously without requiring user input for each cycle.

## Prerequisites

- A PR must already exist (draft or open).
- The PR must have **pending review comments** to address.

## Steps

1.  **Gather Context**

    a. **Identify the PR**: Determine the PR number from the current branch.
    ```bash
    gh pr view --json number,title,url,state,isDraft
    ```

    b. **Fetch all review comments**: Retrieve all pending/unresolved review threads.
    ```bash
    gh api repos/{owner}/{repo}/pulls/{pr_number}/comments --paginate
    ```

    c. **Fetch review summaries**: Get top-level review bodies (not just inline comments).
    ```bash
    gh api repos/{owner}/{repo}/pulls/{pr_number}/reviews --paginate
    ```

    d. **Build a comment inventory**: Create a list of all unresolved comments, grouped by file and reviewer. For each comment, note:
    - **Reviewer**: Who left the comment.
    - **File & Line**: Where the comment applies.
    - **Body**: The actual feedback text.
    - **Thread ID**: For replying to the comment.
    - **Status**: Whether the thread is resolved or still open.

    Filter out already-resolved threads. Focus only on **open/pending** comments.

2.  **Review Feedback Loop**

    **Start an autonomous loop to address all open review comments:**

    > [!IMPORTANT]
    > Do NOT create implementation plans or request user review for each individual cycle. Proceed directly with fixes or explanations.

    a. **Assess Comment**: For each individual comment, perform a critical technical assessment, considering:
    - **Correctness**: Is the suggestion technically sound and idiomatic for this codebase?
    - **Necessity**: Does this fix a real issue, or is it a stylistic preference?
    - **Trade-offs**: Is our existing solution actually better for reasons the reviewer might not see (context, performance, constraints)?
    - **Scope**: Is this feedback about the changes in this PR, or about pre-existing code?

    b. **Decide: Fix or Explain**:
    - **IF FIX REQUIRED**: Implement the requested change following the `/gh-implement` principles. Read the relevant code, understand the full context, and make the fix.
    - **IF NO FIX REQUIRED**: Reply to the review comment thread explaining the technical rationale for keeping the current implementation. Be professional and constructive.

    c. **Reply to comment thread**: Whether fixing or explaining, always reply to the comment thread so the reviewer sees the response.
    ```bash
    gh api repos/{owner}/{repo}/pulls/{pr_number}/comments/{comment_id}/replies \
      -f body="<your response>"
    ```

    For top-level review comments (not inline), use:
    ```bash
    gh pr comment {pr_number} --body "<your response>"
    ```

    d. **Commit and Push** (invoke the `/semantic-commit-messages` skill):
    - Stage only the files that were changed to address review feedback.
    - Write a user-facing commit message describing the _actual fix_, NOT "address review comments".
    - Example: `fix(security): use case-insensitive header comparison`
    - Push to the remote branch:
    ```bash
    git push origin HEAD
    ```

    e. **Continue to next comment**: Move to the next unresolved comment and repeat from step (a).

3.  **Finalization**

    The loop terminates when:
    - (a) All review comments have been addressed with fixes and committed.
    - (b) All remaining comments have been responded to with valid explanations for why they won't be fixed.

    After all comments are handled:

    a. **Verify all threads responded to**: Re-fetch the comment list and confirm every open thread has a reply.
    ```bash
    gh api repos/{owner}/{repo}/pulls/{pr_number}/comments --paginate
    ```

    b. **Report completion**: Summarize the work done:
    - Total comments addressed.
    - Number of fixes implemented (with commit references).
    - Number of comments explained/rejected (with rationale summaries).
    - Any comments that were out of scope (with issue links if created).

    c. **Notify**: Post a summary comment on the PR indicating all feedback has been addressed.
    ```bash
    gh pr comment {pr_number} --body "All review comments have been addressed. See individual thread replies for details."
    ```
