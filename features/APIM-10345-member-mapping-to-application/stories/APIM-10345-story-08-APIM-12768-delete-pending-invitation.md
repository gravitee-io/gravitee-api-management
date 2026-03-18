# APIM-12768 Delete Pending Invitation

## Jira Story Reference
- Story: [APIM-12768](https://gravitee.atlassian.net/browse/APIM-12768)

## Relationship to the Parent Epic
- Parent Epic: [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345)
- This story allows the owner to revoke an invitation before it is accepted.

## Design References
### Screen 01 `../screens/Member - Populated list.png`
![APIM-12768 primary screen](../screens/Member%20-%20Populated%20list.png)
- Coverage: this screen confirms that a delete action is exposed on invited rows.
- Observed mismatch: the export does not show the required confirmation message before deletion and predates the separate-list direction agreed in `APIM-10345-decision-01`.

### Screen 02 `../screens/Member - Populated list-2.png`
![APIM-12768 supporting screen](../screens/Member%20-%20Populated%20list-2.png)
- Coverage: this supporting screen confirms that delete remains available across multiple invitation states.
- Observed mismatch: the export still does not cover revoked-link behavior after deletion, which is a required functional outcome, and its inline composition no longer matches the agreed first-delivery UX.

## Functional Requirements
### Invitation revocation
The owner must be able to delete a pending invitation from the application before it is accepted.

### Token invalidation
Deleting the invitation must invalidate the invitation for any future acceptance attempt so that a revoked invite can no longer be used to gain access.

## Acceptance Criteria
- Clicking delete shows a confirmation message.
- Confirming deletion removes the invite.
- Using a deleted invite later shows an expired or invalid link error.
- A previously invited user must not gain visibility into the application after the invite is deleted.

## Edge Cases and Failure Cases
- Repeated delete attempts on an already-deleted invite.
- Delete race with acceptance of the same invite.
- Failure to invalidate the token after UI removal.

## Open Questions
### APIM-12768-OQ-1
- Question: Should deleted invitations remain visible in audit history or activity logs for owners?
- Answer: TBD
- Status: Open

### APIM-12768-OQ-2
- Question: What exact user-facing error should a revoked invite display?
- Answer: The story only says the link should be expired or invalid, without final wording.
- Status: Open

## Test Considerations
- Verify confirmation flow and backend invalidation.
- Verify revoked-link behavior after deletion.
- Verify the pending list refreshes immediately after successful deletion.
