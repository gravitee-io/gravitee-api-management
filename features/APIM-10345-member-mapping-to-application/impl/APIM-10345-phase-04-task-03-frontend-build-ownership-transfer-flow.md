# APIM-10345 Phase 04 Task 03 Frontend Build Ownership Transfer Flow

## Task Metadata
- Epic: `APIM-10345 Member Mapping to Application`
- Phase: `04 Ownership Transfer and Feature Toggles`
- Task number: `03`
- Area: `frontend`
- Suggested filename: `APIM-10345-phase-04-task-03-frontend-build-ownership-transfer-flow.md`

## Goal
- Build the ownership-transfer UI in the portal and wire it to the phase 04 backend behavior.

## Related Requirements and Stories
- Epic requirements: [../APIM-10345-requirements.md](../APIM-10345-requirements.md)
- Related stories:
- [APIM-12772 Transfer Ownership to Registered Member](../stories/APIM-10345-story-10-APIM-12772-transfer-ownership-to-registered-member.md)
- If the task is not tied to a single story, explain which Epic requirement or technical foundation it supports:
- This task delivers the user-facing portion of the highest-risk portal action in the Epic.

## Concrete Implementation Changes
- Add transfer-ownership entry-point behavior to the members view.
- Build the transfer dialog or confirmation flow once product confirmation is available.
- Restrict visibility and enabled state based on feature-toggle state and user eligibility.
- Refresh portal state after successful ownership transfer.

## Planned File Changes
### Create
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-transfer-ownership-dialog/application-transfer-ownership-dialog.component.ts`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-transfer-ownership-dialog/application-transfer-ownership-dialog.component.html`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-transfer-ownership-dialog/application-transfer-ownership-dialog.component.scss`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-transfer-ownership-dialog/application-transfer-ownership-dialog.component.spec.ts`

### Update
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-tab-members/application-tab-members.component.ts`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-tab-members/application-tab-members.component.html`
- `gravitee-apim-portal-webui-next/src/services/application.service.ts`

### Delete
- `None`

## API Contract Changes
- `None`

## Impacted Modules
- `gravitee-apim-portal-webui-next`
- `gravitee-apim-webui-libs`

## Validation
- Automated checks:
- frontend tests for visibility rules, transfer dialog behavior, and post-transfer state updates
- Manual checks:
- verify only eligible users can start the flow and that the portal reflects the new owner after success

## Open Questions or Blockers
### APIM-10345-TASK-OQ-1
- Question: What exact confirmation UI should be implemented for ownership transfer?
- Answer: `TBD`
- Status: `Open`
