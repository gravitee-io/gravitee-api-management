# APIM-10345 Phase 01 Task 02 Backend Implement Members Read Path

## Task Metadata
- Epic: `APIM-10345 Member Mapping to Application`
- Phase: `01 Members Read Path Foundation`
- Task number: `02`
- Area: `backend`
- Suggested filename: `APIM-10345-phase-01-task-02-backend-implement-members-read-path.md`

## Goal
- Implement backend support for listing and searching application members through the new portal-facing contract.

## Related Requirements and Stories
- Epic requirements: [../APIM-10345-requirements.md](../APIM-10345-requirements.md)
- Related stories:
- [APIM-12280 View existing application members](../stories/APIM-10345-story-01-APIM-12280-view-existing-application-members.md)
- [APIM-11539 Search among existing members](../stories/APIM-10345-story-02-APIM-11539-search-among-existing-members.md)
- If the task is not tied to a single story, explain which Epic requirement or technical foundation it supports:
- This task provides the read capability consumed by the baseline members UI.

## Concrete Implementation Changes
- Implement portal resource and use-case wiring for listing application members.
- Support pagination and search by the fields agreed in the read contract, including always returning pagination metadata for the members view.
- Enforce portal-side authorization for reading application members.
- Map backend data into the contract structure expected by the members table.

## Planned File Changes
### Create
- `None`

### Update
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/ApplicationMembersResource.java`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/mapper/MemberMapper.java`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/test/java/io/gravitee/rest/api/portal/rest/resource/ApplicationMembersResourceTest.java`

### Delete
- `None`

## API Contract Changes
- `None` beyond the contract defined in `APIM-10345 Phase 01 Task 01`.

## Impacted Modules
- `gravitee-apim-rest-api`
- repository and service layers touched by application-member reads

## Validation
- Automated checks:
- focused backend tests for list and search behavior
- API-level tests for permissions, pagination, and no-result search
- Manual checks:
- verify populated, empty, and permission-denied responses through the portal-facing API

## Open Questions or Blockers
### APIM-10345-TASK-OQ-1
- Question: Should search semantics include only active members in phase 01, or any member type already available in storage?
- Answer: Phase 01 search is limited to active application members. Invitation records are introduced later through the separate invitation lifecycle APIs.
- Status: `Resolved`
