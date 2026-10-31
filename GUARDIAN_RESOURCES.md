# Guardian resources — plan

Status: implemented (steps 1 to 6), not committed.

Attach resources to a Guardian the way they are attached to an API: declared on the Guardian definition, started with
the Guardian, replaced when the Guardian is updated, and resolved by the Guardian policies at request time.

Repositories involved:

| Repository | Branch | Scope |
| --- | --- | --- |
| `gravitee-api-management` (this worktree) | `claude/guardian-reactor-plugin-c354fa` | factory refactoring, definition, sync, secrets, `GuardianExecutionContext`, `GuardianReactor` / `GuardianPolicy` |
| `gravitee-reactor-guardian` | `main` | factory, reactor handler (resources, pending requests, execution) |
| `gravitee-reactor-native-kafka` | `claude/guardian-reactor-plugin-c354fa` (from `alpha`) | follows the `AbstractReactorFactory` rename (breaking), points at `4.14.0-guardian-SNAPSHOT` |

## Decisions

- A Guardian policy sees the Guardian resources **only**: no fallback on the resources of the calling API.
- In-flight requests are tracked with a counter, as `AbstractApiReactor` does, so that an updated Guardian stops its
  resources only once the requests using them are done (bounded by the same timeout).
- `AbstractReactorFactory` (handlers-api) is renamed `AbstractApiReactorFactory`; a new, reactable-agnostic
  `AbstractReactorFactory` is extracted and extended by `GuardianReactorFactory`.
- The `addActionOnResponse` actions registered by Guardian policies are executed (they are silently dropped today).

## Current state

- `AbstractReactorFactory#create` builds, for an API: a `CompositeComponentProvider` (custom + global), a
  `DefaultDeploymentContext`, a `DefaultResourceManager` from `reactable.dependencies(Resource.class)` registered as the
  `ResourceManager` component, then the policy manager and the reactor.
- A policy resolves a resource at request time through `ctx.getComponent(ResourceManager.class)`.
- A Guardian has no execution context of its own: `GuardianPolicy` runs `reactor.policyChain().execute(ctx)` with the
  API execution context, so the Guardian policies resolve the API resources.
- `GuardianReactorHandler` (plugin) starts a policy manager and a chain, registers itself in `GuardianRegistry`; on
  update the reactor handler registry starts the new reactor, the registry swaps it in, then the old one is stopped.
  Nothing tracks the requests still running on the old one.
- `FlowChain#executeFlow` runs, before the RESPONSE policies of a flow, `executeActionsOnResponse` on the REQUEST chain
  of that flow: it looks up the action registered by each policy **of that chain**. The Guardian policies are not part
  of it, so their `addActionOnResponse` actions never run.

## 1. Factory refactoring (APIM)

- Rename `io.gravitee.gateway.reactive.handlers.api.v4.AbstractReactorFactory` → `AbstractApiReactorFactory`; update
  `DefaultApiReactorFactory`, `TcpApiReactorFactory` and their tests.
- Extract a new `AbstractReactorFactory<T extends Reactable> implements ReactorFactory<T>` holding what is not API
  specific:
  - bean lookup across the context hierarchy (`getBeanNamesForType`, today duplicated in `LLMProxyApiReactorFactory`
    and `GuardianReactorFactory`);
  - the component provider (`CustomComponentProvider` + global `ComponentProvider`);
  - the `DefaultDeploymentContext`;
  - the `DefaultResourceManager` creation (`resourceLifecycleManager(...)`), registered as `ResourceManager`;
  - the policy plugin manager lookup.
- `AbstractApiReactorFactory` extends it and keeps the API parts (template variable providers of the API, policy
  manager, `buildApiReactor`, `addExtraComponents`).
- **Module** (decided): a new `gravitee-apim-gateway-handlers-common` module under `gravitee-apim-gateway-handlers`
  (`gateway-reactor` cannot host it: `gateway-resource` and `gateway-policy` depend on it). It depends on
  `gateway-reactor`, `gateway-policy` (hence `gateway-resource`) and `gateway-dictionary`; `handlers-api`,
  `handlers-guardian`, the coverage module and the plugin depend on it. The gateway modules are not in the BOM.
- `environmentTemplateVariableProviders(environmentId)`: the dictionaries of the environment and the API-scoped
  `TemplateVariableProviderFactory` providers, used for the deployment context of a Guardian (EL in resource
  configurations).
- **Breaking**: `KafkaApiReactorFactory` (`gravitee-reactor-native-kafka`) extends `AbstractReactorFactory` directly and
  must move to `AbstractApiReactorFactory`. The reactors going through `DefaultApiReactorFactory` (LLM proxy, …) are not
  affected.

## 2. Definition, sync, secrets (APIM)

- `Guardian` (definition model): `List<Resource> resources` (v4 `Resource`), `getPlugins()` includes the enabled
  resources so the license check of `GuardianManagerImpl#register` covers them.
- `ReactableGuardian#dependencies(Resource.class)` returns them.
- `GuardianDefinitionSecretRefsFinder`: also walk the resource configurations (API keys, credentials…).
- Sync: `GuardianMapper` and its tests (the definition is deserialized as a whole; add a test with resources).
- Update path: an update of the Guardian already redeploys it (`GuardianManagerImpl#update` → `ReactorEvent.UPDATE`);
  nothing else is needed for the resources to follow, once they are owned by the reactor (step 5).
- Management API: who writes Guardians and their resources is out of this plan (same situation as `internalFlow`).

## 3. `GuardianExecutionContext` (APIM, `handlers-guardian`)

A wrapper of the API execution context, created by the Guardian reactor for each execution and passed to the Guardian
policy chain. It implements `MutableExecutionContext` (not only `HttpPlainExecutionContext`) so that the
`instanceof HttpExecutionContextInternal` checks keep working (action drains, legacy adapter, tracing).

```java
public class GuardianExecutionContext implements MutableExecutionContext {

    @Delegate(types = MutableExecutionContext.class, excludes = Overridden.class)
    private final MutableExecutionContext delegate;

    private final ResourceManager resourceManager;      // the Guardian's, no fallback
    private final BasePolicy source;                     // the guardian-policy instance of the API flow
    private List<Function<HttpExecutionContext, Completable>> responseActions;
    private ExecutionContextAdapter adaptedContext;
}
```

### Overridden

| Method | Behaviour | Why |
| --- | --- | --- |
| `getComponent(ResourceManager.class)` | the Guardian `ResourceManager` | the point of it; other components come from the API |
| `addActionOnEndpointRequest` / `addActionOnEntrypointResponse` / `addActionOnTerminate` | registered on the delegate, the function wrapped as `c -> fn.apply(this)` | the action gets the API context at execution time; wrapping keeps the Guardian resources |
| `addActionOnResponse` | collected by the wrapper, see step 4 | the API flow chain only looks up actions of its own policies |
| `get/set/removeInternalAttribute(ATTR_INTERNAL_ADAPTED_CONTEXT)` | kept in a field of the wrapper | `ExecutionContextAdapter.create(ctx)` caches the v3 adapter on the context; delegated, a legacy Guardian policy (the steps run in `V4_EMULATION_ENGINE`) would reuse the adapter of the API (API resources), and the API legacy policies would reuse the Guardian one afterwards |

### Delegated as is

- `request()` / `response()`: same objects, headers, body and `onBody` / `onChunks` transformations apply to the real
  exchange.
- Attributes and internal attributes: shared on purpose (the Guardian sees what the API set and conversely); this
  covers `ComponentScope` and the tracing spans, stored as internal attributes.
- `interruptWith(...)`: the interruption error leaves the Guardian chain and goes up the API chain as if
  `guardian-policy` interrupted.
- Tracer, template engine, metrics, logger, drains.

### Why a wrapper rather than swapping the component provider

A policy often works after its `onRequest` / `onResponse` returned: `ctx.response().onBody(...)` / `onChunks(...)` run
when the body flows (SSE on the LLM proxy), after the Guardian chain completed. A temporary swap would already be
undone; the closure of the policy captured the wrapper and keeps resolving the Guardian resources.

### Known limits

- Fluent setters (`ctx.request(x)`, `ctx.tracer(t)`) return the delegate, not the wrapper. No policy relies on it.
- A context that is not a `MutableExecutionContext` is not expected (v4 reactors and debug mode pass a
  `DefaultExecutionContext` or a subclass): fail explicitly rather than running the Guardian without its resources.

## 4. Executing `addActionOnResponse` of Guardian policies (APIM, `handlers-guardian`)

`FlowChain` runs, at the start of the RESPONSE phase of a flow, the action registered by each policy of the REQUEST
chain of that flow, in reverse order. `guardian-policy` is one of those policies. So:

- The wrapper knows `source`, the `guardian-policy` instance executing the Guardian (passed by `GuardianPolicy`).
- On the first `addActionOnResponse(policy, fn)` of a Guardian policy, the wrapper registers on the delegate a single
  action under `source`: `c -> run the collected actions`; every call appends `fn` to `responseActions`.
- When the API flow reaches its RESPONSE phase, it runs the action of `guardian-policy`, which runs the collected
  actions in **reverse order of registration** (same contract as the API policies), each one with the wrapper
  (`fn.apply(this)`) and in the scope of its own policy (`ComponentScope`).
- Result: the actions run exactly where they would if the Guardian policies were inline in the API flow, including
  when the Guardian is called from the internal flow (also a `FlowChain`).
- Not covered (same as an API policy): an `addActionOnResponse` registered during the RESPONSE phase never runs, the
  response actions of a flow being executed before its response policies. A Guardian of phase `RESPONSE` is concerned.

## 5. Guardian reactor (APIM interface + plugin)

### Interface (APIM)

- `GuardianReactor`: replace `HttpPolicyChain policyChain()` by
  `Completable execute(HttpPlainExecutionContext ctx, BasePolicy source)`.
- `GuardianPolicy#execute`: `registry.get(...).execute(ctx, this)`; it no longer touches the chain.

### Plugin (`gravitee-reactor-guardian`)

- `GuardianReactorFactory extends AbstractReactorFactory<ReactableGuardian>`: gets the component provider, the
  deployment context and the `DefaultResourceManager` from the base; removes its own bean lookup.
- `GuardianReactorHandler`:
  - owns the `ResourceLifecycleManager`;
  - `doStart`: resources, then policy manager, then chain, then `guardianRegistry.register(this)`;
  - `execute(ctx, source)`: wraps the context in a `GuardianExecutionContext`, acquires the request (below), runs the
    chain with the wrapper;
  - `doStop`: `guardianRegistry.unregister(this)`, waits for the pending requests (below), then stops the policy
    manager and the resources.

### Pending requests

A counter around the chain alone is not enough: `onBody` / `onChunks` use the resources after the chain completed, an
updated Guardian would stop them in the middle of a stream.

- Acquire on the first execution of the Guardian in the request (an internal attribute marks it, so a request counts
  once).
- Release through an `addActionOnTerminate` registered by the handler: it runs once the response is written, whichever
  way the request ended.
- `doStop` waits for the counter to reach 0, with the timeout of `AbstractApiReactor`
  (`PENDING_REQUESTS_TIMEOUT_PROPERTY`, 10 s by default), polling like `AbstractApiReactor#doStop`.
- Gap: a disposed (cancelled) request does not run the terminate actions; `doStop` then waits until the timeout, same
  as an API whose requests never end.
- The release is a terminate action registered under a per-reactor source (`guardian-<id>`): with tracing enabled it
  shows as a `RESPONSE policy-guardian-<id> (terminate action)` span.

## 6. Tests

APIM:
- `AbstractReactorFactory` / `AbstractApiReactorFactory`: existing factory tests renamed, a test of the extracted base
  (component provider, `ResourceManager` registered).
- `Guardian` / `ReactableGuardian`: `getPlugins()` and `dependencies(Resource.class)` with resources (enabled, disabled).
- `GuardianDefinitionSecretRefsFinderTest`: secret refs in resource configurations.
- `GuardianMapperTest`: a Guardian with resources.
- `GuardianExecutionContextTest`:
  - `getComponent(ResourceManager)` returns the Guardian one, other components the API ones;
  - endpoint request / entrypoint response / terminate actions are registered on the delegate and receive the wrapper;
  - response actions: a single action registered under `source`, collected actions run in reverse order with the
    wrapper and in the scope of their policy;
  - the legacy adapter cache is isolated in both directions;
  - everything else is delegated.
- `GuardianPolicyTest`: delegates to `GuardianReactor#execute(ctx, this)`; Guardian not deployed → request untouched.

Plugin:
- `GuardianReactorFactoryTest`: the handler gets a resource manager built from the Guardian resources.
- `GuardianReactorHandlerTest`: start / stop order; a request is acquired once and released at terminate; `doStop`
  waits for pending requests, then stops the resources; timeout.
- Integration (tests SDK, `@DeployGuardians`):
  - a Guardian with a test resource used by a Guardian policy, in `onRequest` and in an `onBody` transformation;
  - an `addActionOnResponse` registered by a Guardian policy runs at the start of the API flow response phase, with the
    Guardian resources;
  - an update of the Guardian changing the resource configuration: the new value is used, the old resource is stopped
    once the in-flight request ended.

## 7. Order of work and commits

1. APIM — factory refactoring (rename + extracted base), module decision applied.
2. APIM — definition, `ReactableGuardian`, secrets, sync.
3. APIM — `GuardianExecutionContext` (resources, actions, legacy adapter, response actions).
4. APIM — `GuardianReactor#execute`, `GuardianPolicy`. Install the snapshot.
5. Plugin — factory on the new base, handler (resources, pending requests, execution), tests.
6. `gravitee-reactor-native-kafka` — follow the rename (separate change, to coordinate).

## Out of scope / open

- LLM policies inside a Guardian: `GuardianReactorFactory` uses the global `PolicyFactoryManager`, not the
  `LlmPolicyFactoryManager` of the LLM proxy reactor, so an LLM policy in a Guardian is not adapted to a
  `PivotLlmExecutionContext`. It would work with the wrapper (`PivotLlmExecutionContext` delegates `getComponent`) once
  adapted. To decide separately.
- Who writes the Guardian resources (Management API, console).
