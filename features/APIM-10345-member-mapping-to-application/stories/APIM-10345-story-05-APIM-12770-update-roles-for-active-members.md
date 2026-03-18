# APIM-12770 Update Roles for Active Members

## Jira Story Reference
- Story: [APIM-12770](https://gravitee.atlassian.net/browse/APIM-12770)

## Relationship to the Parent Epic
- Parent Epic: [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345)
- This story covers role changes for already active members.

## Design References
### Screen 01 `../screens/Member - Populated list-1.png`
![APIM-12770 primary screen](../screens/Member%20-%20Populated%20list-1.png)
- Coverage: this screen is the cleanest reference for active-member rows and row-level actions.
- Observed mismatch: the export does not show the confirmation step required before persisting a role change.

### Screen 02 `../screens/Member - Populated list.png`
![APIM-12770 supporting screen](../screens/Member%20-%20Populated%20list.png)
- Coverage: this supporting screen confirms that action controls also appear in a mixed table state.
- Observed mismatch: the export mixes invited rows into the table, while this story is specifically about active-member role changes, and it still does not show a dedicated role-edit dialog.

## Functional Requirements
### Role maintenance for active members
The owner must be able to change the role of an active application member from the members management surface.

### Protected-member handling
Protected members such as the creator or current owner may require special handling or be exempt from certain role changes. The implementation must preserve those protection rules once the final product behavior is confirmed.

## Acceptance Criteria
- A role-change control is available for each eligible member.
- The system asks for confirmation before persisting the new role.
- The changed permissions take effect immediately without requiring the target user to log out.

## Edge Cases and Failure Cases
- Attempting to change the role of the current owner or creator.
- Updating to the same role value.
- Backend failure after the UI opens the confirmation step.

## Open Questions
### APIM-12770-OQ-1
- Question: Which members are exempt from role changes in the final design?
- Answer: Jira explicitly mentions the application creator as a possible exception, but the final rule set is not yet confirmed.
- Status: Open

### APIM-12770-OQ-2
- Question: What is the mechanism for "immediate" permission refresh in existing sessions?
- Answer: TBD
- Status: Open

## Test Considerations
- Verify eligible versus protected members.
- Verify confirmation and cancel paths.
- Verify the updated role is reflected in both UI and authorization behavior.
