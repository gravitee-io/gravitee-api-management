# APIM-12771 Update Roles for Invited Members

## Jira Story Reference
- Story: [APIM-12771](https://gravitee.atlassian.net/browse/APIM-12771)

## Relationship to the Parent Epic
- Parent Epic: [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345)
- This story covers role maintenance for users who are invited but not yet active.

## Design References
### Screen 01 `../screens/Member - Populated list.png`
![APIM-12771 primary screen](../screens/Member%20-%20Populated%20list.png)
- Coverage: this screen confirms that invited rows expose edit actions.
- Observed mismatch: the export shows those actions inline in the members list, while `APIM-10345-decision-01` now requires a separate invitations surface for the first delivery. It also does not show a dedicated role-edit dialog or any explicit evidence that the invitation token remains valid after the role change.

### Screen 02 `../screens/Member - Populated list-2.png`
![APIM-12771 supporting screen](../screens/Member%20-%20Populated%20list-2.png)
- Coverage: this supporting screen extends the reference to an `Expired` invited row.
- Observed mismatch: the export does not clarify whether expired invitations are editable, which is an important branch for this story, and it still reflects the pre-decision inline-list composition.

## Functional Requirements
### Role updates before acceptance
The owner must be able to change the role assigned to a pending invitation before the invited user becomes an active member.

### Token continuity
A role-only update must not invalidate the invitation token. The invited user should still be able to accept the same invite after the role change unless product explicitly introduces a different lifecycle rule.

## Acceptance Criteria
- The owner can edit the assigned role for any pending invite.
- Updating the role does not invalidate the invitation link.
- The pending-invites UI reflects the new role immediately.

## Edge Cases and Failure Cases
- Updating the role after the invite has expired.
- Updating the role while the invite is being accepted.
- UI state becomes stale after a successful update.

## Open Questions
### APIM-12771-OQ-1
- Question: Should changing the role on an expired invite reactivate the invite?
- Answer: There is no indication of reactivation behavior in Jira or the exported screens.
- Status: Open

### APIM-12771-OQ-2
- Question: Is notification required when the assigned role changes before acceptance?
- Answer: TBD
- Status: Open

## Test Considerations
- Verify role changes on active versus expired invitations.
- Verify token continuity after role updates.
- Verify immediate UI refresh.
