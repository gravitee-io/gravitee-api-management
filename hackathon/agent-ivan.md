# Agent Log - Ivan

## Gotchas & Surprises

- `InvitationReferenceType.APPLICATION` exists in the data model but has no REST API -- the repository supports it, but no one ever built the endpoints.
- `MemberMapper` (portal) is a Spring `@Component` with `@Autowired UserService` -- legacy pattern. New `MemberV2Mapper` should use MapStruct `@Mapper` instead.
- `paginated-table` has an `expand` column with `routerLink` on rows -- action buttons need `stopPropagation()` to avoid triggering navigation.
- Portal REST tests use a mix of mocked services (`AbstractResourceTest` with `@MockBean`) and in-memory alternatives (`InMemoryConfiguration`). New Onion-style resources should use the in-memory approach.

## Blockers / Open Questions

_None yet._

## Effective Prompts

_Record prompts that produced particularly good results here._
