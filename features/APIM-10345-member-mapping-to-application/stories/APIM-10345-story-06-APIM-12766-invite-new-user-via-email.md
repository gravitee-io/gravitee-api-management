# APIM-12766 Invite New User via Email

## Jira Story Reference
- Story: [APIM-12766](https://gravitee.atlassian.net/browse/APIM-12766)

## Relationship to the Parent Epic
- Parent Epic: [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345)
- This story covers onboarding users who do not yet have a platform account.

## Design References
### Screen 01 `../screens/Member - Add Dropdown.png`
![APIM-12766 entry point](../screens/Member%20-%20Add%20Dropdown.png)
- Coverage: this screen shows the add-members menu entry point for the invite-by-email branch.

### Screen 02 `../screens/Member - Add Dropdown - Add Email Modal.png`
![APIM-12766 modal screen](../screens/Member%20-%20Add%20Dropdown%20-%20Add%20Email%20Modal.png)
- Coverage: this screen represents the empty invite-email modal before user input.
- Observed mismatch: the export does not show the registered-user redirect branch for an email that already exists in the platform.

### Screen 03 `../screens/Member - Add Dropdown - Add Email Modal-1.png`
![APIM-12766 filled modal](../screens/Member%20-%20Add%20Dropdown%20-%20Add%20Email%20Modal-1.png)
- Coverage: this screen confirms the email and role-entry state before invitation submission.
- Observed mismatch: the export does not show token-expiration details, email-delivery feedback, or any bulk-invite variation, so those requirements remain non-visual.

## Functional Requirements
### Invitation entry
The owner must be able to invite one or more non-registered email addresses to the application from the members flow.

### Invitation payload and lifecycle
The invite flow must assign an application role as part of the invitation and generate a secure expiring token that can later be used to complete onboarding.

## Acceptance Criteria
- The system validates the email format.
- If the email already exists, the flow redirects to the registered-user add flow.
- The inviter selects an application role, with `USER` as the initial assumption for the default.
- The system generates a secure invitation token with an expiration.
- An email is sent with the application name and a clickable registration link.
- Multiple email IDs can be entered in one go.
- The invitation behaviour remains otherwise unchanged.

## Edge Cases and Failure Cases
- Invalid email format.
- Existing registered user entered in the invite form.
- Email dispatch failure after token creation.

## Open Questions
### APIM-12766-OQ-1
- Question: What is the exact token lifetime in days?
- Answer: Jira states an `N-day` expiration but does not define the value.
- Status: Open

### APIM-12766-OQ-2
- Question: Is bulk invite by multiple email addresses part of this story or a later enhancement?
- Answer: Jira now explicitly says multiple email IDs can be entered in one go, so bulk invite is part of this story even though the current export does not yet evidence it visually.
- Status: Resolved

## Test Considerations
- Verify validation for malformed and existing emails.
- Verify token creation and expiration behavior.
- Verify that successful invite creation is reflected in pending-invitation views.
