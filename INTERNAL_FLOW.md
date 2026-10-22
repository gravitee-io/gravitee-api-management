# `internalFlow` on v4 APIs — what remains

`internalFlow` is a single `Flow` on the v4 HTTP/message API definition (`io.gravitee.definition.model.v4.Api`), managed by the platform and hidden from the API publisher.

Not supported, by decision: `NativeApi` (Kafka, `NativeFlow`) and V2 APIs.

## Done

| Area | State |
| --- | --- |
| Definition | `Api.internalFlow`, copied by `Api(Api other)`, counted in `getPlugins()` when enabled (license check at deploy) |
| Persistence | Stored inline in the definition JSON (not in the `flows` table) |
| Event payload | Kept by `EventServiceImpl.extractV4ApiDefinition`, which only re-injects `plans` and `flows` |
| Gateway sync (repository) | Deserialized by the sync `ApiMapper`; covered by `ApiMapperTest.should_map_api_v4_with_its_internal_flow` |
| Management API update | `ApiServiceImpl.update` keeps the stored `internalFlow` (`ApiMapper.toRepository(ctx, update, existingApi)`); covers PUT, PATCH, import-update, rollback, CRD / Automation apply |
| Gateway policy loading | `v4/Api.policies()` adds the `internalFlow` policies (when enabled), so `ApiPolicyManager` loads them; covered by `v4/ApiTest` |
| Gateway flow resolver | `InternalFlowResolver` (`v4/flow/resolver/`) provides the `internalFlow` when enabled; `FlowResolverFactory.forInternalFlow(api)` evaluates selectors and condition upfront, never through the best-match selection (single flow) |
| Gateway flow chain | `FlowChainFactory.createInternalFlow(api, tracingContext)`, chain id `"internal"`, no deferred condition |
| LLM proxy reactor | `LLMProxyApiReactor` (gravitee-reactor-llm-proxy) runs the chain: REQUEST after `handleEntrypointRequest`, before plan and API flows; RESPONSE after the API flows, before `afterApiExecutionProcessors`. Covered by `InternalFlowIntegrationTest` (ordering, disabled flow) |
| Policy actions on the context | `addActionOnEndpointRequest` / `addActionOnEntrypointResponse` (gravitee-gateway-api 7.1.0) kept in registration order by `AbstractExecutionContext`, handed over by `drainOnEndpointRequestActions()` (FIFO) / `drainOnEntrypointResponseActions()` (LIFO) on `HttpExecutionContextInternal`; covered by `DefaultExecutionContextTest` |
| Policy actions execution | `PolicyActionExecutor` (`gateway-policy`, `policy/action/`) runs each action in the scope of its policy. `LLMProxyApiReactor` runs the endpoint request actions right before `invokeBackend` (skipped when the request is interrupted earlier) and the entrypoint response actions right before `handleEntrypointResponse`, whichever way the request went (a failure goes through `processThrowable`); `DefaultApiReactor` runs the terminate actions through it. Covered by `PolicyActionExecutorTest` and `PolicyActionsIntegrationTest` (ordering against flows, interrupted request) |
| Policy actions tracing | `TracingPolicyActionHook` (when tracing is enabled): one span per action, e.g. `REQUEST policy-guardian (endpoint-request action)`, with `gravitee.policy` and `gravitee.policy.action` (`endpoint-request`, `entrypoint-response`, `terminate`), verbose events, failure attributes on `interruptWith`. No change to the `addAction*` signatures. Covered by `TracingPolicyActionHookTest` |

## Missing

### 1. Gateway execution (run by the LLM proxy reactor only)

Policy loading, resolver and chain are done (see above), and the LLM proxy reactor runs the chain. Remaining:

- **Reactor wiring** — `DefaultApiReactor.handleRequest` does not run the chain yet. Follow the LLM proxy placement:
  - REQUEST: after `handleEntrypointRequest`, before `executeProductPlanFlowChain`;
  - RESPONSE: after `apiFlowChain`, before `afterApiExecutionProcessors`.
  Same decision for message phases (`MESSAGE_REQUEST` / `MESSAGE_RESPONSE`) in the message flow path.
- **Other reactors** — `TcpApiReactor` (TCP proxy: probably not supported, say so), debug reactors (`DebugV4ApiReactor`) so the debug mode shows the internal flow.
- **Side consumers of flows** — check `PathParametersExtractor` (path params from flow selectors), `ApiV4DefinitionSecretRefsFinder` (secret refs inside policy configurations) and the analytics/logging that list executed flows.
- **Policy actions** — `DefaultApiReactor` (and the message path) does not run the endpoint request / entrypoint response actions yet: same placement as `LLMProxyApiReactor`, through `PolicyActionExecutor`. Not checked on a real trace: whether a span an action starts through `ctx.getTracer()` is parented under the action span.
- **Tests** — a gateway-tests-sdk integration test on `DefaultApiReactor` once wired (`@DeployApi` with an `internalFlow`, assert ordering against API flows), ideally with `guardian-policy`. `v4/ApiTest`, `FlowResolverFactoryTest` and `InternalFlowResolverTest` already cover the building blocks.

### 2. Who writes `internalFlow` (blocking for a real use)

Nothing writes the field yet: only a raw repository/event edit does. Needed:
- a domain service / use case in `gravitee-apim-rest-api-service` (`io.gravitee.apim.core.api`) that sets it (e.g. from attached guardians) and redeploys or marks the API out of sync;
- a decision on when it is recomputed (guardian created/updated/deleted, API created, API migrated V2→V4).

### 3. Management API paths that drop or ignore it

| Path | Current behaviour | To decide |
| --- | --- | --- |
| Create (`NewApiEntity`, `CreateApiDomainService`, `NewHttpApi`) | never set | set by the writer above |
| Import create (`ApiExport.toApiDefinitionBuilder`) | never set | same |
| Export (`GraviteeDefinitionAdapter.mapV4` → `ApiDescriptorV4`) | not exported | probably keep hidden; re-import recomputes |
| Duplicate (`ApiDuplicateServiceImpl` → `ApiMapper.toApiDefinition(ApiEntity)`) | dropped | inherit or recompute |
| V2→V4 migration (`ApiMigration`) | `null` | recompute after migration if needed |
| `GET /apis/{id}/deployments/current` (`GetApiDefinitionUseCase`) | returned (raw definition) | accept or strip |
| Event payloads (`/apis/{id}/events`, audit) | returned | accept or strip |
| "Out of sync" detection (`ApiStateService.isSynchronized` / definition compare) | a change of `internalFlow` alone may not flag the API as out of sync | check and cover |
| Debug mode (`DebugApiUseCase`) | definition passed through | check it reaches the debug gateway |

### 4. Other sync channels

- **Distributed sync** (`DistributedEventType.API`, `distributed/mapper/ApiMapper`): the whole reactable is serialized with Jackson, so it should pass — add a test.
- **Local sync** (`local/mapper/ApiMapper`): same, add a test.
- **Kubernetes sync**: definitions come from the GKO CRD, which has no `internalFlow`; out of scope unless the operator must carry it (owned by `@gravitee-io/gko`).

### 5. Hand-offs

- **Automation API / CRD** (owned by `@gravitee-io/gko`): no schema change; an apply keeps the stored value (update path). PR description must carry an **Automation API / CRD impact** section noting the new v4 definition field.
- **Console**: nothing to show to the publisher. Possibly a read-only hint (e.g. in the policy studio) that platform flows apply.
- **Docs**: execution order of the internal flow relative to platform, plan and API flows.
