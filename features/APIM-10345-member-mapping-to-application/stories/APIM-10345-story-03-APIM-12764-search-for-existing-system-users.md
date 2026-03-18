# APIM-12764 Search for Existing System Users

## Jira Story Reference
- Story: [APIM-12764](https://gravitee.atlassian.net/browse/APIM-12764)

## Relationship to the Parent Epic
- Parent Epic: [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345)
- This story defines the lookup flow used before adding registered users to an application.

## Design References
### Screen 01 `../screens/Member - Add Dropdown.png`
![APIM-12764 entry point](../screens/Member%20-%20Add%20Dropdown.png)
- Coverage: this screen shows the entry point from which the registered-user search flow is launched.

### Screen 02 `../screens/Member - Add Dropdown - User Search Modal.png`
![APIM-12764 modal screen](../screens/Member%20-%20Add%20Dropdown%20-%20User%20Search%20Modal.png)
- Coverage: this screen represents the base user-search modal for finding existing platform users.
- Observed mismatch: the export does not show a "No users found" state or any explicit already-added marker, both of which are required branches in `Functional Requirements` and `Acceptance Criteria`.

### Screen 03 `../screens/Member - Add Dropdown - User Search Modal-1.png`
![APIM-12764 selection state](../screens/Member%20-%20Add%20Dropdown%20-%20User%20Search%20Modal-1.png)
- Coverage: this screen supports the selected-user state after search results are returned.
- Observed mismatch: the export shows selection behavior, but it does not make clear whether matching is name-only or also includes email and user reference.

## Functional Requirements
### Eligible user lookup
The application owner must be able to search the platform user base from the add-members flow in order to find existing registered users who can be attached to the application. The latest Jira wording frames this as search by name, with result rows exposing the matching user's email.

### Duplicate prevention
The lookup flow must not allow duplicate addition of users who are already attached to the current application. The current Jira wording says those users must still be shown, but explicitly marked as `Already Added`.

## Acceptance Criteria
- Search triggers only after at least 1 character is entered.
- Results display the user's email id.
- A "No users found" message is shown when nothing matches.
- Users already in the application are shown as `Already Added`.

## Edge Cases and Failure Cases
- Query returns zero results.
- Query returns users already linked to the application.
- Search service errors or timeouts.

## Open Questions
### APIM-12764-OQ-1
- Question: Should search also match by email and user reference, or name only?
- Answer: The current Jira wording explicitly describes search by name. Broader matching by email or user reference is not yet confirmed.
- Status: Resolved

### APIM-12764-OQ-2
- Question: Should already-added users be hidden or shown as disabled entries?
- Answer: Jira now says already-added users must be shown as `Already Added`, so they should remain visible rather than being hidden.
- Status: Resolved

## Test Considerations
- Verify debounce or threshold behavior at 0 and 1 character.
- Verify duplicate-prevention behavior.
- Verify error and empty-result handling.
