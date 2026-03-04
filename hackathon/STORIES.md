# Application Members Feature - Detailed Story Breakdown

## Convention Reference

| Layer | Base Path |
|-------|-----------|
| Core UseCase | `gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/application_member/use_case/` |
| Core QueryService | `gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/application_member/query_service/` |
| Core Model | `gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/application_member/model/` |
| Infra QueryService Impl | `gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/infra/query_service/application_member/` |
| Portal REST Resource | `gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/` |
| Portal REST Mapper | `gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/mapper/` |
| Portal OpenAPI | `gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml` |
| UseCase Tests | `gravitee-apim-rest-api-service/src/test/java/io/gravitee/apim/core/application_member/use_case/` |
| InMemory Test Doubles | `gravitee-apim-rest-api-service/src/test/java/inmemory/` |
| Portal Resource Tests | `gravitee-apim-rest-api-portal-rest/src/test/java/io/gravitee/rest/api/portal/rest/resource/` |
| FE Component | `gravitee-apim-portal-webui-next/src/app/applications/application/application-tab-members/` |
| FE Service | `gravitee-apim-portal-webui-next/src/services/` |
| FE Routes | `gravitee-apim-portal-webui-next/src/app/app.routes.ts` |

**Naming conventions:**
- UseCase: `VerbNounUseCase.java` with inner `record Input(...)` / `record Output(...)`
- QueryService: `NounQueryService.java` interface in core, `NounQueryServiceImpl.java` in infra
- InMemory: `NounQueryServiceInMemory.java` in test `inmemory/` package
- Portal REST: `NounResourceV2.java` (or `NounResource.java` for new resources)
- FE Component: `kebab-case.component.ts` with matching `.html`, `.scss`, `.spec.ts`
- FE Service: `kebab-case.service.ts` with matching `.spec.ts`

---

## Phase 1: View Members

---

### Story 1.0 - BE: Define Application Members V2 Portal API Contract

**Dependencies:** None

#### Endpoint Info

All endpoints below are defined in this story (spec only, no implementation):

| Method | Path | Type |
|--------|------|------|
| GET | `/applications/{applicationId}/membersV2` | REWRITTEN_EXISTING |
| POST | `/applications/{applicationId}/membersV2` | REWRITTEN_EXISTING |
| PUT | `/applications/{applicationId}/membersV2/{memberId}` | REWRITTEN_EXISTING |
| DELETE | `/applications/{applicationId}/membersV2/{memberId}` | REWRITTEN_EXISTING |
| POST | `/applications/{applicationId}/membersV2/_search-users` | NEW_NOT_BASED_ON_EXISTING |
| POST | `/applications/{applicationId}/membersV2/_transfer-ownership` | REWRITTEN_EXISTING |
| POST | `/applications/{applicationId}/membersV2/_invite` | NEW_BASED_ON_EXISTING |
| GET | `/applications/{applicationId}/membersV2/_invitations` | NEW_NOT_BASED_ON_EXISTING |
| PUT | `/applications/{applicationId}/membersV2/_invitations/{invitationId}` | NEW_NOT_BASED_ON_EXISTING |
| DELETE | `/applications/{applicationId}/membersV2/_invitations/{invitationId}` | NEW_NOT_BASED_ON_EXISTING |
| GET | `/configuration/applications/rolesV2` | REWRITTEN_EXISTING |

REWRITTEN_EXISTING mappings (old -> new):
- `GET /applications/{applicationId}/members` -> `GET /applications/{applicationId}/membersV2`
- `POST /applications/{applicationId}/members` -> `POST /applications/{applicationId}/membersV2`
- `PUT /applications/{applicationId}/members/{memberId}` -> `PUT /applications/{applicationId}/membersV2/{memberId}`
- `DELETE /applications/{applicationId}/members/{memberId}` -> `DELETE /applications/{applicationId}/membersV2/{memberId}`
- `POST /applications/{applicationId}/members/_transfer_ownership` -> `POST /applications/{applicationId}/membersV2/_transfer-ownership`
- `GET /configuration/applications/roles` -> `GET /configuration/applications/rolesV2`

NEW_BASED_ON_EXISTING:
- `POST .../membersV2/_invite` based_on: `POST /configuration/groups/{groupId}/invitations` (GroupInvitationsResource pattern)

#### Subtasks

**1.0.1 - Define V2 member schemas in portal-openapi.yaml**
- Modify: `portal-openapi.yaml`
- Add schemas: `MemberV2`, `MemberV2Input`, `MembersV2Response`, `TransferOwnershipV2Input`, `InvitationInput`, `InvitationV2`, `InvitationsV2Response`, `SearchUsersV2Response`, `SearchUserV2`
- `MemberV2` adds fields vs legacy `Member`: `status` (active/pending), `type` (user/invited)
- Reference: existing `Member`, `MemberInput`, `TransferOwnershipInput` schemas in `portal-openapi.yaml` (lines 4731-4748, 5713-5724, 5824-5835)

**1.0.2 - Define V2 member endpoint paths in portal-openapi.yaml**
- Modify: `portal-openapi.yaml`
- Add all paths listed above with operation IDs, parameters, request/response refs, permission descriptions
- Reference: existing `/applications/{applicationId}/members` paths (lines 1154-1320)

**1.0.3 - Define V2 roles endpoint path**
- Modify: `portal-openapi.yaml`
- Add `GET /configuration/applications/rolesV2` path
- Reference: existing `GET /configuration/applications/roles` (lines 2881-2902)

**Tests:** None (spec-only story). Validate YAML is well-formed.

---

### Story 1.1 - BE: List Application Members (UseCase)

**Dependencies:** Story 1.0

#### Endpoint Info

```
GET /applications/{applicationId}/membersV2
type: REWRITTEN_EXISTING
old: GET /applications/{applicationId}/members (ApplicationMembersResource.getMembersByApplicationId)
new: GET /applications/{applicationId}/membersV2 (ApplicationMembersResourceV2)
Source: Portal legacy -> Portal Onion
```

Unchanged endpoints used: None

#### Subtasks

**1.1.1 - Create GetApplicationMembersUseCase**
- Create: `gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/application_member/use_case/GetApplicationMembersUseCase.java`
- Pattern: `@UseCase`, `@RequiredArgsConstructor`, `record Input(String applicationId, String environmentId, String query, int page, int size)`, `record Output(List<Member> members, long totalElements)`
- Delegates to `MemberQueryService.getMembersByReference(APPLICATION, applicationId)`
- Optionally filters by `query` (name/email substring match)
- Reference pattern: `GetClusterMembersUseCase.java` (`core/cluster/use_case/members/`)

**1.1.2 - Create ApplicationMembersResourceV2 REST resource**
- Create: `gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/ApplicationMembersResourceV2.java`
- `@Inject GetApplicationMembersUseCase`
- `@GET` method, `@Permissions({ @Permission(value = APPLICATION_MEMBER, acls = READ) })`
- Maps domain `Member` to generated `MemberV2` via mapper
- Reference pattern: `PortalNavigationItemResource.java`

**1.1.3 - Register V2 sub-resource on ApplicationResource**
- Modify: `gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/ApplicationResource.java`
- Add `@Path("membersV2")` method returning `ApplicationMembersResourceV2` via `resourceContext.getResource()`
- Reference: existing `@Path("members")` that returns `ApplicationMembersResource`

**1.1.4 - Create MemberV2Mapper**
- Create: `gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/mapper/MemberV2Mapper.java`
- MapStruct `@Mapper` (follow `PortalNavigationItemMapper` pattern, not legacy `MemberMapper` Spring `@Component`)
- Maps `Member` (core domain) -> `MemberV2` (generated portal rest model)

**1.1.5 - Write UseCase unit test**
- Create: `gravitee-apim-rest-api-service/src/test/java/io/gravitee/apim/core/application_member/use_case/GetApplicationMembersUseCaseTest.java`
- Use `MemberQueryServiceInMemory` (from `inmemory/`)
- Test: returns members, empty list, pagination, search filter, permission propagation
- Reference pattern: `GetClusterMembersUseCaseTest.java`

**1.1.6 - Write REST resource integration test**
- Create: `gravitee-apim-rest-api-portal-rest/src/test/java/io/gravitee/rest/api/portal/rest/resource/ApplicationMembersResourceV2Test.java`
- Extend `AbstractResourceTest`
- Use `MemberQueryServiceInMemory` from `InMemoryConfiguration`
- Test: 200 with data, 200 empty, 403 no permission, pagination params
- Reference pattern: `PortalNavigationItemResourceTest.java`

---

### Story 1.2 - BE: List Application Roles (UseCase)

**Dependencies:** Story 1.0

#### Endpoint Info

```
GET /configuration/applications/rolesV2
type: REWRITTEN_EXISTING
old: GET /configuration/applications/roles (ConfigurationResource.getApplicationRoles)
new: GET /configuration/applications/rolesV2 (ApplicationRolesResourceV2)
Source: Portal legacy -> Portal Onion
```

#### Subtasks

**1.2.1 - Create GetApplicationRolesUseCase**
- Create: `gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/application_member/use_case/GetApplicationRolesUseCase.java`
- `record Input(String organizationId)`, `record Output(List<Role> roles)`
- Delegates to `RoleQueryService` (from `core/membership/query_service/`) — needs a new method `findByScope(Role.Scope scope, String organizationId)` or use existing `findByScopeAndName` iteratively
- Reference: `ConfigurationResource.getApplicationRoles()` for business logic

**1.2.2 - Add `findByScope` to RoleQueryService if missing**
- Modify: `gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/membership/query_service/RoleQueryService.java`
- Add: `Set<Role> findByScope(Role.Scope scope, String organizationId)`
- Implement in: `gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/infra/query_service/membership/RoleQueryServiceImpl.java`
- Add to: `inmemory/RoleQueryServiceInMemory.java`

**1.2.3 - Create ApplicationRolesResourceV2 or add to existing resource**
- Create: `gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/ApplicationRolesResourceV2.java`
- Or add `@Path("applications/rolesV2")` to `ConfigurationResource` (less clean)
- Preferred: new resource class registered under `/configuration/`
- `@Inject GetApplicationRolesUseCase`, `@GET` method

**1.2.4 - Write UseCase unit test**
- Create: `gravitee-apim-rest-api-service/src/test/java/io/gravitee/apim/core/application_member/use_case/GetApplicationRolesUseCaseTest.java`
- Use `RoleQueryServiceInMemory`

**1.2.5 - Write REST resource test**
- Create or extend test for the new resource
- Test: returns roles list, authenticated-only access

---

### Story 1.3 - FE: Members Tab & Routing

**Dependencies:** None

#### Subtasks

**1.3.1 - Create ApplicationTabMembersComponent shell**
- Create: `gravitee-apim-portal-webui-next/src/app/applications/application/application-tab-members/application-tab-members.component.ts`
- Create: `...application-tab-members.component.html`
- Create: `...application-tab-members.component.scss`
- Standalone component, minimal shell (placeholder content)
- Reference pattern: `application-tab-settings/application-tab-settings.component.ts`

**1.3.2 - Add route for members tab**
- Modify: `gravitee-apim-portal-webui-next/src/app/app.routes.ts`
- Add `{ path: 'members', component: ApplicationTabMembersComponent, data: { breadcrumb: { skip: true } } }` under `:applicationId` children
- Reference: existing `settings` route (line ~241)

**1.3.3 - Add tab link in application component**
- Modify: `gravitee-apim-portal-webui-next/src/app/applications/application/application.component.html`
- Add `<a mat-tab-link [routerLink]="['.', 'members']" routerLinkActive #membersRla="routerLinkActive" [active]="membersRla.isActive" i18n="@@applicationTabMembers">Members</a>`
- Conditionally render based on `MEMBER` permission `R`
- Modify: `application.component.ts` to import permission check
- Reference: existing tab links for `logs` and `settings`

**1.3.4 - Write component spec**
- Create: `...application-tab-members.component.spec.ts`
- Test: component renders, route is active
- Reference pattern: `application-tab-settings.component.*.spec.ts` (uses `HttpClientTestingModule`, `AppTestingModule`, `NoopAnimationsModule`)

---

### Story 1.4 - FE: Members Table

**Dependencies:** Story 1.3

#### Subtasks

**1.4.1 - Create ApplicationMembersService**
- Create: `gravitee-apim-portal-webui-next/src/services/application-members.service.ts`
- Methods: `list(applicationId, page?, size?, query?): Observable<MembersV2Response>`
- Uses `HttpClient` + `ConfigService.baseURL`
- Reference pattern: `application.service.ts`, `subscription.service.ts`

**1.4.2 - Write service spec**
- Create: `gravitee-apim-portal-webui-next/src/services/application-members.service.spec.ts`
- Test: URL construction, query params, HTTP method
- Reference pattern: `application.service.spec.ts`

**1.4.3 - Create member table column definitions and data model**
- Create: `...application-tab-members/application-tab-members.model.ts` (if needed for view-specific types)
- Define `TableColumn[]` for: Name, Type, Status, Role, Actions
- Map API response to row model

**1.4.4 - Implement members table in component**
- Modify: `...application-tab-members.component.ts` and `.html`
- Add: search bar (`mat-form-field` with `matInput`), `app-paginated-table`, empty state
- Add: header with "Members" title + "Transfer ownership" button + "Add members" `mat-menu` dropdown
- Imports: `MatMenu`, `MatMenuItem`, `MatMenuTrigger` (follow `user-avatar.component.html` pattern)
- Use `rxResource` or signals + service for data fetching
- Reference: `subscriptions.component.html` for empty state, `user-avatar.component.html` for `mat-menu`

**1.4.5 - Create member fixture for FE tests**
- Create: `gravitee-apim-portal-webui-next/src/entities/application-members/application-members.fixture.ts`
- Provide `fakeMember()`, `fakeMembersResponse()`
- Reference: `gravitee-apim-portal-webui-next/src/entities/application/application.fixture.ts`

**1.4.6 - Write component spec for table rendering**
- Modify: `...application-tab-members.component.spec.ts`
- Test: table renders with data, empty state shows when no members, search triggers refetch, header buttons present
- Reference: `subscriptions.component.spec.ts` if it exists, or `application-tab-settings.component.*.spec.ts`

---

### Story 1.5 - FE: Extend Paginated Table for Custom Action Cells

**Dependencies:** Story 1.4

#### Subtasks

**1.5.1 - Add `actions` column type to TableColumn**
- Modify: `gravitee-apim-portal-webui-next/src/components/paginated-table/paginated-table.component.ts`
- Extend: `TableColumn.type` to `'text' | 'date' | 'actions'`
- Add: `@Output() actionClick` or use content projection (`ng-template`) for action cells
- Reference: current `paginated-table.component.html` column rendering switch

**1.5.2 - Update paginated-table template for actions**
- Modify: `gravitee-apim-portal-webui-next/src/components/paginated-table/paginated-table.component.html`
- Add a new case in the column cell template for `actions` type
- Ensure action clicks stop event propagation (prevent `routerLink` navigation)

**1.5.3 - Wire action column in members table**
- Modify: `...application-tab-members.component.html`
- Pass action column definition, handle `actionClick` events for Edit/Delete

**1.5.4 - Write paginated-table test**
- Create: `gravitee-apim-portal-webui-next/src/components/paginated-table/paginated-table.component.spec.ts`
- Test: actions column renders, click emits event, existing text/date columns unaffected

---

## Phase 2: Edit & Delete Members

---

### Story 2.1 - BE: Update Member Role (UseCase)

**Dependencies:** Story 1.1

#### Endpoint Info

```
PUT /applications/{applicationId}/membersV2/{memberId}
type: REWRITTEN_EXISTING
old: PUT /applications/{applicationId}/members/{memberId} (ApplicationMembersResource.updateApplicationMemberByApplicationIdAndMemberId)
new: PUT /applications/{applicationId}/membersV2/{memberId} (ApplicationMembersResourceV2)
Source: Portal legacy -> Portal Onion
```

#### Subtasks

**2.1.1 - Create UpdateApplicationMemberUseCase**
- Create: `gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/application_member/use_case/UpdateApplicationMemberUseCase.java`
- `record Input(String applicationId, String memberId, String roleName, String organizationId)`
- Validates role via `RoleQueryService.findApplicationRole()`
- Rejects `PRIMARY_OWNER` (throw `SinglePrimaryOwnerException`)
- Delegates to `MemberQueryService.updateRoleToMemberOnReference()`
- Reference pattern: `UpdateClusterMemberUseCase.java`

**2.1.2 - Add PUT endpoint to ApplicationMembersResourceV2**
- Modify: `ApplicationMembersResourceV2.java`
- `@PUT @Path("{memberId}")`, `@Permissions({ @Permission(value = APPLICATION_MEMBER, acls = UPDATE) })`
- Accepts `MemberV2Input` body, calls UseCase, returns updated `MemberV2`

**2.1.3 - Write UseCase unit test**
- Create: `...use_case/UpdateApplicationMemberUseCaseTest.java`
- Test: success, role not found, PRIMARY_OWNER rejected, member not found
- Reference: `UpdateClusterMemberUseCaseTest.java`

**2.1.4 - Write REST resource test**
- Add to: `ApplicationMembersResourceV2Test.java` (nested class `UpdateMemberTest`)
- Test: 200 success, 400 bad role, 403 no permission

---

### Story 2.2 - BE: Delete Application Member (UseCase)

**Dependencies:** Story 1.1

#### Endpoint Info

```
DELETE /applications/{applicationId}/membersV2/{memberId}
type: REWRITTEN_EXISTING
old: DELETE /applications/{applicationId}/members/{memberId} (ApplicationMembersResource.deleteApplicationMember)
new: DELETE /applications/{applicationId}/membersV2/{memberId} (ApplicationMembersResourceV2)
Source: Portal legacy -> Portal Onion
```

#### Subtasks

**2.2.1 - Create DeleteApplicationMemberUseCase**
- Create: `...use_case/DeleteApplicationMemberUseCase.java`
- `record Input(String applicationId, String memberId)`
- Validates member is not PRIMARY_OWNER before deletion
- Delegates to `MemberQueryService.deleteReferenceMember(APPLICATION, applicationId, USER, memberId)`
- Reference pattern: `DeleteClusterMemberUseCase.java`

**2.2.2 - Add DELETE endpoint to ApplicationMembersResourceV2**
- Modify: `ApplicationMembersResourceV2.java`
- `@DELETE @Path("{memberId}")`, `@Permissions(APPLICATION_MEMBER, DELETE)`
- Returns `Response.noContent().build()`

**2.2.3 - Write UseCase unit test**
- Create: `...use_case/DeleteApplicationMemberUseCaseTest.java`
- Test: success (204), cannot delete PRIMARY_OWNER, member not found

**2.2.4 - Write REST resource test**
- Add to: `ApplicationMembersResourceV2Test.java` (nested class `DeleteMemberTest`)

---

### Story 2.3 - FE: Edit Member Role Dialog

**Dependencies:** Story 1.4 (table), Story 1.2 (roles endpoint)

#### Endpoint Info

Uses: `PUT /applications/{applicationId}/membersV2/{memberId}` (Story 2.1)
Uses: `GET /configuration/applications/rolesV2` (Story 1.2, unchanged by this story)

#### Subtasks

**2.3.1 - Create EditMemberRoleDialogComponent**
- Create: `...application-tab-members/edit-member-role-dialog/edit-member-role-dialog.component.ts`
- Create: `...edit-member-role-dialog.component.html`
- `@Inject(MAT_DIALOG_DATA)` receives `{ member, roles }`
- Dropdown (`mat-select`) of roles, pre-selected current role
- `mat-dialog-actions`: Cancel (`[mat-dialog-close]="null"`) and Save (`[mat-dialog-close]="selectedRole"`)
- Reference pattern: `confirm-dialog.component.ts`

**2.3.2 - Add roles service method**
- Modify: `gravitee-apim-portal-webui-next/src/services/application-members.service.ts`
- Add: `listRoles(): Observable<ApplicationRolesV2Response>`
- Add: `updateMemberRole(applicationId, memberId, role): Observable<MemberV2>`

**2.3.3 - Wire edit action in members table**
- Modify: `...application-tab-members.component.ts`
- On Edit click: `matDialog.open(EditMemberRoleDialogComponent, { data: { member, roles } }).afterClosed()` -> call `updateMemberRole()` -> refresh table

**2.3.4 - Create dialog harness**
- Create: `...edit-member-role-dialog/edit-member-role-dialog.harness.ts`
- Provides: `selectRole()`, `save()`, `cancel()`
- Reference pattern: `confirm-dialog.harness.ts`

**2.3.5 - Write dialog spec**
- Create: `...edit-member-role-dialog/edit-member-role-dialog.component.spec.ts`
- Test: renders roles, pre-selects current, save returns new role, cancel returns null

---

### Story 2.4 - FE: Delete Member Dialog

**Dependencies:** Story 1.4 (table)

#### Endpoint Info

Uses: `DELETE /applications/{applicationId}/membersV2/{memberId}` (Story 2.2)

#### Subtasks

**2.4.1 - Wire delete action in members table**
- Modify: `...application-tab-members.component.ts`
- On Delete click: `matDialog.open(ConfirmDialogComponent, { role: 'alertdialog', data: { title, content, confirmLabel, cancelLabel } }).afterClosed()` -> call `deleteMember()` -> refresh
- Reference pattern: `application-tab-settings.component.ts` delete flow

**2.4.2 - Add delete service method**
- Modify: `application-members.service.ts`
- Add: `deleteMember(applicationId, memberId): Observable<void>`

**2.4.3 - Write delete flow spec**
- Add to: `application-tab-members.component.spec.ts`
- Test: delete button opens confirm dialog, confirm calls API, table refreshes
- Reference: `application-tab-settings.component.delete.spec.ts`

---

## Phase 3: Add Members

---

### Story 3.1 - BE: Search Users for Application (UseCase)

**Dependencies:** Story 1.1

#### Endpoint Info

```
POST /applications/{applicationId}/membersV2/_search-users?q={query}
type: NEW_NOT_BASED_ON_EXISTING
Note: Existing POST /users/_search requires ORGANIZATION_USERS[READ] (too restrictive).
This new endpoint uses APPLICATION_MEMBER[CREATE] permission and is scoped to the application.
```

#### Subtasks

**3.1.1 - Create SearchUsersForApplicationMemberUseCase**
- Create: `...use_case/SearchUsersForApplicationMemberUseCase.java`
- `record Input(String applicationId, String query, String environmentId)`
- `record Output(List<SearchableUser> users)` (or a dedicated `SearchUserResult` model)
- Delegates to `IdentityService.search(query)` for user lookup
- Filters out users already members via `MemberQueryService.getMembersByReference()`
- Reference: `UsersResource.getUsers()` for `IdentityService` usage pattern

**3.1.2 - Create ApplicationMemberUserQueryService (if needed)**
- If `IdentityService` is too legacy, create a core query service interface wrapping it
- Create: `...query_service/ApplicationMemberUserQueryService.java`
- Implement in infra: `ApplicationMemberUserQueryServiceImpl.java` delegating to `IdentityService`

**3.1.3 - Add POST _search-users endpoint to ApplicationMembersResourceV2**
- Modify: `ApplicationMembersResourceV2.java`
- `@POST @Path("_search-users")`, `@Permissions(APPLICATION_MEMBER, CREATE)`
- Query param `q` for search text

**3.1.4 - Write UseCase unit test**
- Create: `...use_case/SearchUsersForApplicationMemberUseCaseTest.java`
- Test: returns matching users, excludes existing members, empty query returns all

**3.1.5 - Write REST resource test**
- Add to: `ApplicationMembersResourceV2Test.java` (nested class `SearchUsersTest`)

---

### Story 3.2 - BE: Add Application Member (UseCase)

**Dependencies:** Story 1.1

#### Endpoint Info

```
POST /applications/{applicationId}/membersV2
type: REWRITTEN_EXISTING
old: POST /applications/{applicationId}/members (ApplicationMembersResource.createApplicationMember)
new: POST /applications/{applicationId}/membersV2 (ApplicationMembersResourceV2)
Source: Portal legacy -> Portal Onion
```

#### Subtasks

**3.2.1 - Create AddApplicationMemberUseCase**
- Create: `...use_case/AddApplicationMemberUseCase.java`
- `record Input(String applicationId, List<AddMemberRequest> members, boolean notify, String environmentId, String organizationId)`
- `record AddMemberRequest(String userId, String reference, String roleName)`
- Validates role via `RoleQueryService`, rejects `PRIMARY_OWNER`
- Delegates to `MembershipDomainService.createNewMembership()` per member
- Reference pattern: `AddClusterMemberUseCase.java`, plus existing `ApplicationMembersResource.createApplicationMember()` for business rules

**3.2.2 - Add POST endpoint to ApplicationMembersResourceV2**
- Modify: `ApplicationMembersResourceV2.java`
- `@POST`, `@Permissions(APPLICATION_MEMBER, CREATE)`
- Accepts `MemberV2Input` (or batch variant)
- Optional query param `notify=true/false`

**3.2.3 - Write UseCase unit test**
- Create: `...use_case/AddApplicationMemberUseCaseTest.java`
- Test: single add, batch add, duplicate member rejected, PRIMARY_OWNER rejected, invalid role

**3.2.4 - Write REST resource test**
- Add to: `ApplicationMembersResourceV2Test.java` (nested class `AddMemberTest`)

---

### Story 3.3 - FE: Search Users Dialog (Add Members)

**Dependencies:** Story 1.4

#### Endpoint Info

Uses: `POST /applications/{applicationId}/membersV2/_search-users` (Story 3.1)
Uses: `POST /applications/{applicationId}/membersV2` (Story 3.2)
Uses: `GET /configuration/applications/rolesV2` (Story 1.2)

#### Subtasks

**3.3.1 - Create SearchUsersDialogComponent**
- Create: `...application-tab-members/search-users-dialog/search-users-dialog.component.ts`
- Create: `...search-users-dialog.component.html`
- Create: `...search-users-dialog.component.scss`
- Dialog with: role `mat-select`, user search `mat-form-field` (autocomplete), `mat-chip-set` for selections, notify checkbox
- `mat-dialog-actions`: Cancel / Add members
- Uses `ReactiveFormsModule`
- Reference: `more-filters-dialog.component.ts` for complex dialog pattern

**3.3.2 - Add search and add service methods**
- Modify: `application-members.service.ts`
- Add: `searchUsers(applicationId, query): Observable<SearchUsersV2Response>`
- Add: `addMembers(applicationId, members, notify): Observable<MemberV2[]>`

**3.3.3 - Wire "Search users" menu item to dialog**
- Modify: `...application-tab-members.component.ts`
- `mat-menu-item` "Search users" click -> `matDialog.open(SearchUsersDialogComponent, ...)` -> on result, call `addMembers()` -> refresh table

**3.3.4 - Create dialog harness**
- Create: `...search-users-dialog/search-users-dialog.harness.ts`

**3.3.5 - Write dialog spec**
- Create: `...search-users-dialog/search-users-dialog.component.spec.ts`
- Test: search triggers API call, users added to chipset, remove from chipset, submit sends correct payload

**3.3.6 - Write integration spec for add members flow**
- Add to: `application-tab-members.component.spec.ts`
- Test: "Search users" menu item opens dialog, dialog result triggers add API call, table refreshes

---

## Phase 4: Invite Users via Email

---

### Story 4.1 - BE: Invite User to Application (UseCase)

**Dependencies:** Story 1.1

#### Endpoint Info

```
POST /applications/{applicationId}/membersV2/_invite
type: NEW_BASED_ON_EXISTING
based_on: POST /configuration/groups/{groupId}/invitations (GroupInvitationsResource.createGroupInvitation)
Uses InvitationService.create() with InvitationReferenceType.APPLICATION
```

#### Subtasks

**4.1.1 - Create InviteApplicationMemberUseCase**
- Create: `...use_case/InviteApplicationMemberUseCase.java`
- `record Input(String applicationId, String email, String roleName, String environmentId, String organizationId)`
- Creates `NewInvitationEntity` with `referenceType=APPLICATION`, `referenceId=applicationId`, `applicationRole=roleName`
- Delegates to `InvitationService.create()` (legacy service, wrapped if needed)
- Reference: `GroupInvitationsResource.createGroupInvitation()` for `InvitationService` usage pattern

**4.1.2 - Create InvitationServiceWrapper (if needed for core layer)**
- If `InvitationService` is too legacy for direct UseCase usage, create a domain service wrapper
- Create: `...domain_service/ApplicationInvitationDomainService.java` in `core/application_member/`
- Implement in infra as legacy wrapper

**4.1.3 - Add POST _invite endpoint to ApplicationMembersResourceV2**
- Modify: `ApplicationMembersResourceV2.java`
- `@POST @Path("_invite")`, `@Permissions(APPLICATION_MEMBER, CREATE)`
- Accepts `InvitationInput` body

**4.1.4 - Write UseCase unit test**
- Create: `...use_case/InviteApplicationMemberUseCaseTest.java`
- Test: creates invitation, sends email, validates email format

**4.1.5 - Write REST resource test**
- Add to: `ApplicationMembersResourceV2Test.java` (nested class `InviteUserTest`)

---

### Story 4.2 - BE: List Pending Invitations (UseCase)

**Dependencies:** Story 4.1

#### Endpoint Info

```
GET /applications/{applicationId}/membersV2/_invitations
type: NEW_NOT_BASED_ON_EXISTING
Uses InvitationService.findByReference(APPLICATION, applicationId)
```

#### Subtasks

**4.2.1 - Create GetApplicationInvitationsUseCase**
- Create: `...use_case/GetApplicationInvitationsUseCase.java`
- `record Input(String applicationId)`, `record Output(List<InvitationEntity> invitations)`
- Delegates to `InvitationService.findByReference(APPLICATION, applicationId)`

**4.2.2 - Add GET _invitations endpoint**
- Modify: `ApplicationMembersResourceV2.java`
- `@GET @Path("_invitations")`, `@Permissions(APPLICATION_MEMBER, READ)`

**4.2.3 - Write UseCase unit test**
- Create: `...use_case/GetApplicationInvitationsUseCaseTest.java`

**4.2.4 - Write REST resource test**
- Add to: `ApplicationMembersResourceV2Test.java` (nested class `ListInvitationsTest`)

---

### Story 4.3 - BE: Delete Pending Invitation (UseCase)

**Dependencies:** Story 4.2

#### Endpoint Info

```
DELETE /applications/{applicationId}/membersV2/_invitations/{invitationId}
type: NEW_NOT_BASED_ON_EXISTING
Uses InvitationService.delete(invitationId, applicationId)
```

#### Subtasks

**4.3.1 - Create DeleteApplicationInvitationUseCase**
- Create: `...use_case/DeleteApplicationInvitationUseCase.java`
- `record Input(String applicationId, String invitationId)`
- Validates invitation belongs to application
- Delegates to `InvitationService.delete()`

**4.3.2 - Add DELETE _invitations/{invitationId} endpoint**
- Modify: `ApplicationMembersResourceV2.java`
- `@DELETE @Path("_invitations/{invitationId}")`, `@Permissions(APPLICATION_MEMBER, DELETE)`

**4.3.3 - Write UseCase unit test**
- Create: `...use_case/DeleteApplicationInvitationUseCaseTest.java`

**4.3.4 - Write REST resource test**
- Add to: `ApplicationMembersResourceV2Test.java`

---

### Story 4.4 - BE: Update Invited Member Role (UseCase)

**Dependencies:** Story 4.2

#### Endpoint Info

```
PUT /applications/{applicationId}/membersV2/_invitations/{invitationId}
type: NEW_NOT_BASED_ON_EXISTING
Uses InvitationService.update() with UpdateInvitationEntity
```

#### Subtasks

**4.4.1 - Create UpdateApplicationInvitationUseCase**
- Create: `...use_case/UpdateApplicationInvitationUseCase.java`
- `record Input(String applicationId, String invitationId, String roleName)`
- Builds `UpdateInvitationEntity` with new `applicationRole`
- Delegates to `InvitationService.update()`

**4.4.2 - Add PUT _invitations/{invitationId} endpoint**
- Modify: `ApplicationMembersResourceV2.java`

**4.4.3 - Write UseCase unit test**
- Create: `...use_case/UpdateApplicationInvitationUseCaseTest.java`

**4.4.4 - Write REST resource test**
- Add to: `ApplicationMembersResourceV2Test.java`

---

### Story 4.5 - BE: Invitation Acceptance to Membership Flow

**Dependencies:** Story 4.1

#### Endpoint Info

```
Not a new REST endpoint. Modifies existing registration/acceptance flow.
type: NEW_NOT_BASED_ON_EXISTING
Uses InvitationService.addMember() which already supports APPLICATION reference type.
```

#### Subtasks

**4.5.1 - Investigate existing acceptance flow**
- Read: `InvitationService.addMember()` implementation
- Read: Registration flow to find where invitations are processed
- Determine if `InvitationReferenceType.APPLICATION` is already handled end-to-end

**4.5.2 - Implement or fix application invitation acceptance**
- If `addMember()` already handles APPLICATION type: verify and add tests
- If not: extend the acceptance flow to create `APPLICATION` membership from invitation
- Modify: `InvitationServiceImpl` or create a new domain service

**4.5.3 - Write integration test**
- Test: invitation created -> user registers/accepts -> user is now an active application member -> invitation is consumed

---

### Story 4.6 - BE: Unified Members + Invitations Query (UseCase)

**Dependencies:** Story 1.1, Story 4.2

#### Endpoint Info

```
GET /applications/{applicationId}/membersV2?includeInvitations=true
type: REWRITTEN_EXISTING (enhances Story 1.1 endpoint)
Combines: GET .../membersV2 + GET .../membersV2/_invitations into one response
```

#### Subtasks

**4.6.1 - Create GetApplicationMembersAndInvitationsUseCase**
- Create: `...use_case/GetApplicationMembersAndInvitationsUseCase.java`
- Or extend `GetApplicationMembersUseCase` to accept `includeInvitations` flag
- Fetches members via `MemberQueryService` + invitations via `InvitationService.findByReference()`
- Merges into unified list with `status` (active/pending) and `type` (user/invited) fields
- Supports pagination across both, search filter on name/email

**4.6.2 - Update ApplicationMembersResourceV2 GET endpoint**
- Modify: `ApplicationMembersResourceV2.java`
- Add optional query param `includeInvitations` (default false for backward compat)
- When true, uses unified UseCase

**4.6.3 - Write UseCase unit test**
- Create: `...use_case/GetApplicationMembersAndInvitationsUseCaseTest.java`
- Test: members only, members + invitations, search across both, pagination

**4.6.4 - Write REST resource test**
- Add to: `ApplicationMembersResourceV2Test.java`

---

### Story 4.7 - FE: Invite User Dialog

**Dependencies:** Story 1.4

#### Endpoint Info

Uses: `POST /applications/{applicationId}/membersV2/_invite` (Story 4.1)
Uses: `GET /configuration/applications/rolesV2` (Story 1.2)

#### Subtasks

**4.7.1 - Create InviteUserDialogComponent**
- Create: `...application-tab-members/invite-user-dialog/invite-user-dialog.component.ts`
- Create: `...invite-user-dialog.component.html`
- Dialog with: role `mat-select`, email `mat-form-field` (text input with email validation)
- `mat-dialog-actions`: Cancel / Invite member
- Reference pattern: `confirm-dialog.component.ts`

**4.7.2 - Add invite service method**
- Modify: `application-members.service.ts`
- Add: `inviteMember(applicationId, email, role): Observable<InvitationV2>`

**4.7.3 - Wire "Invite users" menu item to dialog**
- Modify: `...application-tab-members.component.ts`
- `mat-menu-item` "Invite users" click -> `matDialog.open(InviteUserDialogComponent, ...)` -> on result, call `inviteMember()` -> refresh table, show snackbar

**4.7.4 - Create dialog harness**
- Create: `...invite-user-dialog/invite-user-dialog.harness.ts`

**4.7.5 - Write dialog spec**
- Create: `...invite-user-dialog/invite-user-dialog.component.spec.ts`
- Test: email validation, role selection, submit sends correct payload

---

### Story 4.8 - FE: Display Invited Members in Table

**Dependencies:** Story 1.4, Story 4.6

#### Endpoint Info

Uses: `GET /applications/{applicationId}/membersV2?includeInvitations=true` (Story 4.6)
Uses: `PUT .../membersV2/_invitations/{invitationId}` (Story 4.4)
Uses: `DELETE .../membersV2/_invitations/{invitationId}` (Story 4.3)

#### Subtasks

**4.8.1 - Update members service to pass includeInvitations**
- Modify: `application-members.service.ts`
- Update `list()` to accept `includeInvitations` param

**4.8.2 - Add invitation-specific service methods**
- Modify: `application-members.service.ts`
- Add: `updateInvitationRole(applicationId, invitationId, role): Observable<InvitationV2>`
- Add: `deleteInvitation(applicationId, invitationId): Observable<void>`

**4.8.3 - Update table to render unified data**
- Modify: `...application-tab-members.component.ts` and `.html`
- Enable `includeInvitations=true` in service call
- Status column renders "Active" / "Pending" badges
- Type column shows "User" / "Invited"
- Name column shows email for pending invitations (no full name available)
- Edit action: if pending -> update invitation role; if active -> update member role
- Delete action: if pending -> delete invitation; if active -> delete member

**4.8.4 - Write spec for unified table**
- Add to: `application-tab-members.component.spec.ts`
- Test: renders both active and pending rows, correct actions per row type, search works across both

---

## Phase 5: Transfer Ownership

---

### Story 5.1 - BE: Transfer Application Ownership (UseCase)

**Dependencies:** Story 1.1, Story 3.1

#### Endpoint Info

```
POST /applications/{applicationId}/membersV2/_transfer-ownership
type: REWRITTEN_EXISTING
old: POST /applications/{applicationId}/members/_transfer_ownership (ApplicationMembersResource.transferMemberOwnership)
new: POST /applications/{applicationId}/membersV2/_transfer-ownership (ApplicationMembersResourceV2)
Source: Portal legacy -> Portal Onion
```

#### Subtasks

**5.1.1 - Create TransferApplicationOwnershipUseCase**
- Create: `...use_case/TransferApplicationOwnershipUseCase.java`
- `record Input(String applicationId, String newPrimaryOwnerId, String newPrimaryOwnerReference, String previousOwnerNewRole, String organizationId)`
- Validates new role is not PRIMARY_OWNER
- Resolves role via `RoleQueryService.findApplicationRole()`
- Delegates to `MembershipDomainService.transferOwnership()` with `RoleScope.APPLICATION`
- Reference pattern: `TransferClusterOwnershipUseCase.java`
- Reference business logic: `ApplicationMembersResource.transferMemberOwnership()` (lines 207-235)

**5.1.2 - Add POST _transfer-ownership endpoint**
- Modify: `ApplicationMembersResourceV2.java`
- `@POST @Path("_transfer-ownership")`, `@Permissions(APPLICATION_MEMBER, UPDATE)`
- Accepts `TransferOwnershipV2Input` body

**5.1.3 - Write UseCase unit test**
- Create: `...use_case/TransferApplicationOwnershipUseCaseTest.java`
- Test: successful transfer, invalid role, target user not found, cannot set previous owner to PRIMARY_OWNER
- Reference: `TransferClusterOwnershipUseCaseTest.java`

**5.1.4 - Write REST resource test**
- Add to: `ApplicationMembersResourceV2Test.java` (nested class `TransferOwnershipTest`)

---

### Story 5.2 - FE: Transfer Ownership Dialog

**Dependencies:** Story 1.4, Story 1.2 (roles), Story 3.1 (user search)

#### Endpoint Info

Uses: `POST /applications/{applicationId}/membersV2/_transfer-ownership` (Story 5.1)
Uses: `GET /applications/{applicationId}/membersV2` (Story 1.1, for current members list)
Uses: `POST /applications/{applicationId}/membersV2/_search-users` (Story 3.1, for searching other users)
Uses: `GET /configuration/applications/rolesV2` (Story 1.2)

#### Subtasks

**5.2.1 - Create TransferOwnershipDialogComponent**
- Create: `...application-tab-members/transfer-ownership-dialog/transfer-ownership-dialog.component.ts`
- Create: `...transfer-ownership-dialog.component.html`
- Create: `...transfer-ownership-dialog.component.scss`
- Toggle buttons (`mat-button-toggle-group`): "Application member", "Other user", "Primary owner"
- Default: "Application member"
- Role dropdown (`mat-select`) for previous owner's new role
- User selection: `mat-select` (app members) or search input (other users) depending on mode
- Warning text shown when "Primary owner" selected
- `mat-dialog-actions`: Cancel / Transfer
- Reference: `mat-button-toggle-group` usage in codebase (search for existing examples)

**5.2.2 - Add transfer service method**
- Modify: `application-members.service.ts`
- Add: `transferOwnership(applicationId, newOwnerId, newOwnerReference, previousOwnerRole): Observable<void>`

**5.2.3 - Wire "Transfer ownership" button to dialog**
- Modify: `...application-tab-members.component.ts`
- Button click -> `matDialog.open(TransferOwnershipDialogComponent, { data: { members, roles } })` -> on result, call `transferOwnership()` -> refresh table

**5.2.4 - Create dialog harness**
- Create: `...transfer-ownership-dialog/transfer-ownership-dialog.harness.ts`

**5.2.5 - Write dialog spec**
- Create: `...transfer-ownership-dialog/transfer-ownership-dialog.component.spec.ts`
- Test: toggle switches modes, "Application member" shows member dropdown, "Other user" shows search, "Primary owner" shows warning, submit sends correct payload

**5.2.6 - Write integration spec for transfer flow**
- Add to: `application-tab-members.component.spec.ts`
- Test: transfer button opens dialog, dialog result triggers API call, table refreshes
