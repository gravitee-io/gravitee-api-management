# APIM-11532 Delete Registered Member

## Jira Story Reference
- Story: [APIM-11532](https://gravitee.atlassian.net/browse/APIM-11532)

## Relationship to the Parent Epic
- Parent Epic: [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345)
- This story covers removal of an active registered member from application access.

## Design References
### Screen Status
- The current exports show row-level delete actions on member rows, but they do not show the final confirmation dialog or the post-delete state.
- Observed mismatch: the design exports confirm the presence of the action entry point, but not the confirmation copy or the post-removal access-loss messaging.

## Functional Requirements
### Active-member removal
The application owner must be able to delete a registered member from the application access so that an unintended or no-longer-needed member can be removed.

### Access revocation effect
Once the member is removed, that user must no longer be able to see the application in the portal.

## Acceptance Criteria
- Clicking the delete action displays a confirmation message.
- Confirming the action removes the registered member from the application.
- Once removed, that user can no longer see the application in the portal.

## Edge Cases and Failure Cases
- The owner attempts to delete a protected member such as the primary owner.
- The member is removed concurrently by another administrator.
- The UI removes the row but backend access revocation fails.

## Open Questions
### APIM-11532-OQ-1
- Question: Which active members are protected from deletion in the final UX beyond the primary owner rules?
- Answer: Jira confirms the delete flow exists, but it does not fully specify the protected-member matrix.
- Status: Open

## Test Considerations
- Verify confirmation flow and row removal.
- Verify that removed users lose visibility to the application.
- Verify protected-member handling.
