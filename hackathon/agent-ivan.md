# Agent Log - Ivan

## Snapshot (current progress)

### What was implemented

- **Story 1.3 – FE: Members Tab & Routing**
  - `ApplicationTabMembersComponent` shell (standalone): signal inputs (`applicationId`, `userApplicationPermissions`).
  - Route: `path: 'members'` under `:applicationId` children in `app.routes.ts`.
  - Application nav: "Members" tab link gated by `MEMBER` read permission.
  - `application.component.ts`: `@Input() userApplicationPermissions`, `hasMemberReadPermission` via `ngOnChanges`.
  - Spec: 3 tests (create, render, title).

- **Story 1.4 – FE: Members Table**
  - `ApplicationMembersService` (`services/application-members.service.ts`): `list(applicationId, page?, size?, query?)` → `GET /applications/{id}/membersV2` using `inject(HttpClient)` + `{ params }` pattern.
  - Entity types: `MemberV2`, `MembersV2Response` in `entities/application-members/`.
  - Fixtures: `fakeMember()`, `fakeMembersResponse()` with modifier pattern.
  - Component: `rxResource` for data fetching with signal-based `searchQuery`, `currentPage`, `pageSize`. Search bar, "Members" header, "Transfer Ownership" button, "Add Members" `mat-menu` dropdown with "User Search" (+ "Find registered users") and "Email Invitation" (+ "Invite unregistered users"). `app-paginated-table` for list. Empty states for no-data and filtered-empty.
  - Permission: "Add Members" button gated by `MEMBER` CREATE permission.
  - Service spec: 3 tests (default params, page+size, query).
  - Component spec: 14 tests (render, title, search, buttons, table, empty state, permissions, pagination).

### Key decisions

- **Signal inputs on new component** — `ApplicationTabMembersComponent` uses `input.required<>()` per Angular rules; existing `ApplicationComponent` kept on `@Input()` + `OnChanges` to avoid refactoring scope.
- **`rxResource` for data fetching** — Matches `SubscriptionsComponent` pattern. Signal-based params (`searchQuery`, `currentPage`, `pageSize`) auto-trigger refetch. No manual subscribe/unsubscribe needed.
- **`provideHttpClient()` + `provideHttpClientTesting()` in specs** — Required for `rxResource` tests. The older `HttpClientTestingModule` + `AppTestingModule` approach causes `rxResource` to hang in `await fixture.whenStable()`.
- **Two columns now (Name, Role), five later** — The design shows Name (with avatar+email), Type, Status, Role, Actions. But `paginated-table` only supports `text`/`date`. Story 1.5 adds `actions` column; Stories 4.6/4.8 add Type/Status with unified invitation data.
- **Dropdown descriptions** — Added subtitle text ("Find registered users", "Invite unregistered users") to match the `add-members-dropdown.png` screenshot.

### Gotchas & surprises

- `InvitationReferenceType.APPLICATION` exists in the data model but has no REST API -- the repository supports it, but no one ever built the endpoints.
- `MemberMapper` (portal) is a Spring `@Component` with `@Autowired UserService` -- legacy pattern. New `MemberV2Mapper` should use MapStruct `@Mapper` instead.
- `paginated-table` has an `expand` column with `routerLink` on rows -- action buttons need `stopPropagation()` to avoid triggering navigation.
- Portal REST tests use a mix of mocked services (`AbstractResourceTest` with `@MockBean`) and in-memory alternatives (`InMemoryConfiguration`). New Onion-style resources should use the in-memory approach.
- Route binding: `userApplicationPermissions` is populated by the parent route's `applicationPermissionResolver` via `withComponentInputBinding()`, so no manual pass-through from a parent template.
- **`rxResource` test gotcha:** Using `HttpClientTestingModule` + `AppTestingModule` with `rxResource` causes tests to hang on `await fixture.whenStable()`. Must use `provideHttpClient()` + `provideHttpClientTesting()` + `provideRouter([])` instead. Also need a final `fixture.detectChanges()` after `await fixture.whenStable()` to re-render after signal updates.

### Blockers / open questions

- None.

### Effective prompts

- **Start Story 1.3:**
  `Read @hackathon/STORIES.md for the full list of user stories. Before starting, study the existing code that we're evolving. For this session I want you to work only on the front end stories. Start implementing Story 1.3 - FE: Members Tab & Routing`

- **Align with rules/screenshots:**
  `I forgot to provide you with rules and screenshots. Now they are all in place, please adjust the new changes with this context.`

- **Clarify data flow:**
  `how will hasMemberReadPermission be populated?`

- **Continue to next story:**
  `proceed with story 1.4 - FE: Members Table and run all newly created tests`

- **Screenshot check nudge:**
  `have you checked the screenshots in @hackathon/screenshots/ ?`
