# APIM-12773 API Selection Wizard

## Jira Story Reference
- Story: [APIM-12773](https://gravitee.atlassian.net/browse/APIM-12773)

## Relationship to the Parent Epic
- Parent Epic: [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345)
- This story is currently linked to the Epic in Jira, but it remains outside the local member-mapping slice documented in this directory because its scope belongs to application creation rather than member mapping.

## Design References
### External design baseline
- Figma reference from [APIM-11540](https://gravitee.atlassian.net/browse/APIM-11540): <https://www.figma.com/design/eAS6p0kITPnEI0VPbmATi3/Developer-Portal?node-id=35471-46251&t=km886KcJVeA9B9nv-0>
- Coverage: this is the current application-creation design baseline linked from Jira.
- Observed mismatch: there is no local export for the exact `API Catalog` step, so the concrete step layout, checkbox behavior, and empty-state handling are not evidenced in the Epic screen set.

### Screen Status
- No exported screen for this application-creation step exists yet in `../screens/`.
- Observed mismatch: this story is still anchored to an external application-creation baseline rather than the local member-management exports used by the rest of the Epic.

## Functional Requirements
### API catalog step
During application creation, the owner can browse available APIs as part of a dedicated selection step.

### Multi-selection
The owner can select multiple APIs before creating the application so the created application starts with the intended initial mappings.

## Acceptance Criteria
- An "API Catalog" step is visible in the create-application flow.
- Users can select multiple APIs with checkboxes.
- Each API shows a brief description or version status.

## Edge Cases and Failure Cases
- No APIs are available for selection.
- Some APIs become unavailable during selection.
- Large API catalogs require search, filtering, or pagination that is not yet specified.

## Open Questions
### APIM-12773-OQ-1
- Question: Is this story intentionally part of [APIM-10345](https://gravitee.atlassian.net/browse/APIM-10345), or should it belong to application creation scope instead?
- Answer: For the local documentation in this Epic directory, it stays out of scope. It remains linked in Jira, but its content aligns with application creation rather than member mapping.
- Status: Resolved

### APIM-12773-OQ-2
- Question: Which design is the source of truth for this step?
- Answer: The current reference is the application-creation Figma baseline from [APIM-11540](https://gravitee.atlassian.net/browse/APIM-11540); no exported screen for this specific step is present locally.
- Status: Open

## Test Considerations
- Verify multi-select behavior and persistence across steps.
- Verify empty-catalog behavior.
- Verify selection data is passed into application creation submission.
