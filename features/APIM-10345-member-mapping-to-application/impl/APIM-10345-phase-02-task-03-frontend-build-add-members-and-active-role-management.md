# APIM-10345 Phase 02 Task 03 Frontend Build Add Members and Active Role Management

## Task Metadata
- Epic: `APIM-10345 Member Mapping to Application`
- Phase: `02 Registered Member Management`
- Task number: `03`
- Area: `frontend`
- Suggested filename: `APIM-10345-phase-02-task-03-frontend-build-add-members-and-active-role-management.md`

## Goal
- Build the registered-user add flow plus active-member role-edit and delete flows on top of the phase 02 backend capabilities.

## Related Requirements and Stories
- Epic requirements: [../APIM-10345-requirements.md](../APIM-10345-requirements.md)
- Related stories:
- [APIM-12764 Search for Existing System Users](../stories/APIM-10345-story-03-APIM-12764-search-for-existing-system-users.md)
- [APIM-12765 Add Registered User to Application](../stories/APIM-10345-story-04-APIM-12765-add-registered-user-to-application.md)
- [APIM-12770 Update Roles for Active Members](../stories/APIM-10345-story-05-APIM-12770-update-roles-for-active-members.md)
- [APIM-11532 Delete Registered Member](../stories/APIM-10345-story-15-APIM-11532-delete-registered-member.md)
- If the task is not tied to a single story, explain which Epic requirement or technical foundation it supports:
- This task converts the baseline members tab into an actionable registered-member management UI.

## Concrete Implementation Changes
- Build the add-members dropdown and user-search modal states from the exported screen set.
- Implement selected-user chips, role selector, duplicate-state rendering, and optional notify control where confirmed.
- Add active-member edit and delete entry points from the members table, while keeping destructive actions unavailable for the protected owner row.
- Refresh the members list after successful add or role-update operations.

## Planned File Changes
### Create
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-members-add-dialog/application-members-add-dialog.component.ts`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-members-add-dialog/application-members-add-dialog.component.html`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-members-add-dialog/application-members-add-dialog.component.scss`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-members-add-dialog/application-members-add-dialog.component.spec.ts`

### Update
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-tab-members/application-tab-members.component.ts`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-tab-members/application-tab-members.component.html`
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
- frontend tests for dropdown behavior, modal rendering, selected-user state, and active-role edit handling
- Manual checks:
- verify the user-search flow against the exported modal screens and confirm the table refreshes after successful mutations

## Open Questions or Blockers
### APIM-10345-TASK-OQ-1
- Question: Should the notify checkbox be active in phase 02 for registered-user add, or only exposed once backend notification behavior is confirmed?
- Answer: `TBD`
- Status: `Open`
