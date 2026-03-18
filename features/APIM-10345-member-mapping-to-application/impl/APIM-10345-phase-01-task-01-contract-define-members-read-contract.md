# APIM-10345 Phase 01 Task 01 Contract Define Members Read Contract

## Task Metadata
- Epic: `APIM-10345 Member Mapping to Application`
- Phase: `01 Members Read Path Foundation`
- Task number: `01`
- Area: `contract`
- Suggested filename: `APIM-10345-phase-01-task-01-contract-define-members-read-contract.md`

## Goal
- Define the additive portal-facing contract for retrieving and searching application members, with always-present pagination, without breaking existing consumers.

## Related Requirements and Stories
- Epic requirements: [../APIM-10345-requirements.md](../APIM-10345-requirements.md)
- Related stories:
- [APIM-12280 View existing application members](../stories/APIM-10345-story-01-APIM-12280-view-existing-application-members.md)
- [APIM-11539 Search among existing members](../stories/APIM-10345-story-02-APIM-11539-search-among-existing-members.md)
- If the task is not tied to a single story, explain which Epic requirement or technical foundation it supports:
- This task creates the shared contract foundation that both the members table and in-page search rely on.

## Concrete Implementation Changes
- Add or refine portal OpenAPI paths and schemas for listing application members and searching that same member set.
- Define response fields needed by the table: `Name` payload, `Email`, `Role`, and available row actions.
- Make the contract explicit about always-present pagination controls and search parameters, including the one-character threshold used by the members search flow.

## Planned File Changes
### Create
- `None`

### Update
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/test/resources/portal-openapi.yaml`

### Delete
- `None`

## API Contract Changes
### List and search application members
- HTTP method: `GET`
- Path: `/applications/{applicationId}/members`
- Path parameters:
- `applicationId`: target application identifier
- Query parameters:
- `page`: 1-based page number
- `size`: page size
- `q`: optional search phrase applied to the member `Name` field
- Request body:
- `None`
- Expected responses:
- `200 OK`: `MembersResponse` with `data` containing member rows for the current application and pagination metadata in `links`
- `400 Bad Request`: invalid pagination input
- `403 Forbidden`: caller cannot read application members
- `404 Not Found`: application does not exist or is not visible to the caller

## Impacted Modules
- `gravitee-apim-rest-api`
- Portal OpenAPI contract

## Validation
- Automated checks:
- contract generation or validation checks for the updated portal OpenAPI definition
- Manual checks:
- review that the contract supports both base list and search use cases without assuming invited-member-only fields

## Open Questions or Blockers
### APIM-10345-TASK-OQ-1
- Question: Should the read contract model invited members in the same collection from phase 01, or should invited-member fields be introduced only in phase 03?
- Answer: Per `APIM-10345-decision-01`, phase 01 remains active-member-only. Invitation-specific fields belong to the separate phase 03 invitations contract rather than the members collection.
- Status: `Resolved`
