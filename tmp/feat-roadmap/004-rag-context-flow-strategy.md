# 004: RAG Context — Flow Strategy

> **Common docs**: [rag-strategy.md](./rag-strategy.md) · [agent-backend.md](./agent-backend.md)

## Objective

Implement the `RagContextStrategy` domain interface and the first concrete implementation: `FlowRagContextAdapter`, which retrieves existing flow/policy context from MongoDB via domain query services.

## Files

```
io.gravitee.apim.core.zee.domain_service/
└── RagContextStrategy.java              ← Interface (domain layer)

io.gravitee.apim.infra.zee.adapter/
├── FlowRagContextAdapter.java           ← First implementation
└── NoOpRagStrategy.java                 ← Default fallback
```

## Key Implementation Notes

### Finding the right query services

Before coding, search the APIM codebase for existing query services:

```bash
grep -r "FlowQueryService\|FlowCrudService" gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/ --include="*.java" -l
grep -r "PluginService\|PolicyService" gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/ --include="*.java" -l
```

The APIM codebase uses specific naming conventions (see [architecture-overview.md](./architecture-overview.md)):

- `*CrudService` / `*CrudServiceImpl` for CRUD operations
- `*QueryService` for read-only queries
- `*DomainService` for domain logic

### Context Formatting

Format RAG context as structured markdown sections that the LLM can parse:

```
### Existing Flows
- Rate Limit Flow: Limits requests to 50/min on /api/v2/*
- Auth Flow: JWT validation on all endpoints

### Available Policies
- rate-limit: Rate limiting and quota management
- transform-headers: Add/remove/modify HTTP headers
- jwt: JWT token validation
- assign-content: Set request/response body content
```

### Tenant Scoping

All queries MUST pass `envId` and `orgId` from the GraviteeContext. The domain query services already handle this — just pass the parameters through.

### Hard Limits

- Max 5 existing flows
- Max 20 policies (plugin listing)
- Total RAG context section should not exceed ~2000 tokens (~1500 words)

## Acceptance Criteria

- [ ] `RagContextStrategy` interface in domain layer
- [ ] `FlowRagContextAdapter` retrieves flows + policies via domain services
- [ ] `NoOpRagStrategy` returns empty string
- [ ] All queries are tenant-scoped
- [ ] Unit test verifies context formatting

## Commit Message

```
feat(zee): add RAG context strategy for Flow resources

Add RagContextStrategy domain interface with FlowRagContextAdapter
that retrieves existing flows and available policies via existing
domain query services. Includes NoOpRagStrategy fallback.
```
