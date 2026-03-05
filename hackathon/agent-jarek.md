# Agent Log - Jarek

## Brief Summary of What Was Implemented

- Implemented **Phase 1 / Story 1.0** API-contract groundwork in `portal-openapi.yaml` for the V2 members scope and `rolesV2`.
- Implemented **Phase 1 / Story 1.1** backend listing flow for application members in Onion style:
  - `GetApplicationMembersUseCase`
  - `ApplicationMembersResourceV2` + registration under application resource
  - mapper and tests aligned with existing patterns.
- Implemented **Phase 1 / Story 1.2** backend listing flow for application roles in Onion style:
  - `GetApplicationRolesUseCase`
  - `RoleQueryService.findByScope(...)` in core + infra + in-memory implementation
  - `ApplicationRolesResourceV2` + registration under configuration resource
  - use case and REST tests added and passing in module-scoped runs.
- Implemented **Phase 2 / Story 2.1** backend update-member-role flow in Onion style:
  - `UpdateApplicationMemberUseCase` with role validation and `PRIMARY_OWNER` rejection
  - `PUT /applications/{applicationId}/membersV2/{memberId}` in `ApplicationMembersResourceV2`
  - use case tests (success, role not found, primary owner rejected, member not found)
  - REST tests for update flow (`200`, `400`, `403`) in `ApplicationMembersResourceV2Test`.
- Implemented **Phase 2 / Story 2.2** backend delete-member flow in Onion style:
  - `DeleteApplicationMemberUseCase` with member existence check and `PRIMARY_OWNER` deletion rejection
  - `DELETE /applications/{applicationId}/membersV2/{memberId}` in `ApplicationMembersResourceV2`
  - use case tests (success, primary owner rejected, member not found)
  - REST tests for delete flow (`204`, `400`, `403`) in `ApplicationMembersResourceV2Test`.
- Implemented **Phase 3 / Story 3.1** backend search-users flow in Onion style:
  - `SearchUsersForApplicationMemberUseCase`
  - `ApplicationMemberUserQueryService` + legacy wrapper over `IdentityService.search(...)`
  - `POST /applications/{applicationId}/membersV2/_search-users` in `ApplicationMembersResourceV2`
  - existing application members are excluded from search results
  - use case tests (matching users, exclude existing members, empty query)
  - REST tests for search flow (`200`, exclusion behavior, `403` on missing permission) in `ApplicationMembersResourceV2Test`.

## Key Decisions Made and Why

- **Used V2 endpoints instead of modifying legacy endpoints** to avoid regressions and preserve backward compatibility for existing portal consumers.
- **Kept business logic in UseCases** and resources as thin adapters to stay consistent with Onion architecture and improve testability.
- **Extended `RoleQueryService` with `findByScope`** instead of ad-hoc lookups, because this keeps role retrieval reusable and avoids duplicated query logic.
- **Reused in-memory test doubles** (`RoleQueryServiceInMemory`, etc.) to match existing test style and keep tests fast/deterministic.
- **Used `NotFoundDomainException` in the new use case when update returns null** so missing members are mapped as proper domain-level 404 in V2 flow.
- **Kept the legacy consistency check (`memberInput.user` must match path `memberId`)** in V2 update endpoint to preserve behavior and avoid silent mismatches.
- **Used the same `PRIMARY_OWNER` protection for delete as for update** to preserve ownership safety invariants across member-management actions.
- **Returned `204 No Content` from V2 delete endpoint** to align with REST semantics and existing Gravitee endpoint behavior.
- **Introduced a dedicated application-member user query service** instead of using `IdentityService` directly in use case to keep Onion boundaries clean and testable.
- **Normalized blank search query to `*`** to keep behavior consistent with existing users search and return full candidate set when no filter is provided.
- **Excluded existing application members in use case layer** so endpoint always returns addable users only, independent of frontend filtering.

## Gotchas or Surprises

- `prettier` checks in `portal-rest` failed on newly added files until `prettier:write` was run.
- `rg` was unavailable in this environment, so fallback shell tools (`grep`, `find`) were used for lookups.
- Running a single REST test in this module still triggers heavy OpenAPI generation/compilation before test execution.
- Portal REST module initially failed to compile against the new use case until the updated service module artifact was installed locally (`mvn ...service -DskipTests install`).
- Maven run for a single resource test still performs expensive OpenAPI generation and broad module compilation, so feedback loops are slower than expected.
- `portal-rest` compile failed until the updated `service` module was installed locally after adding new core classes (`mvn ...service -DskipTests install`).

## Blockers / Open Questions

- No functional blockers at the moment.
- Open question: none currently.

## Prompts That Were Particularly Effective (Exact Text)

```text
odczytaj i przeanalizuj plik  './hackathon/STORIES.md' Before starting, study the existing code that we're evolving. Przygotuj  plan implementacji  dla `Phase 1`  `Story 1.0`
```

```text
wykonaj implementacje na podstawie planu dla Phase 1 / Story 1.0
```

```text
Implement Pahase 1 Story 1.1 from ./hackathon/STORIES.md.
Include tests following the existing patterns in the codebase.
```

```text
Implement Pahase 1 Story 1.2 from ./hackathon/STORIES.md.
Include tests following the existing patterns in the codebase.
```

```text
Implement Pahase 2 Story 2.1 from ./hackathon/STORIES.md. Include tests following the existing patterns in the codebase.
```

```text
Implement Phase 2 Story 2.2 from ./hackathon/STORIES.md. Include tests following the existing patterns in the codebase.
```

```text
Implement Phase 3 Story 3.1 from ./hackathon/STORIES.md. Include tests following the existing patterns in the codebase.
```
