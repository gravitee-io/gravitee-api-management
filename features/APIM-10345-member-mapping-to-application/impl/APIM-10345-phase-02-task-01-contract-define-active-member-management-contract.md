# APIM-10345 Phase 02 Task 01 Contract Define Active Member Management Contract

## Task Metadata
- Epic: `APIM-10345 Member Mapping to Application`
- Phase: `02 Registered Member Management`
- Task number: `01`
- Area: `contract`
- Suggested filename: `APIM-10345-phase-02-task-01-contract-define-active-member-management-contract.md`

## Goal
- Define the portal-facing contract for searching eligible platform users, adding registered members, updating active-member roles, and deleting active members.

## Related Requirements and Stories
- Epic requirements: [../APIM-10345-requirements.md](../APIM-10345-requirements.md)
- Related stories:
- [APIM-12764 Search for Existing System Users](../stories/APIM-10345-story-03-APIM-12764-search-for-existing-system-users.md)
- [APIM-12765 Add Registered User to Application](../stories/APIM-10345-story-04-APIM-12765-add-registered-user-to-application.md)
- [APIM-12770 Update Roles for Active Members](../stories/APIM-10345-story-05-APIM-12770-update-roles-for-active-members.md)
- [APIM-11532 Delete Registered Member](../stories/APIM-10345-story-15-APIM-11532-delete-registered-member.md)
- If the task is not tied to a single story, explain which Epic requirement or technical foundation it supports:
- This task consolidates the mutation and lookup contracts needed for active-member management into one coherent backend/frontend interface.

## Concrete Implementation Changes
- Define lookup contract for eligible platform users.
- Define lookup response fields needed by the search modal, including email display and `Already Added` duplicate markers.
- Define add-member request and response models, including role assignment, lowest-role defaults, and duplicate-prevention expectations.
- Define active-member role update and delete-member contracts together with any returned refresh payloads or status responses.

## Planned File Changes
### Create
- `None`

### Update
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/test/resources/portal-openapi.yaml`

### Delete
- `None`

## API Contract Changes
### Search eligible registered users for one application
- HTTP method: `POST`
- Path: `/applications/{applicationId}/members/_search/users`
- Path parameters:
- `applicationId`: target application identifier
- Query parameters:
- `page`: 1-based page number
- `size`: page size
- `q`: search phrase applied to user name and email fields
- Request body:
- `None`
- Expected responses:
- `200 OK`: paginated lookup response with `data` rows containing user identity, email, and an `alreadyAdded` marker when the user is already a member of the current application
- `403 Forbidden`: caller cannot manage application members
- `404 Not Found`: application does not exist or is not visible to the caller

### Add registered member to application
- HTTP method: `POST`
- Path: `/applications/{applicationId}/members`
- Path parameters:
- `applicationId`: target application identifier
- Query parameters:
- `None`
- Request body:
- `MemberInput`-compatible payload with target user identifier and selected application role
- Expected responses:
- `201 Created`: created member row matching the members table contract
- `400 Bad Request`: invalid role or malformed request
- `403 Forbidden`: caller cannot add members
- `409 Conflict`: target user is already a member when duplicate protection is enforced

### Update active member role
- HTTP method: `PUT`
- Path: `/applications/{applicationId}/members/{memberId}`
- Path parameters:
- `applicationId`: target application identifier
- `memberId`: target member identifier
- Query parameters:
- `None`
- Request body:
- `MemberInput`-compatible payload carrying the updated application role
- Expected responses:
- `200 OK`: updated member row
- `400 Bad Request`: malformed request or invalid role transition
- `403 Forbidden`: caller cannot update members
- `404 Not Found`: application or member does not exist in the current scope

### Delete active member
- HTTP method: `DELETE`
- Path: `/applications/{applicationId}/members/{memberId}`
- Path parameters:
- `applicationId`: target application identifier
- `memberId`: target member identifier
- Query parameters:
- `None`
- Request body:
- `None`
- Expected responses:
- `204 No Content`: member removed
- `403 Forbidden`: caller cannot delete members
- `404 Not Found`: application or member does not exist in the current scope

## Impacted Modules
- Portal OpenAPI contract
- `gravitee-apim-rest-api`

## Validation
- Automated checks:
- portal OpenAPI validation or generation checks
- Manual checks:
- verify that lookup, add, and update contracts can support the modal flows shown in the exported screens

## Open Questions or Blockers
### APIM-10345-TASK-OQ-1
- Question: Should the add-member contract support bulk add in the first version, or only a single member per request?
- Answer: The local requirements currently allow adding selected registered users through the same member-management flow, but Jira does not yet make the bulk shape as explicit here as it does for email invitations. Keep the contract extensible enough for multi-select without blocking an initial single-request implementation shape.
- Status: `Open`
