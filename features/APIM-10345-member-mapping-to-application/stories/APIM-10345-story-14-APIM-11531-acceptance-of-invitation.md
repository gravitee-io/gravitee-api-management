# APIM-11531 Acceptance of Invitation

## Jira Story Reference
- Story: [APIM-11531](https://gravitee.atlassian.net/browse/APIM-11531)

## Relationship to the Parent Epic
- Parent Epic: [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345)
- This story covers the lifecycle transition in which a pending invitation becomes active application membership after acceptance.

## Design References
### Screen Status
- No dedicated local export currently shows the acceptance flow or the post-acceptance transition.
- Observed mismatch: the available member-management exports focus on member and invitation lists, not on the acceptance journey or the exact post-acceptance state transition.

## Functional Requirements
### Invitation acceptance transition
Accepting a valid invitation must create active application membership for the invited person. After acceptance, the person must no longer be represented as a pending invitation and must instead appear in the `Members` list as an active member with the granted role.

### Registration without acceptance
If the invited person creates a platform account before accepting the invitation, that does not by itself create application membership. In that state, the system may know that the person is now a `Registered User`, but the application-access state remains invitation-based until acceptance. Local documentation must preserve the distinction between `Registered User` as a platform-account concept and `Application Membership` as an access-grant concept.

## Acceptance Criteria
- Once the invited member accepts the invitation, the person gains access to the application and appears in the `Members` list.
- Once the invitation is accepted, the pending invitation is no longer shown as an active invitation entry.
- If the invited person has registered but not accepted the invitation yet, the person remains in the `Invitations` surface and is not treated as an active member.

## Edge Cases and Failure Cases
- The invited person registers a platform account before accepting the invitation.
- The invitation is accepted after the role was updated while still pending.
- The invitation is expired or deleted before acceptance is attempted.
- The post-acceptance transition updates access correctly but leaves stale invitation data visible.

## Open Questions
### APIM-11531-OQ-1
- Question: Does the post-acceptance transition need dedicated user-facing confirmation in the portal, or is the list transition itself sufficient?
- Answer: TBD
- Status: Open

## Test Considerations
- Verify that a successful acceptance grants access and creates active membership.
- Verify that the accepted user disappears from the active invitation state and appears in the `Members` list.
- Verify that registration-before-acceptance does not grant active membership prematurely.
