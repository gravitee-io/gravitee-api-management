# APIM-12774 Finalize API Mapping on App Creation

## Jira Story Reference
- Story: [APIM-12774](https://gravitee.atlassian.net/browse/APIM-12774)

## Relationship to the Parent Epic
- Parent Epic: [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345)
- This story is currently linked to the Epic in Jira, but it remains outside the local member-mapping slice documented in this directory because its scope belongs to application creation rather than member mapping.

## Design References
### External design baseline
- Figma reference from [APIM-11540](https://gravitee.atlassian.net/browse/APIM-11540): <https://www.figma.com/design/eAS6p0kITPnEI0VPbmATi3/Developer-Portal?node-id=35471-46251&t=km886KcJVeA9B9nv-0>
- Coverage: this is the current application-creation design baseline linked from Jira.
- Observed mismatch: there is no local export for the final create-and-map step, so the design does not explicitly confirm success, partial failure, or rollback behavior described in `Functional Requirements`.

### Screen Status
- No exported screen for this application-creation step exists yet in `../screens/`.
- Observed mismatch: this story is still aligned to an external application-creation baseline rather than the local member-management exports used by the rest of the Epic.

## Functional Requirements
### Combined create-and-map flow
Creating an application can persist API mappings together with application details as one end-user flow.

### Post-create visibility
Successful mappings must be visible after creation on the application details page so the result of the creation flow is immediately understandable.

## Acceptance Criteria
- Clicking "Create Application" saves the application details and API mappings together.
- If API mapping fails, an error is displayed.
- Successfully mapped APIs are shown on the application details page after creation.

## Edge Cases and Failure Cases
- Application creation succeeds but API mapping fails.
- Partial mapping success leaves an inconsistent application state.
- Retry behavior is not defined after mapping failure.

## Open Questions
### APIM-12774-OQ-1
- Question: Is this story intentionally part of [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345), or should it be moved to application-creation scope?
- Answer: For the local documentation in this Epic directory, it stays out of scope. It remains linked in Jira, but its content aligns with application creation rather than member mapping.
- Status: Resolved

### APIM-12774-OQ-2
- Question: Is atomic create-plus-map behavior required, or can the mapping step fail independently?
- Answer: Jira mentions simultaneous save behavior, but the exact transactional requirement is not fully defined.
- Status: Open

## Test Considerations
- Verify success and failure combinations for app creation and mapping.
- Verify mapped APIs appear after creation.
- Verify user-facing error behavior for mapping failures.
