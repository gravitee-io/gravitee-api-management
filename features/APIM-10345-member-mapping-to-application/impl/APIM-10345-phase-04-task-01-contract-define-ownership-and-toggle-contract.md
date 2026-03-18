# APIM-10345 Phase 04 Task 01 Contract Define Ownership and Toggle Contract

## Task Metadata
- Epic: `APIM-10345 Member Mapping to Application`
- Phase: `04 Ownership Transfer and Feature Toggles`
- Task number: `01`
- Area: `contract`
- Suggested filename: `APIM-10345-phase-04-task-01-contract-define-ownership-and-toggle-contract.md`

## Goal
- Define the portal and admin-facing contracts for ownership transfer and feature toggles.

## Related Requirements and Stories
- Epic requirements: [../APIM-10345-requirements.md](../APIM-10345-requirements.md)
- Related stories:
- [APIM-12772 Transfer Ownership to Registered Member](../stories/APIM-10345-story-10-APIM-12772-transfer-ownership-to-registered-member.md)
- [APIM-12888 Administrative Toggle Controls](../stories/APIM-10345-story-11-APIM-12888-administrative-toggle-controls.md)
- If the task is not tied to a single story, explain which Epic requirement or technical foundation it supports:
- This task establishes the final high-risk administrative capabilities for the Epic.

## Concrete Implementation Changes
- Define ownership-transfer request and response payloads, including target-member validation and any fallback role semantics.
- Define feature-toggle read and write contracts for member mapping, invitation sending, and ownership transfer.
- Define any audit-related contract expectations needed for admin verification.

## Planned File Changes
### Create
- `None`

### Update
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml`
- `gravitee-apim-console-webui/src/services-ngx/application-members.service.ts`

### Delete
- `None`

## API Contract Changes
### Transfer application ownership
- HTTP method: `POST`
- Path: `/applications/{applicationId}/members/_transfer_ownership`
- Path parameters:
- `applicationId`: target application identifier
- Query parameters:
- `None`
- Request body:
- `TransferOwnershipInput`-compatible payload carrying the new primary owner identifier and the fallback role for the former owner
- Expected responses:
- `204 No Content`: ownership transferred
- `400 Bad Request`: invalid fallback role or malformed payload
- `403 Forbidden`: caller cannot transfer ownership
- `404 Not Found`: application or target member cannot be resolved

### Read member-mapping feature toggles
- HTTP method: `GET`
- Path: `TBD in this task`
- Path parameters:
- `TBD`
- Query parameters:
- `TBD`
- Request body:
- `None`
- Expected responses:
- `200 OK`: toggle state for member mapping, invitation sending, and ownership transfer

### Update member-mapping feature toggles
- HTTP method: `PUT`
- Path: `TBD in this task`
- Path parameters:
- `TBD`
- Query parameters:
- `TBD`
- Request body:
- payload carrying the enabled or disabled state for the three toggles
- Expected responses:
- `200 OK` or `204 No Content`: toggle state persisted
- `403 Forbidden`: caller cannot update toggle state

## Impacted Modules
- Portal OpenAPI contract
- admin settings contract surface

## Validation
- Automated checks:
- contract validation or generation checks
- Manual checks:
- verify that transfer and toggle contracts are sufficient for both portal and admin UI flows

## Open Questions or Blockers
### APIM-10345-TASK-OQ-1
- Question: What is the exact confirmation payload or UX contract for ownership transfer?
- Answer: `TBD`
- Status: `Open`
