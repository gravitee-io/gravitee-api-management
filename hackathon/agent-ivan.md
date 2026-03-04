# Agent Log - Ivan

## Snapshot (current progress)

### What was implemented

- **Story 1.3 – FE: Members Tab & Routing**
  - `ApplicationTabMembersComponent` shell (standalone): placeholder view with "Members" header, uses signal inputs (`applicationId`, `userApplicationPermissions`).
  - Route: `path: 'members'` under `:applicationId` children in `app.routes.ts`, `data: { breadcrumb: { skip: true } }`.
  - Application nav: "Members" tab link between Analytics & Logs and Settings; shown only when `userApplicationPermissions.MEMBER` includes `'R'`.
  - `application.component.ts`: `@Input() userApplicationPermissions`, `hasMemberReadPermission` derived in `ngOnChanges`.
  - SCSS: theme import `@use '../../../../scss/theme' as app-theme;` per design system.
  - Spec: create, render section, title display; inputs set via `fixture.componentRef.setInput()`.

### Key decisions

- **Signal inputs on new component** — `ApplicationTabMembersComponent` uses `input.required<string>()` and `input.required<UserApplicationPermissions>()` to follow Angular rules; existing `ApplicationComponent` kept on `@Input()` + `OnChanges` to avoid refactoring scope.
- **Permission from route** — `hasMemberReadPermission` is driven by resolver `userApplicationPermissions` and `withComponentInputBinding()`, same pattern as settings tab and `DEFINITION` permissions.
- **Test targeting** — `data-testid="application-tab-members"` for the shell; design system prefers `aria-label` or `data-testid` when no harness exists.

### Gotchas & surprises

- `InvitationReferenceType.APPLICATION` exists in the data model but has no REST API -- the repository supports it, but no one ever built the endpoints.
- `MemberMapper` (portal) is a Spring `@Component` with `@Autowired UserService` -- legacy pattern. New `MemberV2Mapper` should use MapStruct `@Mapper` instead.
- `paginated-table` has an `expand` column with `routerLink` on rows -- action buttons need `stopPropagation()` to avoid triggering navigation.
- Portal REST tests use a mix of mocked services (`AbstractResourceTest` with `@MockBean`) and in-memory alternatives (`InMemoryConfiguration`). New Onion-style resources should use the in-memory approach.
- **New:** Route binding: `userApplicationPermissions` is populated by the parent route’s `applicationPermissionResolver`; `ApplicationComponent` receives it via `withComponentInputBinding()`, so no manual pass-through from a parent template.

### Blockers / open questions

- None.

### Effective prompts

- **Start Story 1.3:**  
  `Read @hackathon/STORIES.md for the full list of user stories. Before starting, study the existing code that we're evolving. For this session I want you to work only on the front end stories. Start implementing Story 1.3 - FE: Members Tab & Routing`

- **Align with rules/screenshots:**  
  `I forgot to provide you with rules and screenshots. Now they are all in place, please adjust the new changes with this context.`

- **Clarify data flow:**  
  `how will hasMemberReadPermission be populated?`
