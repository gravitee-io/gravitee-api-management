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

## Key Decisions Made and Why

- **Used V2 endpoints instead of modifying legacy endpoints** to avoid regressions and preserve backward compatibility for existing portal consumers.
- **Kept business logic in UseCases** and resources as thin adapters to stay consistent with Onion architecture and improve testability.
- **Extended `RoleQueryService` with `findByScope`** instead of ad-hoc lookups, because this keeps role retrieval reusable and avoids duplicated query logic.
- **Reused in-memory test doubles** (`RoleQueryServiceInMemory`, etc.) to match existing test style and keep tests fast/deterministic.

## Gotchas or Surprises

- `prettier` checks in `portal-rest` failed on newly added files until `prettier:write` was run.
- `rg` was unavailable in this environment, so fallback shell tools (`grep`, `find`) were used for lookups.
- Running a single REST test in this module still triggers heavy OpenAPI generation/compilation before test execution.

## Blockers / Open Questions

- No functional blockers at the moment.
- Open question for team alignment: should completed checklist items in `PROGRESS.md` consistently use `:white_check_mark:` or mixed styles (`✅` and markdown checkboxes)?

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
