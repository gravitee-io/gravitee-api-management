# APIM-10345 Phase 03 Task 03 Frontend Build Invite and Pending Member Management

## Task Metadata
- Epic: `APIM-10345 Member Mapping to Application`
- Phase: `03 Invitation Lifecycle Management`
- Task number: `03`
- Area: `frontend`
- Suggested filename: `APIM-10345-phase-03-task-03-frontend-build-invite-and-pending-member-management.md`

## Goal
- Build the email-invitation flow, invitation search, resend, and invited-member management UI on top of the phase 03 backend lifecycle.

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
- This task exposes the full invited-member lifecycle in the portal UI.

## Concrete Implementation Changes
- Build the invite-email modal states from the exported screens, including multi-email entry.
- Render a dedicated invitations list or section with invitation-specific identity, lifecycle state, role, and row actions, following `APIM-10345-decision-01`.
- Add invitation search by email together with invited-role edit, resend, and invite-delete actions to the invitation rows.
- Refresh the invitations list or section after successful invite lifecycle mutations.

## Planned File Changes
### Create
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-tab-invitations/application-tab-invitations.component.ts`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-tab-invitations/application-tab-invitations.component.html`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-tab-invitations/application-tab-invitations.component.scss`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-tab-invitations/application-tab-invitations.component.spec.ts`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-invite-dialog/application-invite-dialog.component.ts`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-invite-dialog/application-invite-dialog.component.html`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-invite-dialog/application-invite-dialog.component.scss`
- `gravitee-apim-portal-webui-next/src/app/applications/application/application-invite-dialog/application-invite-dialog.component.spec.ts`

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
- frontend tests for invite modal validation, multi-email handling, invitations-list rendering, invitation search, and invited-member actions
- Manual checks:
- verify that a newly invited email appears in the dedicated invitations surface with the correct invitation state, that resend and search behave correctly, and that a newly registered but not-yet-accepted person remains invitation-scoped rather than appearing as an active member

## Open Questions or Blockers
### APIM-10345-TASK-OQ-1
- Question: Should phase 03 expose invited entries inline in the main table or as a separate dedicated pending section?
- Answer: `APIM-10345-decision-01` was agreed on `2025-03-18`. Phase 03 should expose invited entries through a separate dedicated invitations list or section, not inline in the main members table.
- Status: `Resolved`
