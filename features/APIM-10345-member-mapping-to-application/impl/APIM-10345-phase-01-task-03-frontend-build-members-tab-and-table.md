# APIM-10345 Phase 01 Task 03 Frontend Build Members Tab and Table

## Task Metadata
- Epic: `APIM-10345 Member Mapping to Application`
- Phase: `01 Members Read Path Foundation`
- Task number: `03`
- Area: `frontend`
- Suggested filename: `APIM-10345-phase-01-task-03-frontend-build-members-tab-and-table.md`

## Goal
- Build the members tab, members table, always-present pagination, empty state, and in-page search UI on top of the phase 01 backend read path.

## Related Requirements and Stories
- Epic requirements: [../APIM-10345-requirements.md](../APIM-10345-requirements.md)
- Related stories:
- [APIM-12280 View existing application members](../stories/APIM-10345-story-01-APIM-12280-view-existing-application-members.md)
- [APIM-11539 Search among existing members](../stories/APIM-10345-story-02-APIM-11539-search-among-existing-members.md)
- If the task is not tied to a single story, explain which Epic requirement or technical foundation it supports:
- This task creates the baseline visual surface used by all later member-management interactions.

## Concrete Implementation Changes
- Add the members tab to the application detail area in `portal-next`.
- Build the members table using the agreed `Name`, `Email`, `Role`, and `Actions` columns and the documented `Name` cell composition.
- Render pagination controls consistently for the members view, including low-cardinality result sets.
- Implement empty state and in-page search behavior against the phase 01 API.
- Keep the phase 01 surface scoped to active members only, with invitations introduced later through a separate phase 03 surface per `APIM-10345-decision-01`.
- Align the first rendered states with exported screens `Member - Populated list-1.png` and `Members - Empty.png`.

## Planned File Changes
### Create
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-tab-members/application-tab-members.component.ts`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-tab-members/application-tab-members.component.html`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-tab-members/application-tab-members.component.scss`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-tab-members/application-tab-members.component.spec.ts`

### Update
- `gravitee-apim-portal-webui-next/src/app/app.routes.ts`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application.component.html`
- `gravitee-apim-portal-webui-next/src/services/application.service.ts`
- `gravitee-apim-portal-webui-next/src/entities/application/application.ts`

### Delete
- `None`

## API Contract Changes
- `None`

## Impacted Modules
- `gravitee-apim-portal-webui-next`
- `gravitee-apim-webui-libs`

## Validation
- Automated checks:
- frontend tests for routing, table rendering, pagination controls, search input behavior, and empty state
- Manual checks:
- verify the members tab with populated and empty datasets and confirm column structure matches the documented design mapping

## Open Questions or Blockers
### APIM-10345-TASK-OQ-1
- Question: Should phase 01 render invited members if the backend already exposes them, or should the UI stay active-member-only until phase 03?
- Answer: Per `APIM-10345-decision-01`, phase 01 stays active-member-only. Invitations must be introduced through a separate invitations surface in phase 03.
- Status: `Resolved`
