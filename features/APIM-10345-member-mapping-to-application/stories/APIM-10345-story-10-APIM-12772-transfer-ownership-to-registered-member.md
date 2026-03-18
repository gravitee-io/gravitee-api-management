# APIM-12772 Transfer Ownership to Registered Member

## Jira Story Reference
- Story: [APIM-12772](https://gravitee.atlassian.net/browse/APIM-12772)

## Relationship to the Parent Epic
- Parent Epic: [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345)
- This story covers the highest-risk administrative action in the feature.

## Design References
### Screen 01 `../screens/Member - Populated list-1.png`
![APIM-12772 entry point](../screens/Member%20-%20Populated%20list-1.png)
- Coverage: this is the strongest available reference for the `Transfer Ownership` entry point on a registered-member list.
- Observed mismatch: the export does not show the required explicit confirmation step or target-selection flow.

### Screen 02 `../screens/Member - Populated list.png`
![APIM-12772 supporting screen 1](../screens/Member%20-%20Populated%20list.png)
- Coverage: this supporting screen confirms that the entry point can coexist with a mixed members-and-invitations table state.
- Observed mismatch: the screen mixes invited rows into the view even though the transfer target must be a registered member, so eligibility is not visually isolated here.

### Screen 03 `../screens/Member - Populated list-2.png`
![APIM-12772 supporting screen 2](../screens/Member%20-%20Populated%20list-2.png)
- Coverage: this supporting screen keeps the same entry-point pattern in another populated-table variant.
- Observed mismatch: there is still no dedicated transfer dialog, confirmation UX, or post-transfer state shown in the exported set.

## Functional Requirements
### Transfer eligibility
Only the current application owner or creator may initiate ownership transfer. The transfer target must already be a registered member of the application.

### Atomic ownership handoff
The transfer operation must be atomic so the application always has exactly one owner during and after the change. No intermediate state may leave the application without an owner.

## Acceptance Criteria
- The transfer-ownership option is visible only to the current owner.
- The recipient is an existing registered member.
- The user must complete an explicit confirmation step before transfer.
- After transfer, the new owner has full application-administration rights.
- The application is never left without an owner.

## Edge Cases and Failure Cases
- Attempting to transfer ownership to a non-member or invited-only user.
- Concurrent owner changes by multiple sessions.
- Transfer succeeds but fallback role for previous owner is unclear.

## Open Questions
### APIM-12772-OQ-1
- Question: What exact confirmation UX is approved: typed keyword, password, or design-led alternative?
- Answer: Jira mentions typed confirmation or password, but the current exported screens do not include a confirmation dialog.
- Status: Open

### APIM-12772-OQ-2
- Question: What role does the previous owner keep after a successful transfer?
- Answer: TBD
- Status: Open

## Test Considerations
- Verify visibility restrictions and target validation.
- Verify atomicity and rollback behavior on backend failure.
- Verify permissions of both new and previous owners after transfer.
