# APIM-12767 View Pending Invitation Dashboard

## Jira Story Reference
- Story: [APIM-12767](https://gravitee.atlassian.net/browse/APIM-12767)

## Relationship to the Parent Epic
- Parent Epic: [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345)
- This story makes invitation state visible after invite creation.

## Design References
### Screen 01 `../screens/Member - Populated list.png`
![APIM-12767 primary screen](../screens/Member%20-%20Populated%20list.png)
- Coverage: this screen confirms the required invitation row data and `Pending` invitation state.
- Observed mismatch: the export shows an inline-table presentation, but `APIM-10345-decision-01` now requires a separate `Invitations` list or section for the first delivery.

### Screen 02 `../screens/Member - Populated list-2.png`
![APIM-12767 supporting screen](../screens/Member%20-%20Populated%20list-2.png)
- Coverage: this supporting screen adds the `Expired` invitation state and helps confirm invitation-specific row content.
- Observed mismatch: the export still reflects the pre-decision inline-list variant and does not clearly show invitation date and pagination as explicit fields.

## Functional Requirements
### Invitation visibility
The owner must be able to view pending invitations for an application after they are created, through a dedicated `Invitations` list or section in the same member-management feature area.

### Invitation tracking data
Pending invitations must expose enough information to understand their current state and support follow-up actions. At minimum, the view needs to make invitation identity, assigned role, and lifecycle state understandable to the owner.

### Registered-before-acceptance state
If the invited person creates a platform account before accepting the invitation, the invitation record may expose that the person is now registered. That must remain an invitation-scoped state in the dedicated `Invitations` surface rather than becoming an active member row automatically. Local documentation must preserve the distinction between `Registered User` as a platform-account concept and active application membership as an access-grant concept.

## Acceptance Criteria
- The dashboard lists email, application role, and invitation date.
- The dashboard visually distinguishes active and expired invitations.
- Pagination is enabled when multiple invites exist.
- Once an invited member accepts the invitation, the entry is removed from the `Invitations` surface and appears under `Members`.
- If the invited person has registered but not accepted yet, the record remains in the `Invitations` surface and is not treated as active membership.

## Edge Cases and Failure Cases
- No pending invitations.
- Mixed active and expired invitations.
- Invited person creates a platform account before accepting the invitation.
- Very large invite list requiring pagination.

## Open Questions
### APIM-12767-OQ-1
- Question: Is a dedicated pending-invites dashboard required, or should this state be shown in the main members list?
- Answer: `APIM-10345-decision-01` was agreed on `2025-03-18`. The first delivery must expose invitations through a dedicated `Invitations` list or section rather than inline in the `Members` table.
- Status: Resolved

### APIM-12767-OQ-2
- Question: Is resend in scope for the first delivery?
- Answer: Jira now defines resend through the dedicated story [APIM-11535](https://gravitee.atlassian.net/browse/APIM-11535), so resend should not be treated as part of this base dashboard story.
- Status: Resolved

## Test Considerations
- Verify active-versus-expired rendering.
- Verify pagination and sorting behavior.
- Verify empty-state behavior.
