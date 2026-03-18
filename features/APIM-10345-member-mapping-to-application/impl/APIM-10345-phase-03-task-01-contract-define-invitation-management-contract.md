# APIM-10345 Phase 03 Task 01 Contract Define Invitation Management Contract

## Task Metadata
- Epic: `APIM-10345 Member Mapping to Application`
- Phase: `03 Invitation Lifecycle Management`
- Task number: `01`
- Area: `contract`
- Suggested filename: `APIM-10345-phase-03-task-01-contract-define-invitation-management-contract.md`

## Goal
- Define the invitation-management contract for creating invites, listing pending invites, searching invites, resending invites, updating invited roles, deleting invites, and completing invitation acceptance in a dedicated invitations surface.

## Related Requirements and Stories
- Epic requirements: [../APIM-10345-requirements.md](../APIM-10345-requirements.md)
- Related stories:
- [APIM-12766 Invite New User via Email](../stories/APIM-10345-story-06-APIM-12766-invite-new-user-via-email.md)
- [APIM-12767 View Pending Invitation Dashboard](../stories/APIM-10345-story-07-APIM-12767-view-pending-invitation-dashboard.md)
- [APIM-11538 Search among Invited Members](../stories/APIM-10345-story-17-APIM-11538-search-among-invited-members.md)
- [APIM-12768 Delete Pending Invitation](../stories/APIM-10345-story-08-APIM-12768-delete-pending-invitation.md)
- [APIM-12771 Update Roles for Invited Members](../stories/APIM-10345-story-09-APIM-12771-update-roles-for-invited-members.md)
- [APIM-11535 Resend Invitation](../stories/APIM-10345-story-16-APIM-11535-resend-invitation.md)
- [APIM-11531 Acceptance of Invitation](../stories/APIM-10345-story-14-APIM-11531-acceptance-of-invitation.md)
- If the task is not tied to a single story, explain which Epic requirement or technical foundation it supports:
- This task defines the shared lifecycle model for invited members.

## Concrete Implementation Changes
- Define invite creation contract with one-or-more email inputs, role, and expiration semantics.
- Define a dedicated invitations-list contract for pending rows, including invitation-lifecycle states such as `Pending` and `Expired`.
- Define invitation search, invited-role update, resend, delete, and acceptance contracts.
- Make it explicit that a platform account appearing after invitation issuance does not change the record into active membership before acceptance.
- Keep invitation retrieval separate from the members collection per `APIM-10345-decision-01`.

## Planned File Changes
### Create
- `None`

### Update
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/test/resources/portal-openapi.yaml`

### Delete
- `None`

## API Contract Changes
### List and search pending invitations
- HTTP method: `GET`
- Path: `/applications/{applicationId}/invitations`
- Path parameters:
- `applicationId`: target application identifier
- Query parameters:
- `page`: 1-based page number
- `size`: page size
- `q`: optional search phrase applied to invited email
- Request body:
- `None`
- Expected responses:
- `200 OK`: paginated invitations response with `data` rows containing invitation id, invited email, role, status, and available row actions
- `403 Forbidden`: caller cannot read invitations for the application
- `404 Not Found`: application does not exist or is not visible to the caller

### Create one or more invitations
- HTTP method: `POST`
- Path: `/applications/{applicationId}/invitations`
- Path parameters:
- `applicationId`: target application identifier
- Query parameters:
- `None`
- Request body:
- invitation-create payload with one-or-more email values, selected role, and optional notification flag when that behavior is confirmed
- Expected responses:
- `201 Created`: created invitation records or an aggregate creation response for the submitted emails
- `400 Bad Request`: invalid email input or malformed payload
- `403 Forbidden`: caller cannot invite users
- `409 Conflict`: duplicate invitation or conflicting active membership when the contract chooses to surface that explicitly

### Update invitation role
- HTTP method: `PUT`
- Path: `/applications/{applicationId}/invitations/{invitationId}`
- Path parameters:
- `applicationId`: target application identifier
- `invitationId`: target invitation identifier
- Query parameters:
- `None`
- Request body:
- payload carrying the updated invited-member role
- Expected responses:
- `200 OK`: updated invitation row
- `403 Forbidden`: caller cannot update invitations
- `404 Not Found`: invitation is missing or no longer belongs to the current application

### Resend invitation
- HTTP method: `POST`
- Path: `/applications/{applicationId}/invitations/{invitationId}/_resend`
- Path parameters:
- `applicationId`: target application identifier
- `invitationId`: target invitation identifier
- Query parameters:
- `None`
- Request body:
- `None`
- Expected responses:
- `204 No Content`: resend accepted and invitation validity refreshed
- `403 Forbidden`: caller cannot resend invitations
- `404 Not Found`: invitation is missing or no longer belongs to the current application

### Delete invitation
- HTTP method: `DELETE`
- Path: `/applications/{applicationId}/invitations/{invitationId}`
- Path parameters:
- `applicationId`: target application identifier
- `invitationId`: target invitation identifier
- Query parameters:
- `None`
- Request body:
- `None`
- Expected responses:
- `204 No Content`: invitation deleted
- `403 Forbidden`: caller cannot delete invitations
- `404 Not Found`: invitation is missing or no longer belongs to the current application

### Accept invitation
- HTTP method: `POST`
- Path: `/invitations/_accept`
- Path parameters:
- `None`
- Query parameters:
- `None`
- Request body:
- acceptance payload carrying the invitation token and any required registration context
- Expected responses:
- `204 No Content`: invitation accepted, membership created, invitation removed
- `400 Bad Request`: malformed or already-used token
- `404 Not Found`: invitation token no longer resolves to an active invitation

## Impacted Modules
- Portal OpenAPI contract
- `gravitee-apim-rest-api`

## Validation
- Automated checks:
- contract validation for invitation endpoints and schemas
- Manual checks:
- verify the contract can represent the screen states shown for invited members without collapsing invitation state into active membership when a platform account exists before acceptance

## Open Questions or Blockers
### APIM-10345-TASK-OQ-1
- Question: Does the first version require a dedicated pending-invites endpoint, or can the unified members endpoint represent invited rows well enough?
- Answer: `APIM-10345-decision-01` was agreed on `2025-03-18`. The first version should use a dedicated invitations read contract rather than a unified members endpoint.
- Status: `Resolved`
