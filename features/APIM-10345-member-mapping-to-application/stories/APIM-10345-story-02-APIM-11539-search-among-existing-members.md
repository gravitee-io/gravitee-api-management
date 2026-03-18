# APIM-11539 Search among existing members

## Jira Story Reference
- Story: [APIM-11539](https://gravitee.atlassian.net/browse/APIM-11539)

## Relationship to the Parent Epic
- Parent Epic: [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345)
- This story defines search behavior inside the application members experience.

## Design References
### Screen 01 `../screens/Member - Populated list-1.png`
![APIM-11539 primary screen](../screens/Member%20-%20Populated%20list-1.png)
- Coverage: this screen is the best available anchor for the in-page search field inside the members table.
- Observed mismatch: the export does not show an active filtered-results state, so it does not prove how search results should look after the query is applied.

### Screen 02 `../screens/Members - Empty.png`
![APIM-11539 empty state](../screens/Members%20-%20Empty.png)
- Coverage: this screen is the closest available reference for a no-results or no-data state.
- Observed mismatch: the export looks like a base empty-members state rather than an explicit search-no-match state, so the exact empty-search wording and behavior remain inferred.

## Functional Requirements
### Search entry point
The members area must support searching among existing application members without leaving the members view. Search is an enhancement of the default members list rather than a separate feature surface.

This story is specifically about the behavior of the search input placed above the members list. The input must refine the list in place, inside the same members view, without navigating away, opening a separate search screen, or changing the table structure. Clearing the search input must restore the default unfiltered list for the current members view. The search must start only after at least one character is entered.

### Search result rendering
Search results must reuse the same table structure as the default members list so the user is not forced into a different interpretation of the data after filtering. Per `APIM-10345-decision-01`, this story applies to the `Members` list only and does not define search behavior for the separate invitations list.

### Searchable fields
The search term must be matched against the user-visible identity and membership fields represented by the `Members` list. That means the search should cover member display name, member email, and member role.

The search scope should be defined in terms of user-visible business fields from the `Members` list, not hidden technical identifiers or backend-only metadata.

## Acceptance Criteria
- A table view displays members with name, email, role, and actions.
- A search input is available above the list and filters the list in place.
- Clearing the search input restores the unfiltered members list.
- An empty state is displayed when no members match the provided search term.

## Edge Cases and Failure Cases
- No results for the entered term.
- Search terms that match only invitations must not contaminate the members result set.
- Backend search failure must surface a recoverable error state.

## Open Questions
### APIM-11539-OQ-1
- Question: Should invited and active members be merged into one searchable list?
- Answer: `APIM-10345-decision-01` was agreed on `2025-03-18`. Search in this story remains scoped to the `Members` list, while invitation visibility and any invitation-specific filtering belong to the separate invitations surface.
- Status: Resolved

### APIM-11539-OQ-2
- Question: Is this story distinct from [APIM-12280](https://gravitee.atlassian.net/browse/APIM-12280) or should both be consolidated?
- Answer: TBD
- Status: Open

### APIM-11539-OQ-3
- Question: Does this story refer to an inline search input above the members list, or to a separate search experience?
- Answer: The current evidence points to an inline list-filtering input above the table. The story design references describe an in-page search field anchored to the members table, and the existing table-filtering pattern in `gravitee-apim-console-webui/src/management/settings/groups/group/group.component.ts` applies a `searchTerm` directly to the current table collection through `gioTableFilterCollection` in `gravitee-apim-console-webui/src/shared/components/gio-table-wrapper/gio-table-wrapper.util.ts`. That pattern supports in-place filtering of the current list and returns the full collection again when the search term is empty.
- Status: Resolved

## Test Considerations
- Verify empty-state behavior for unmatched terms.
- Verify search against display name, email, and role for the members list.
- Verify invitation-only entries are not returned by the members search path.
- Verify clearing the input restores the default list.
- Verify pagination and search interaction when more than 10 records exist.
