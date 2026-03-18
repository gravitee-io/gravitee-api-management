# APIM-10345 Phase 04 Task 04 Frontend Build Admin Toggle Controls and Rollout Guards

## Task Metadata
- Epic: `APIM-10345 Member Mapping to Application`
- Phase: `04 Ownership Transfer and Feature Toggles`
- Task number: `04`
- Area: `frontend`
- Suggested filename: `APIM-10345-phase-04-task-04-frontend-build-admin-toggle-controls-and-rollout-guards.md`

## Goal
- Build the admin-facing toggle controls and connect portal-side rollout guards to the persisted toggle state.

## Related Requirements and Stories
- Epic requirements: [../APIM-10345-requirements.md](../APIM-10345-requirements.md)
- Related stories:
- [APIM-12888 Administrative Toggle Controls](../stories/APIM-10345-story-11-APIM-12888-administrative-toggle-controls.md)
- If the task is not tied to a single story, explain which Epic requirement or technical foundation it supports:
- This task closes the rollout loop by exposing control surfaces in admin UI and consuming those controls in the portal UI.

## Concrete Implementation Changes
- Add admin controls for member-mapping, invitation-sending, and ownership-transfer toggles in the chosen management UI.
- Hide or disable portal members features when the corresponding toggles are off.
- Ensure portal-side guards react correctly to toggle state at load time and after refresh.
- Add or update documentation notes if the admin toggle surface changes the rollout workflow.

## Planned File Changes
### Create
- `None`

### Update
- `gravitee-apim-console-webui/src/management/application/details/user-group-access/members/application-general-members.component.ts`
- `gravitee-apim-console-webui/src/management/application/details/user-group-access/members/application-general-members.component.html`
- `gravitee-apim-console-webui/src/services-ngx/application-members.service.ts`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application.component.html`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-tab-members/application-tab-members.component.ts`

### Delete
- `None`

## API Contract Changes
- `None`

## Impacted Modules
- `gravitee-apim-console-webui`
- `gravitee-apim-portal-webui-next`

## Validation
- Automated checks:
- frontend tests for toggle rendering, save behavior, and portal guard behavior
- Manual checks:
- verify toggles in admin UI affect the portal without requiring a manual restart

## Open Questions or Blockers
### APIM-10345-TASK-OQ-1
- Question: Which exact admin screen should host the three feature toggles?
- Answer: `TBD`
- Status: `Open`
