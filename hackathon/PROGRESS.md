# Application Members Feature - Progress Tracker

## Phase & Story Checklist

### Phase 1: View Members
- ✅ 1.0 - BE: Define Application Members V2 Portal API Contract
- ✅ 1.1 - BE: List Application Members (UseCase)
- ✅ 1.2 - BE: List Application Roles (UseCase)
- ✅ 1.3 - FE: Members Tab & Routing
- ✅ 1.4 - FE: Members Table
- ✅ 1.5 - FE: Extend Paginated Table for Custom Action Cells

### Phase 2: Edit & Delete Members
- ✅ 2.1 - BE: Update Member Role (UseCase)
- ✅ 2.2 - BE: Delete Application Member (UseCase)
- ✅ 2.3 - FE: Edit Member Role Dialog
- ✅ 2.4 - FE: Delete Member Dialog

### Phase 3: Add Members
- ✅ 3.1 - BE: Search Users for Application (UseCase)
- ✅ 3.2 - BE: Add Application Member (UseCase)
- ✅ 3.3 - FE: Search Users Dialog (Add Members)

### Phase 4: Invite Users via Email
- [ ] 4.1 - BE: Invite User to Application (UseCase)
- [ ] 4.2 - BE: List Pending Invitations (UseCase)
- [ ] 4.3 - BE: Delete Pending Invitation (UseCase)
- [ ] 4.4 - BE: Update Invited Member Role (UseCase)
- [ ] 4.5 - BE: Invitation Acceptance to Membership Flow
- [ ] 4.6 - BE: Unified Members + Invitations Query (UseCase)
- [ ] 4.7 - FE: Invite User Dialog
- [ ] 4.8 - FE: Display Invited Members in Table

### Phase 5: Transfer Ownership
- [ ] 5.1 - BE: Transfer Application Ownership (UseCase)
- [ ] 5.2 - FE: Transfer Ownership Dialog

---

## Summary of Completed Work

- **Story 1.0 (BE: Define Application Members V2 Portal API Contract)** — Added V2 contract paths and schemas in portal OpenAPI, including `/applications/{applicationId}/membersV2*` family and `/configuration/applications/rolesV2`.
- **Story 1.1 (BE: List Application Members (UseCase))** — Added members V2 use case/resource flow and mapper in Onion style, wired under application sub-resource, and covered with use case + REST tests.
- **Story 1.2 (BE: List Application Roles (UseCase))** — Added `GetApplicationRolesUseCase`, extended `RoleQueryService` with `findByScope`, implemented new `/configuration/applications/rolesV2` resource, and added use case + REST tests.
- **Story 1.3 (FE: Members Tab & Routing)** — Application tab members shell component, route `members` under `:applicationId`, Members tab link in application nav (gated by `MEMBER` read permission), component spec. Aligned with portal-next design system (theme SCSS, m3 typography) and Angular rules (signal inputs, standalone).
- **Story 1.4 (FE: Members Table)** — `ApplicationMembersService` (list endpoint), `MemberV2`/`MembersV2Response` entity types + fixtures, full table component with `rxResource`, search bar, header with "Transfer Ownership" + "Add Members" dropdown (User Search / Email Invitation with descriptions), `app-paginated-table`, empty states, permission-gated "Add Members" button. Service spec (3 tests) + component spec (14 tests).
- **Story 1.5 (FE: Extend Paginated Table for Custom Action Cells)** — Added `actions` column type to `TableColumn` with `ActionButton[]`, `actionClick` output with `TableActionEvent`, edit/delete icon buttons per row. Members table wired with Actions column. Post-story refinements: `rowLink`/`showExpandColumn` inputs (members: no chevron, no row click), actions column right-aligned. Paginated-table spec (12 tests). 65+ tests pass.
- **Story 2.3 (FE: Edit Member Role Dialog)** — `EditMemberRoleDialogComponent` with `mat-select` role dropdown, pre-selected current role, Save disabled until changed. Service: `listRoles()`, `updateMemberRole()`. Edit action wired in members table (fetches roles → opens dialog → calls PUT → snackbar → reload). `rowActionsHidden` input on paginated-table to hide actions for PRIMARY_OWNER rows. Dialog harness + spec (7 tests), service spec (2 new tests). BE not ready — endpoints will 404 at runtime until Stories 2.1/1.2 land.
- **Story 2.4 (FE: Delete Member Dialog)** — Reuses existing `ConfirmDialogComponent` (no new dialog component). Delete action wired in members table: opens confirm dialog ("Remove Member" / red "Remove" button, matching `delete-member-dialog.png`), on confirm calls `deleteMember()` → snackbar → reloads table. Service: `deleteMember(applicationId, memberId)` → `DELETE /membersV2/{memberId}`. Component spec: 4 new tests (dialog opens, cancel no-op, confirm calls API + reload, PRIMARY_OWNER has no actions). Service spec: 1 new test. All 24 component + 6 service tests pass.
- **Story 2.1 (BE: Update Member Role (UseCase))** — Added `UpdateApplicationMemberUseCase` (role validation + PRIMARY_OWNER guard + member update), wired `PUT /applications/{applicationId}/membersV2/{memberId}` in `ApplicationMembersResourceV2`, and added use case + REST tests (`200` success, `400` bad role, `403` no permission).
- **Story 2.2 (BE: Delete Application Member (UseCase))** — Added `DeleteApplicationMemberUseCase` (not found handling + PRIMARY_OWNER guard + member deletion), wired `DELETE /applications/{applicationId}/membersV2/{memberId}` in `ApplicationMembersResourceV2`, and added use case + REST tests (`204` success, `400` primary owner, `403` no permission).
- **Story 3.1 (BE: Search Users for Application (UseCase))** — Added `SearchUsersForApplicationMemberUseCase`, introduced `ApplicationMemberUserQueryService` + legacy wrapper over `IdentityService.search(...)`, implemented `POST /applications/{applicationId}/membersV2/_search-users` with `APPLICATION_MEMBER[CREATE]`, filtered out existing app members, and added use case + REST tests.
- **Story 3.2 (BE: Add Application Member (UseCase))** — Added `AddApplicationMemberUseCase` (role validation, `PRIMARY_OWNER` rejection, duplicate member rejection, batch-capable input), implemented `POST /applications/{applicationId}/membersV2` in `ApplicationMembersResourceV2`, and added use case + REST tests (`201` success, `400` invalid role / primary owner / duplicate, `403` missing permission).
- **Story 3.3 (FE: Search Users Dialog (Add Members))** — `SearchUsersDialogComponent` with role `mat-select` (pre-selects default non-system role), user search `mat-autocomplete` (debounced 300ms, min 2 chars), `mat-chip-set` for selected users with remove, notify `mat-checkbox`. Service: `searchUsers()` (POST `_search-users`), `addMembers()` (POST `/membersV2`). "User Search" menu item wired to open dialog → on result calls `addMembers()` → snackbar → reload. Dialog harness + spec (4 tests), service spec (2 new tests). All 37 tests pass. BE not ready — endpoints will 404 at runtime until Stories 3.1/3.2 land.

## Key Decisions

- **New V2 paths instead of modifying legacy endpoints** -- e.g. `/membersV2` instead of `/members`. Avoids breaking existing consumers (AC3).
- **Onion Architecture with @UseCase classes** -- All new backend code follows the UseCase pattern (like `PortalNavigationItemResource`), not the legacy service-direct pattern (like `ApplicationMembersResource`).
- **API contract defined upfront (Story 1.0)** -- Enables parallel FE/BE development from day one.
- **Unified members + invitations query (Story 4.6)** -- Single backend endpoint returns both active members and pending invitations, instead of forcing frontend to merge two separate responses.
- **Dedicated paginated-table extension (Story 1.5)** -- Existing `paginated-table` only supports `text`/`date` columns. Actions column needs explicit work on the shared component.
- **No page 1 / page 2 navigation** -- "Add members" and "Invite users" are dialog-driven from a dropdown button, not a separate page view.
- **APPLICATION_MEMBER[CREATE] for user search** -- The existing `POST /users/_search` requires `ORGANIZATION_USERS[READ]` which portal users lack. New search endpoint uses application-scoped permission.

## Per-Agent Logs

Detailed insights, gotchas, surprises, blockers, and effective prompts are tracked per agent:

- [agent-ivan.md](agent-ivan.md) -- Ivan's agent
- [agent-jarek.md](agent-jarek.md) -- Jarek's agent
