# APIM-11535 Resend Invitation

## Jira Story Reference
- Story: [APIM-11535](https://gravitee.atlassian.net/browse/APIM-11535)

## Relationship to the Parent Epic
- Parent Epic: [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345)
- This story covers resending a pending invitation when the original invite was lost or expired.

## Design References
### Screen Status
- No local export currently shows a dedicated resend action or the post-resend confirmation state.
- Observed mismatch: the invitation list exports show invitation rows and actions generally, but they do not explicitly evidence a resend control.

## Functional Requirements
### Resend flow
The application owner must be able to resend an invitation to an invited user from the invitations surface.

### Expiration refresh
Resending the invitation must refresh the 7-day expiration timer and trigger a new invitation email.

## Acceptance Criteria
- A resend action is available for eligible invitation entries.
- Triggering resend refreshes the 7-day expiration timer.
- Triggering resend sends a new email to the invited user.

## Edge Cases and Failure Cases
- Resend is attempted on an invitation that is no longer eligible for resend.
- The expiration is refreshed but email dispatch fails.
- Multiple resend attempts are triggered in quick succession.

## Open Questions
### APIM-11535-OQ-1
- Question: Is resend allowed only for expired invites, for pending invites, or for both?
- Answer: Jira confirms the resend capability and the timer refresh, but it does not fully define the eligibility matrix.
- Status: Open

## Test Considerations
- Verify resend action availability rules.
- Verify expiration refresh behavior.
- Verify that a new invitation email is dispatched.
