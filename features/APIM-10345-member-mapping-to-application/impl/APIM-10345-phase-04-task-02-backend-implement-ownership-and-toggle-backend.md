# APIM-10345 Phase 04 Task 02 Backend Implement Ownership and Toggle Backend

## Task Metadata
- Epic: `APIM-10345 Member Mapping to Application`
- Phase: `04 Ownership Transfer and Feature Toggles`
- Task number: `02`
- Area: `backend`
- Suggested filename: `APIM-10345-phase-04-task-02-backend-implement-ownership-and-toggle-backend.md`

## Goal
- Implement backend support for ownership transfer, feature-toggle persistence, and audit logging.

## Related Requirements and Stories
- Epic requirements: [../APIM-10345-requirements.md](../APIM-10345-requirements.md)
- Related stories:
- [APIM-12772 Transfer Ownership to Registered Member](../stories/APIM-10345-story-10-APIM-12772-transfer-ownership-to-registered-member.md)
- [APIM-12888 Administrative Toggle Controls](../stories/APIM-10345-story-11-APIM-12888-administrative-toggle-controls.md)
- If the task is not tied to a single story, explain which Epic requirement or technical foundation it supports:
- This task provides the administrative backend behavior needed for final rollout control and owner reassignment.

## Concrete Implementation Changes
- Implement ownership transfer with registered-member validation and atomic owner reassignment.
- Persist feature-toggle state for member mapping, invitation sending, and ownership transfer.
- Emit audit records for toggle changes and ownership transfer where required.
- Expose toggle state to portal-facing consumers so UI guards can respond without manual restarts.

## Planned File Changes
### Create
- `None`

### Update
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/ApplicationMembersResource.java`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-management/gravitee-apim-rest-api-management-rest/src/main/java/io/gravitee/rest/api/management/rest/resource/ApplicationMembersResource.java`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/rest/api/service/impl/MembershipServiceImpl.java`

### Delete
- `None`

## API Contract Changes
- `None` beyond the contract defined in `APIM-10345 Phase 04 Task 01`.

## Impacted Modules
- `gravitee-apim-rest-api`
- persistence and audit-related backend components

## Validation
- Automated checks:
- backend tests for ownership transfer atomicity, toggle persistence, and audit logging
- Manual checks:
- verify toggles survive restart and that ownership transfer never leaves the application without an owner

## Open Questions or Blockers
### APIM-10345-TASK-OQ-1
- Question: Are feature toggles scoped per environment, organization, or installation?
- Answer: `TBD`
- Status: `Open`
