# APIM-10345 Story Refinement

## Meeting Date
- `2026-03-23`

## Agenda
### `APIM-11539` Search among existing application members
- Suggestion: extend the story description from `As an Application Owner, I want to view search among existing members of the application` to clarify that the purpose of search is to filter the members table.
- Proposed wording: `As an Application Owner, I want to search among existing application members so that I can quickly filter the members table and find the member I am looking for.`
- Please remove the question `Is it possible to merge invited members and existing members?`, as this has already been decided elsewhere.
- Please clarify where the search results should be presented.
- Please clarify what the expected empty state should look like, since it does not seem to be covered by the current mockups.
- Please clarify whether search should start from the first typed character or only after a minimum number of characters has been entered.
- Please confirm that search should match against the `Name` field and that the entered phrase may match any part of the field value, not only the prefix.
- Suggestion: the search should be executed with a small debounce delay to avoid triggering a request on every keystroke.

### `APIM-12764` Search for existing system users
- Suggestion: it would be helpful for the story to reference a specific mockup explicitly.
- Please clarify how this story maps to the current mockups.
- Please clarify where this search is supposed to live in the UX flow.
- Is this search part of the add-member modal flow, or is it expected to appear somewhere else?
- Please clarify how this story relates to `APIM-11539` and whether these are intended to be two distinct search flows.
- Please clarify the expected behavior for users who are already members of the current application, in particular whether they must be filtered out or shown as `Already Added`.

### `APIM-12765` Add registered user to application
- Please clarify whether this story is actually reflected in the current mockups.
- Please confirm whether the `toast` feedback was explicitly validated with UX.
- Please clarify whether the acceptance criterion `The added user(s) must default to the lowest permission level (e.g., User) unless otherwise specified.` is still valid, since the current screen seems to show a modal where the role is selected explicitly.

## Action points
- None yet.


## Follow-Up Actions
- Update the affected story files after the meeting if scope or wording changes.
