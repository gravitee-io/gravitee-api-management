# APIM-10345 Phase 03 Task 02 Backend Implement Invitation Lifecycle

## Task Metadata
- Epic: `APIM-10345 Member Mapping to Application`
- Phase: `03 Invitation Lifecycle Management`
- Task number: `02`
- Area: `backend`
- Suggested filename: `APIM-10345-phase-03-task-02-backend-implement-invitation-lifecycle.md`

## Goal
- Implement backend support for invite creation, pending-invite visibility, invite search, invited-role updates, resend, invite deletion, and invitation acceptance transition.

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
- This task implements the invitation lifecycle that the frontend will surface as pending and invited member states.

## Concrete Implementation Changes
- Implement invite creation with email validation, bulk input handling, role assignment, and secure token generation.
- Implement retrieval and search of invitation records for display in a dedicated invitations list or section rather than the members read path.
- Implement invited-role update without invalidating valid invitation tokens unless explicitly required.
- Implement resend behavior that refreshes invitation validity without creating duplicate active memberships.
- Implement invite deletion and token invalidation behavior.
- Implement invitation acceptance so a valid acceptance creates active membership and removes the invitation from the pending lifecycle.
- Preserve the local state model where a newly registered platform account can still be represented as invitation-pending until acceptance.

## Planned File Changes
### Create
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/ApplicationInvitationsResource.java`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/test/java/io/gravitee/rest/api/portal/rest/resource/ApplicationInvitationsResourceTest.java`

### Update
- `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/rest/api/service/impl/InvitationServiceImpl.java`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/rest/api/service/impl/UserServiceImpl.java`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/rest/api/service/impl/MembershipServiceImpl.java`

### Delete
- `None`

## API Contract Changes
- `None` beyond the contract defined in `APIM-10345 Phase 03 Task 01`.

## Impacted Modules
- `gravitee-apim-rest-api`
- invitation, email, and token-related backend components

## Validation
- Automated checks:
- backend tests for invite creation, bulk input handling, expiration, invitation search, resend, role updates, deletion, acceptance, and post-delete invalidation behavior
- Manual checks:
- verify pending and expired states are returned correctly for frontend rendering

## Open Questions or Blockers
### APIM-10345-TASK-OQ-1
- Question: What exact expiration duration should the implementation use for invitation tokens?
- Answer: `TBD`
- Status: `Open`
