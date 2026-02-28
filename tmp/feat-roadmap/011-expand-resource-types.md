# 011: Expand to Additional Resource Types

> **Common docs**: [agent-backend.md](./agent-backend.md) · [rag-strategy.md](./rag-strategy.md) · [frontend-widget.md](./frontend-widget.md)

## Objective

Expand Zee Mode to support Plan, Endpoint, Api, and Entrypoint resource types. Each needs: backend component registration, RAG strategy, frontend adapter, and Console UI integration point.

## Per-Resource Checklist

### Plan (PLAN)

**Backend**:

- [ ] Register `Plan` in `LlmEngineServiceImpl` generator map
- [ ] Implement `PlanRagContextAdapter` — retrieves existing plans, security configs
- [ ] Domain services needed: existing plan query service, security type registry

**Frontend**:

- [ ] Create `PlanAdapter` (transforms generated Plan → save payload)
- [ ] Create `PlanCardComponent` structured preview
- [ ] Wire into plan creation page

### Endpoint (ENDPOINT)

**Backend**:

- [ ] Register `Endpoint` in generator map
- [ ] Implement `EndpointRagContextAdapter` — retrieves existing endpoint groups, connector types

**Frontend**:

- [ ] Create `EndpointAdapter`
- [ ] Wire into endpoint configuration page

### Api (API) — Most Complex

**Backend**:

- [ ] Register `Api` in generator map
- [ ] Implement `ApiRagContextAdapter` — retrieves org's existing APIs, common patterns
- [ ] Note: The `Api` schema is the largest and most complex — test carefully

**Frontend**:

- [ ] Create `ApiAdapter` — this will be the most complex adapter
- [ ] Wire into API creation page

### Entrypoint (ENTRYPOINT)

**Backend**:

- [ ] Register `Entrypoint` in generator map
- [ ] Implement `EntrypointRagContextAdapter`

**Frontend**:

- [ ] Create `EntrypointAdapter`
- [ ] Wire into entrypoint configuration page

## Notes

- Add each resource type incrementally — test one before starting the next
- Start with `Plan` (structurally simple), then `Endpoint`, then `Entrypoint`, then `Api` (most complex)
- Each resource type may have different Console UI save payload nuances — check the existing form components

## Commit Message

```
feat(zee): expand to Plan, Endpoint, Api, Entrypoint

Add backend component registrations, RAG strategies, frontend
adapters, and Console UI integrations for Plan, Endpoint, Api,
and Entrypoint resource types.
```
