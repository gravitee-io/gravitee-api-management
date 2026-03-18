# APIM-11538 Search among Invited Members

## Jira Story Reference
- Story: [APIM-11538](https://gravitee.atlassian.net/browse/APIM-11538)

## Relationship to the Parent Epic
- Parent Epic: [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345)
- This story defines search behavior inside the dedicated invitations surface.

## Design References
### Screen Status
- No local export currently shows the invitations search input or an explicit filtered-results state for invited members.
- Observed mismatch: the available invitation-list exports help confirm row structure, but not the dedicated search interaction or the filtered empty state.

## Functional Requirements
### Search entry point
The invitations area must support searching among invited members without leaving the invitations view. Search is an enhancement of the invitation list rather than a separate feature surface.

### Search scope
The search must start only after at least one character is entered and must search by email id only.

### Search result rendering
Search results must reuse the same invitations table structure so the user can filter invitation entries in place without changing view context.

## Acceptance Criteria
- A table view displays invitation entries with `Email`, `Role`, and `Actions`.
- A search input is available above the invitations list.
- At least one character must be entered before filtered data is displayed.
- Invitation entries are searched by email id only.
- An empty state is displayed when no invitation entries match the provided search term.

## Edge Cases and Failure Cases
- No results for the entered email fragment.
- Search input is cleared after a filtered result set is displayed.
- The invitations list contains a mix of pending and expired rows while filtering.

## Open Questions
### APIM-11538-OQ-1
- Question: Should the invitations search also match invitation status or role in addition to email?
- Answer: Jira currently restricts the search to email id only.
- Status: Resolved

## Test Considerations
- Verify threshold behavior at 0 and 1 character.
- Verify filtering by email only.
- Verify empty-state behavior for unmatched invitation entries.
