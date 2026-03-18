# APIM-10345 Phase 02 Task 02 Backend Implement User Lookup and Active Member Mutations

## Task Metadata
- Epic: `APIM-10345 Member Mapping to Application`
- Phase: `02 Registered Member Management`
- Task number: `02`
- Area: `backend`
- Suggested filename: `APIM-10345-phase-02-task-02-backend-implement-user-lookup-and-active-member-mutations.md`

## Goal
- Implement backend support for registered-user lookup, add-member mutations, active-member role updates, and active-member deletion.

## Related Requirements and Stories
- Epic requirements: [../APIM-10345-requirements.md](../APIM-10345-requirements.md)
- Related stories:
- [APIM-12764 Search for Existing System Users](../stories/APIM-10345-story-03-APIM-12764-search-for-existing-system-users.md)
- [APIM-12765 Add Registered User to Application](../stories/APIM-10345-story-04-APIM-12765-add-registered-user-to-application.md)
- [APIM-12770 Update Roles for Active Members](../stories/APIM-10345-story-05-APIM-12770-update-roles-for-active-members.md)
- [APIM-11532 Delete Registered Member](../stories/APIM-10345-story-15-APIM-11532-delete-registered-member.md)
- If the task is not tied to a single story, explain which Epic requirement or technical foundation it supports:
- This task turns the active-member management contract into working portal-facing behavior.

## Concrete Implementation Changes
- Implement eligible-user search with duplicate-member filtering or explicit `Already Added` markers and returned email identity.
- Implement add-member mutation for registered users with selected roles and the documented default-role restrictions.
- Implement active-member role update mutation and enforce owner or creator restrictions.
- Implement active-member deletion with confirmation-safe semantics and owner-protection rules.
- Ensure successful mutations are observable by the phase 01 members table refresh path.

## Planned File Changes
### Create
- `None`

### Update
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/ApplicationMembersResource.java`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/test/java/io/gravitee/rest/api/portal/rest/resource/ApplicationMembersResourceTest.java`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/rest/api/service/impl/MembershipServiceImpl.java`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/rest/api/service/impl/UserServiceImpl.java`

### Delete
- `None`

## API Contract Changes
- `None` beyond the contract defined in `APIM-10345 Phase 02 Task 01`.

## Impacted Modules
- `gravitee-apim-rest-api`
- member-management domain and repository layers

## Validation
- Automated checks:
- focused backend tests for lookup filtering, add-member behavior, duplicate protection, and role updates
- Manual checks:
- verify new members appear in the list and role changes are reflected through subsequent reads

## Open Questions or Blockers
### APIM-10345-TASK-OQ-1
- Question: Is active-member deletion intentionally part of this phase or should it remain excluded until product scope confirms it?
- Answer: Jira now defines [APIM-11532](https://gravitee.atlassian.net/browse/APIM-11532) as a dedicated story, so active-member deletion is part of phase 02.
- Status: `Resolved`
