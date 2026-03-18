# APIM-10345 Member Mapping to Application Implementation Plan

## Related Requirements
- Epic directory: `features/APIM-10345-member-mapping-to-application/`
- Epic requirements: [APIM-10345-requirements.md](./APIM-10345-requirements.md)
- Implementation plan file: `features/APIM-10345-member-mapping-to-application/APIM-10345-implementation-plan.md`
- Related stories:
- `APIM-10345-story-01` [APIM-12280 View existing application members](./stories/APIM-10345-story-01-APIM-12280-view-existing-application-members.md)
- `APIM-10345-story-02` [APIM-11539 Search among existing members](./stories/APIM-10345-story-02-APIM-11539-search-among-existing-members.md)
- `APIM-10345-story-03` [APIM-12764 Search for Existing System Users](./stories/APIM-10345-story-03-APIM-12764-search-for-existing-system-users.md)
- `APIM-10345-story-04` [APIM-12765 Add Registered User to Application](./stories/APIM-10345-story-04-APIM-12765-add-registered-user-to-application.md)
- `APIM-10345-story-05` [APIM-12770 Update Roles for Active Members](./stories/APIM-10345-story-05-APIM-12770-update-roles-for-active-members.md)
- `APIM-10345-story-15` [APIM-11532 Delete Registered Member](./stories/APIM-10345-story-15-APIM-11532-delete-registered-member.md)
- `APIM-10345-story-06` [APIM-12766 Invite New User via Email](./stories/APIM-10345-story-06-APIM-12766-invite-new-user-via-email.md)
- `APIM-10345-story-07` [APIM-12767 View Pending Invitation Dashboard](./stories/APIM-10345-story-07-APIM-12767-view-pending-invitation-dashboard.md)
- `APIM-10345-story-17` [APIM-11538 Search among Invited Members](./stories/APIM-10345-story-17-APIM-11538-search-among-invited-members.md)
- `APIM-10345-story-08` [APIM-12768 Delete Pending Invitation](./stories/APIM-10345-story-08-APIM-12768-delete-pending-invitation.md)
- `APIM-10345-story-09` [APIM-12771 Update Roles for Invited Members](./stories/APIM-10345-story-09-APIM-12771-update-roles-for-invited-members.md)
- `APIM-10345-story-16` [APIM-11535 Resend Invitation](./stories/APIM-10345-story-16-APIM-11535-resend-invitation.md)
- `APIM-10345-story-10` [APIM-12772 Transfer Ownership to Registered Member](./stories/APIM-10345-story-10-APIM-12772-transfer-ownership-to-registered-member.md)
- `APIM-10345-story-11` [APIM-12888 Administrative Toggle Controls](./stories/APIM-10345-story-11-APIM-12888-administrative-toggle-controls.md)
- `APIM-10345-story-14` [APIM-11531 Acceptance of Invitation](./stories/APIM-10345-story-14-APIM-11531-acceptance-of-invitation.md)

## Scope and Impacted Modules
- Scope:
- Deliver the Next Gen Portal application member-management capability as phased, end-to-end increments covering read paths, active member management including deletion, invitation lifecycle including search, resend, and acceptance transition, ownership transfer, and the three administrative feature toggles.
- Keep [APIM-12773 API Selection Wizard](https://gravitee.atlassian.net/browse/APIM-12773) and [APIM-12774 Finalize API Mapping on App Creation](https://gravitee.atlassian.net/browse/APIM-12774) out of the local implementation slice even though they remain linked to the Epic in Jira.
- Impacted modules:
- `gravitee-apim-portal-webui-next`
- `gravitee-apim-webui-libs`
- `gravitee-apim-rest-api`
- Portal OpenAPI contract
- Admin settings UI in `gravitee-apim-console-webui` or equivalent management surface
- Invitation and email support components

## Assumptions and Constraints
- The first delivery targets the Next Gen Developer Portal only.
- Per `APIM-10345-decision-01`, the first delivery must expose active members and invitations as separate list surfaces within the same feature area.
- New or revised portal-facing APIs should be introduced as additive contracts rather than by mutating existing legacy endpoints in place.
- Each phase must end in a slice that can be validated with both backend and frontend where the user-facing flow is in scope.

## Phase Overview
- Phase 01: Members Read Path Foundation
- Phase 02: Registered Member Management
- Phase 03: Invitation Lifecycle Management
- Phase 04: Ownership Transfer and Feature Toggles

## Phase 01 Members Read Path Foundation
- Goal:
- Deliver the first working members tab with a stable `Name` / `Email` / `Role` / `Actions` table contract, always-present pagination, members retrieval, empty state, and in-page search over the existing member set.
- Why this phase is a coherent increment:
- It creates the first testable end-to-end slice of the feature: a user can open the members area, see current members, and search within that list. All later write flows depend on this baseline view.
- Validation approach:
- Backend contract and resource tests for member list, always-present pagination, and search
- Frontend tests for tab routing, table rendering, pagination controls, and empty state
- Manual verification of the members tab using populated and empty datasets

### Tasks
- [ ] [Phase 01 Task 01 Contract](./impl/APIM-10345-phase-01-task-01-contract-define-members-read-contract.md)
  - Primary files:
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml`
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/test/resources/portal-openapi.yaml`
  - Depends on:
  - `none`
  - Reason:
  - This task defines the first additive contract for the feature and should start before any backend or frontend implementation that consumes it.
- [ ] [Phase 01 Task 02 Backend](./impl/APIM-10345-phase-01-task-02-backend-implement-members-read-path.md)
  - Primary files:
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/ApplicationMembersResource.java`
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/test/java/io/gravitee/rest/api/portal/rest/resource/ApplicationMembersResourceTest.java`
  - Depends on:
  - [Phase 01 Task 01 Contract](./impl/APIM-10345-phase-01-task-01-contract-define-members-read-contract.md)
  - Reason:
  - The backend read path must implement the agreed portal contract so request parameters, response fields, and pagination behavior are stable before the UI is wired.
- [ ] [Phase 01 Task 03 Frontend](./impl/APIM-10345-phase-01-task-03-frontend-build-members-tab-and-table.md)
  - Primary files:
  - `gravitee-apim-portal-webui-next/src/app/app.routes.ts`
  - `gravitee-apim-portal-webui-next/src/app/applications/application/application.component.html`
  - `gravitee-apim-portal-webui-next/src/services/application.service.ts`
  - Depends on:
  - [Phase 01 Task 02 Backend](./impl/APIM-10345-phase-01-task-02-backend-implement-members-read-path.md)
  - Reason:
  - The members tab and table should be built on top of the real read API so rendering, search, and empty states match the final contract instead of temporary mocked assumptions.

## Phase 02 Registered Member Management
- Goal:
- Enable searching eligible registered users, adding them to an application, updating roles for active members, and deleting active members from the members area.
- Why this phase is a coherent increment:
- It turns the read-only members area into a usable collaboration surface for active members. The user can find eligible platform users, add them, maintain role assignments, and remove no-longer-needed access in one closed slice.
- Validation approach:
- Backend tests for user lookup, add-member mutations, active-role updates, and active-member deletion
- Frontend tests for add-members entry points, user-search modal, and active-member edit and delete flows
- Manual verification that newly added users appear immediately in the members table, can have roles updated, and can be removed when allowed

### Tasks
- [ ] [Phase 02 Task 01 Contract](./impl/APIM-10345-phase-02-task-01-contract-define-active-member-management-contract.md)
  - Primary files:
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml`
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/test/resources/portal-openapi.yaml`
  - Depends on:
  - [Phase 01 Task 01 Contract](./impl/APIM-10345-phase-01-task-01-contract-define-members-read-contract.md)
  - Reason:
  - Active-member mutations and lookup endpoints must stay aligned with the member table model already introduced in phase 01.
- [ ] [Phase 02 Task 02 Backend](./impl/APIM-10345-phase-02-task-02-backend-implement-user-lookup-and-active-member-mutations.md)
  - Primary files:
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/ApplicationMembersResource.java`
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/UsersResource.java`
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/test/java/io/gravitee/rest/api/portal/rest/resource/ApplicationMembersResourceTest.java`
  - Depends on:
  - [Phase 02 Task 01 Contract](./impl/APIM-10345-phase-02-task-01-contract-define-active-member-management-contract.md)
  - Reason:
  - The backend lookup and mutation behavior should implement the finalized payloads, duplicate-handling rules, add and delete semantics, and role-update semantics before frontend modal work begins.
- [ ] [Phase 02 Task 03 Frontend](./impl/APIM-10345-phase-02-task-03-frontend-build-add-members-and-active-role-management.md)
  - Primary files:
  - `gravitee-apim-portal-webui-next/src/app/app.routes.ts`
  - `gravitee-apim-portal-webui-next/src/app/applications/application/application.component.html`
  - `gravitee-apim-portal-webui-next/src/services/application.service.ts`
  - Depends on:
  - [Phase 02 Task 02 Backend](./impl/APIM-10345-phase-02-task-02-backend-implement-user-lookup-and-active-member-mutations.md)
  - Reason:
  - The add-members and active-member action UI needs working lookup and mutation endpoints to validate selected users, submit updates, remove members, and refresh the members table correctly.

## Phase 03 Invitation Lifecycle Management
- Goal:
- Add invitation-based onboarding and management of invited users through a dedicated invitations list or section within the same member-management feature area, including bulk invite input, invitation search, resend, and the transition from invitation state to active membership after acceptance.
- Why this phase is a coherent increment:
- It extends member management from registered users to non-registered collaborators without coupling invitation rows to the members table. Invitation creation, pending visibility, invitation search, invited-role updates, resend, invite deletion, and accepted-invite transition can be tested together as one lifecycle while preserving the local distinction between `Invited Person` and `Registered User`.
- Validation approach:
- Backend tests for invitation creation, pending queries, invitation search, resend, role updates, invite revocation, and accepted-invite transition
- Frontend tests for invite modal, invitations-list rendering, invited-member actions, and movement from invitations to members after acceptance
- Manual verification that invited users appear in the dedicated invitations surface with the correct invitation lifecycle state, that a newly registered but not-yet-accepted person remains invitation-scoped, and that accepted invites become active members without collapsing the invitation and membership states prematurely

### Tasks
- [ ] [Phase 03 Task 01 Contract](./impl/APIM-10345-phase-03-task-01-contract-define-invitation-management-contract.md)
  - Primary files:
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml`
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/test/resources/portal-openapi.yaml`
  - Depends on:
  - [Phase 01 Task 01 Contract](./impl/APIM-10345-phase-01-task-01-contract-define-members-read-contract.md)
  - Reason:
  - Invitation management must align with the phase 01 feature-area structure and navigation model while keeping a separate list contract per `APIM-10345-decision-01`.
- [ ] [Phase 03 Task 02 Backend](./impl/APIM-10345-phase-03-task-02-backend-implement-invitation-lifecycle.md)
  - Primary files:
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/rest/api/service/impl/InvitationServiceImpl.java`
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/rest/api/service/impl/UserServiceImpl.java`
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/test/java/io/gravitee/rest/api/portal/rest/resource/ApplicationMembersResourceTest.java`
  - Depends on:
  - [Phase 03 Task 01 Contract](./impl/APIM-10345-phase-03-task-01-contract-define-invitation-management-contract.md)
  - Reason:
  - Backend invitation creation, search, resend, pending state retrieval, invited-role updates, deletion, and acceptance transition all depend on a stable lifecycle contract and status model.
- [ ] [Phase 03 Task 03 Frontend](./impl/APIM-10345-phase-03-task-03-frontend-build-invite-and-pending-member-management.md)
  - Primary files:
  - `gravitee-apim-portal-webui-next/src/app/app.routes.ts`
  - `gravitee-apim-portal-webui-next/src/app/applications/application/application.component.html`
  - `gravitee-apim-portal-webui-next/src/services/application.service.ts`
  - Depends on:
  - [Phase 03 Task 02 Backend](./impl/APIM-10345-phase-03-task-02-backend-implement-invitation-lifecycle.md)
  - Reason:
  - The invite modal and invitations-list actions should be implemented against the real invitation lifecycle so the UI reflects actual pending, expired, resent, and accepted states without treating registration alone as active membership.

## Phase 04 Ownership Transfer and Feature Toggles
- Goal:
- Deliver the ownership-transfer flow together with feature toggles that can enable or disable member mapping, invitation sending, and ownership transfer safely.
- Why this phase is a coherent increment:
- Ownership transfer is the highest-risk management action and should be added only after core member flows are stable. Feature toggles belong with that final rollout slice because they gate the now-complete portal behavior, including invitation sending, and can be validated together with admin controls.
- Validation approach:
- Backend tests for ownership transfer, toggle persistence, and audit logging
- Frontend tests for transfer flow, toggle-aware portal behavior, and admin settings controls
- Manual verification that the three toggles hide and reveal the correct features and that ownership transfer works end-to-end when enabled

### Tasks
- [ ] [Phase 04 Task 01 Contract](./impl/APIM-10345-phase-04-task-01-contract-define-ownership-and-toggle-contract.md)
  - Primary files:
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml`
  - `gravitee-apim-console-webui/src/services-ngx/application-members.service.ts`
  - Depends on:
  - [Phase 02 Task 01 Contract](./impl/APIM-10345-phase-02-task-01-contract-define-active-member-management-contract.md)
  - Reason:
  - Ownership transfer reuses the registered-member identity model introduced in active-member management, and feature-toggle exposure must align with already defined feature surfaces.
- [ ] [Phase 04 Task 02 Backend](./impl/APIM-10345-phase-04-task-02-backend-implement-ownership-and-toggle-backend.md)
  - Primary files:
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/ApplicationMembersResource.java`
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/rest/api/service/impl/MembershipServiceImpl.java`
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-management/gravitee-apim-rest-api-management-rest/src/main/java/io/gravitee/rest/api/management/rest/resource/ApplicationMembersResource.java`
  - Depends on:
  - [Phase 04 Task 01 Contract](./impl/APIM-10345-phase-04-task-01-contract-define-ownership-and-toggle-contract.md)
  - Reason:
  - Ownership-transfer semantics, three-toggle persistence, and audit payloads need to be fixed before backend implementation can remain stable.
- [ ] [Phase 04 Task 03 Frontend](./impl/APIM-10345-phase-04-task-03-frontend-build-ownership-transfer-flow.md)
  - Primary files:
  - `gravitee-apim-portal-webui-next/src/app/app.routes.ts`
  - `gravitee-apim-portal-webui-next/src/app/applications/application/application.component.html`
  - `gravitee-apim-portal-webui-next/src/services/application.service.ts`
  - Depends on:
  - [Phase 04 Task 02 Backend](./impl/APIM-10345-phase-04-task-02-backend-implement-ownership-and-toggle-backend.md)
  - Reason:
  - The portal transfer UI requires working eligibility checks, mutation behavior, and feature-toggle state from the backend before the flow can be validated end-to-end.
- [ ] [Phase 04 Task 04 Frontend](./impl/APIM-10345-phase-04-task-04-frontend-build-admin-toggle-controls-and-rollout-guards.md)
  - Primary files:
  - `gravitee-apim-console-webui/src/management/application/details/user-group-access/members/application-general-members.component.ts`
  - `gravitee-apim-console-webui/src/services-ngx/application-members.service.ts`
  - `gravitee-apim-portal-webui-next/src/app/applications/application/application.component.html`
  - Depends on:
  - [Phase 04 Task 02 Backend](./impl/APIM-10345-phase-04-task-02-backend-implement-ownership-and-toggle-backend.md)
  - Reason:
  - Admin controls and portal rollout guards need a persisted source of truth for the three toggle states and update endpoints before frontend wiring can be completed and tested.

## Cross-Phase Dependencies
- Phase 02 depends on Phase 01
- Reason: both registered-user add flows and active-role updates depend on the members tab, stable table structure, and members read contract produced in Phase 01.
- Phase 03 depends on Phase 02
- Reason: invitation flows reuse the same entry points, role-selection conventions, and post-mutation refresh patterns established for registered-member management in Phase 02.
- Phase 04 depends on Phases 01 through 03
- Reason: ownership transfer requires a stable registered-member management base, and feature toggles only make sense once the gated capabilities, including invitation sending, already exist and can be hidden, exposed, and audited.

## Open Issues and Follow-up Items
- [APIM-12773 API Selection Wizard](https://gravitee.atlassian.net/browse/APIM-12773) remains linked in Jira, but it stays out of the local member-mapping implementation slice documented here.
- [APIM-12774 Finalize API Mapping on App Creation](https://gravitee.atlassian.net/browse/APIM-12774) remains linked in Jira, but it stays out of the local member-mapping implementation slice documented here.
- `APIM-10345-decision-01` was agreed on `2025-03-18`. Exported screens that show inline invited rows should now be interpreted as pre-decision design evidence and must not override the agreed separate-list direction.
- [APIM-12888 Administrative Toggle Controls](https://gravitee.atlassian.net/browse/APIM-12888) currently contains stale wording in Jira that still says "two distinct toggles". The local plan follows the newer Jira wording that names three toggles: member mapping, invitation sending, and ownership transfer.
