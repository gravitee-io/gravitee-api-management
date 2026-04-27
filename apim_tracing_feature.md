# APIM Tracing Feature — POC Summary

A proof-of-concept migration of the standalone `gravitee-apim-console-extension-otel` plugin
into `gravitee-api-management` as a first-class feature. This document captures the goal, the
plan that drove implementation, the files produced/modified, the current behaviour of each view,
and the known limitations.

---

## 1. Goal

The existing OTel extension (deployed as a separate plugin zip) ships a tracing explorer that reads
from Grafana Tempo. The goal is to embed that capability into the core product:

- Expose tracing endpoints via the v2 REST API (new resource).
- Implement the domain/use-case layer in `gravitee-apim-rest-api-service`.
- Surface the UI inside `gravitee-apim-console-webui` under **Observability → Tracing**.
- Keep it a POC (single Tempo backend, no license gate, no multi-tenancy scoping yet).

---

## 2. Decisions (answered at planning time)

| Question | Answer |
|---|---|
| Backend abstraction? | Tempo only for now (interface kept so Jaeger / OTLP-direct can be added later). |
| Permissions? | Reuse `environment-platform-r` (same as other Observability items). |
| Multi-tenancy scoping in TraceQL? | Not needed for the POC — noted as TODO in the infra impl. |
| License gated? | No — feature is always available in this POC. |
| Scope of deliverable? | Single demo-ready POC; no PR split; minimal tests. |

---

## 3. Architecture

```
 ┌─────────────────────────────┐
 │ gravitee-apim-console-webui │   Observability → Tracing pages + 3 views (Flow / Timeline / Debug)
 └─────────────┬───────────────┘
               │  HTTPS GET
               ▼
 ┌──────────────────────────────────────────┐
 │ gravitee-apim-rest-api-management-v2-rest │   /environments/{envId}/tracing/...
 │  - TracingResource (JAX-RS)               │
 └─────────────┬────────────────────────────┘
               │
               ▼
 ┌─────────────────────────────────────────┐
 │ gravitee-apim-rest-api-service          │
 │  core/tracing/                           │
 │   ├─ model/         (Trace, TraceSpan,   │
 │   │                  TraceEvent, etc.)   │
 │   ├─ query_service/ TracingQueryService  │
 │   ├─ domain_service/TracingGraphBuilder  │
 │   └─ use_case/      (3 use cases)        │
 │  infra/                                  │
 │   └─ query_service/tracing/              │
 │      ├─ TempoTracingQueryService @Service│
 │      └─ TempoHttpClient + response POJOs │
 └─────────────┬────────────────────────────┘
               │ HTTP /api/traces, /api/search
               ▼
 ┌─────────────────────────────┐
 │       Grafana Tempo          │
 └─────────────────────────────┘
```

Clean-architecture split:

- **core/** has no framework dependencies (except the `@UseCase`/`@DomainService` marker annotations).
  Ports (`TracingQueryService`) live here.
- **infra/** provides Spring-wired adapters. `TempoTracingQueryService` implements the port, reads
  `tracing.tempo.url` via `@Value` (default `http://localhost:3200`).
- **v2-rest** calls use cases; each endpoint resolves the audit info from `AbstractResource.getAuditInfo()`
  and returns the domain records serialised as JSON (no DTO layer yet — see TODO).

---

## 4. Backend work

### 4.1 Service module — `gravitee-apim-rest-api-service`

New package `io.gravitee.apim.core.tracing` containing:

| File | Purpose |
|---|---|
| `model/Trace.java` | Root record: traceId, start time, duration, root service/operation, **`hasError` flag**, spans. |
| `model/TraceSpan.java` | Single span: IDs, operation/service name, timing, attributes, events, children. |
| `model/TraceEvent.java` | Span event (e.g. `gravitee.policy.pre` / `.post`): name, time, attributes. |
| `model/TracingGraph.java` | Agent/LLM/MCP graph: traceId, duration, nodes, edges. |
| `model/TracingNode.java` | Node: id, type (`agent`/`mcp_server`/`llm`/…), label, subtitle, status, metadata. |
| `model/TracingEdge.java` | Edge: from, to, list of `EdgeSpan`. |
| `model/EdgeSpan.java` | Single call inside an edge: operation, duration, status, tool, tokens. |
| `model/TraceSearchCriteria.java` | Search filter: tags (logfmt), limit, start, end (epoch seconds). |
| `query_service/TracingQueryService.java` | Port: `searchTraces(criteria)` + `getTrace(id)`. |
| `domain_service/TracingGraphBuilder.java` | `@DomainService` — classifies spans from OTel attributes (agent, LLM, MCP) and builds the edge map. Pure function, no I/O. |
| `use_case/SearchTracesUseCase.java` | `@UseCase` input=(AuditInfo, criteria), output=List\<Trace>. |
| `use_case/GetTraceUseCase.java` | `@UseCase` — throws `NotFoundDomainException` when Tempo returns empty. |
| `use_case/GetTracingGraphUseCase.java` | `@UseCase` — composes `GetTrace` + `TracingGraphBuilder`. |

Infra adapters under `io.gravitee.apim.infra.query_service.tracing`:

| File | Purpose |
|---|---|
| `TempoTracingQueryService.java` | `@Service` — implements the port. Reads `tracing.tempo.url`. Parses OTLP JSON into domain records, including span events. Computes `hasError` from spans. Overrides `rootOperation` from the inbound HTTP server span's `http.target` (+ `http.method`) so policies that mutate `http.route` (e.g. http-callout) don't corrupt the displayed path. Normalises OTLP status codes to `UNSET`/`OK`/`ERROR` regardless of whether Tempo emits them as numeric (`"0"/"1"/"2"`) or as the proto enum (`STATUS_CODE_*`). TODO-marked for env/org scoping. |
| `TempoHttpClient.java` | Lightweight wrapper over `java.net.http.HttpClient`. Calls `/api/traces/{id}` and `/api/search?q=…`. |
| `TempoTraceResponse.java` | Jackson records for the `/api/traces` payload. `@JsonAlias("instrumentationLibrarySpans")` on `scopeSpans` so both legacy and modern OTLP JSON shapes are accepted. `Value` record handles `stringValue`/`intValue`/`boolValue`/`doubleValue`. Includes a nested `Event` record for policy pre/post events. |
| `TempoSearchResponse.java` | Jackson records for the `/api/search` payload, including a `SpanSet` projection so the TraceQL `select(span.http.target, span.http.method)` projection rides along with each search result. |

**Search-time enrichments (`searchTraces`):**

1. `buildTraceQL(tags, errorsOnly=false)` produces the primary query and appends `| select(span.http.target, span.http.method) with (most_recent=true)`. The select makes Tempo return those attributes inside each `TraceResult.spanSet` so `deriveRootOperation()` can emit `METHOD /target` instead of the raw `rootTraceName` (which Tempo derives from `span.name`, itself driven by the mutable `http.route`).
2. A second TraceQL pass with `status = error` runs in parallel — matching trace IDs are flagged with `hasError=true`. Wrapped in try/catch so older Tempo builds that don't support the `status` intrinsic degrade gracefully (list still loads, status column shows `—`).

### 4.2 REST module — `gravitee-apim-rest-api-management-v2-rest`

| File | Change | Purpose |
|---|---|---|
| `resource/tracing/TracingResource.java` | **new** | JAX-RS resource mounted under `/environments/{envId}/tracing`. Exposes `GET /traces`, `GET /traces/{id}`, `GET /traces/{id}/graph`. Injects the three use cases. Returns domain records directly (Jackson serialises them natively). |
| `resource/installation/EnvironmentResource.java` | edited | Adds `@Path("/tracing")` sub-resource method so the tracing endpoints hang under the env resource. |
| `GraviteeManagementV2Application.java` | edited | `register(TracingResource.class)` alongside the other v2 resources. |

Endpoints exposed:

- `GET /management/v2/environments/{envId}/tracing/traces?tags=&limit=&start=&end=`
- `GET /management/v2/environments/{envId}/tracing/traces/{traceId}`
- `GET /management/v2/environments/{envId}/tracing/traces/{traceId}/graph`

---

## 5. Console UI work (`gravitee-apim-console-webui`)

### 5.1 Service + shared files

| File | Purpose |
|---|---|
| `services-ngx/tracing-v2.service.ts` | HTTP service hitting `{env.v2BaseURL}/tracing/traces*`. `searchTraces`, `getTrace`, `getTracingGraph` (latter augments nodes with UI icon hints). |
| `management/observability/tracing/tracing.model.ts` | TS mirror of the domain records (Trace, TraceSpan, TraceEvent, TracingGraph, etc.). |
| `management/observability/tracing/entity-colors.ts` | Palette per entity type (Agent/LLM/MCP/API/…), used by timeline & graph renderings. |

### 5.2 List page

| File | Purpose |
|---|---|
| `management/observability/tracing/tracing-list.component.ts` + `.scss` | Standalone component. Wraps a `<gio-header title="Tracing">` + `<mat-card>` containing the filter bar (Period · API · Refresh) and a Material table with pagination/sort. Uses the shared `gio-responsive-content-container` mixin so the page matches the Logs page padding exactly. |
| `management/observability/observability.module.ts` | Added `tracing` + `tracing/:traceId` routes pointing to the list and detail components. |
| `components/gio-side-nav/gio-side-nav.component.ts` | Added a "Tracing" item inside the Observability menu block. |

**Filter bar — aligned with the Logs page:**

- **Period** (mat-select, replaces the old "Time range") — 5m / 15m / 30m / 1h / 6h / 24h / 7d.
- **API** — uses the shared `GioSelectSearchComponent` (same one as `EnvLogsFilterBarComponent`). A custom `apiResultsLoader` calls `ApiV2Service.search(...)` and maps each API to `{ value: api.name, label: api.name }` so the selected value drops straight into a TraceQL `service.name=<name>` tag — no id↔name resolution needed. The picker is multi-select by design; the first selected value drives the query.
- **Refresh** stroked button.

**Table columns** (in order): `Status`, `Trace ID`, `API` (renamed from "Root Service" but still bound to `row.rootService`), `Operation`, `Start Time`, `Duration`, actions.

- **Status badge** (`status-ok` / `status-error` / `status-unknown`) — driven by `trace.hasError`. See §5.7 for how the backend computes that flag.
- Default `pageSize` set to **10** with options `[10, 25, 50]`, matching the Logs paginator.

### 5.3 Trace detail + views

| File | Purpose |
|---|---|
| `management/observability/tracing/trace-detail/trace-detail.component.ts` + `.scss` | Parent page with back button, trace-id chip, and a 3-button view toggle (Timeline / Flow / Debug). Loads the trace + graph in parallel (`forkJoin`). Flow is the default view. |
| `trace-detail/timeline-view/timeline-view.component.ts` | Waterfall view: left tree of service/operation names, right-side time ruler with proportional span bars, click-to-select. |
| `trace-detail/flow-view/flow-view.component.ts` | **Primary** view. Two-row horizontal chain of cards representing the request/response loop. Details below. |
| `trace-detail/debug-view/debug-view.component.ts` + `trace-to-debug-response.ts` | Reuses the existing `debug-mode-response` component from the debug-mode feature. Details below. |

### 5.4 Flow view — layout

The view renders a **request/response loop** with a loopback that drops below the Client card:

```
          [Client] → [Request] → {FLOW · org [Security]} → {FLOW · plan-test [policy → policy]} ─ ─ ─ ─┐
              ▲                                                                                        │
              │                                                                                        ▼
              └ ─ ─ [Response] ← {FLOW · org [transform-headers]} ← [Backend] ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
```

Key design points:

- **Line 1 (request path, LTR)**: `[Client] → [Request] → [grouped policies] → ─ ─ corner ↓` — request cards are left-stacked; a flex-`1` `.stretch-arrow` fills the remaining width. Its `::before` draws a horizontal dashed line at card-middle, `::after` drops a short vertical stub into the inter-line gap, and `arrow_downward` sits at the bottom with its tip touching line 2's top.
- **Line 2 (response path, rendered LTR but visually RTL)**: `[Response, loopback-horizontal, response groups, Backend]`. `loopback-horizontal` is a `flex:1` filler whose `::before` draws the dashed line between Response's left edge and the loopback vertical. Arrows use `arrow_back` so the reading direction matches the actual flow.
- **Flow groups**: consecutive cards whose parent OTel span carries a `flow` attribute are wrapped in a dashed box labelled `FLOW · {name}`. `formatFlowName()` shortens the label: `organization-all*` → `org`, and any trailing `-/` (e.g. `plan-all-/`, `plan-test-/`) is stripped.
- **Card types** (`client`, `request`, `security`, `policy`, `backend`, `agent`, `llm`, `mcp`, `response`) have distinct border colours, icons, and badges.
- **Loopback visual** (rebuilt to drop the old far-left "L"): `.loopback-vertical` is absolute-positioned at `left: 64px` (the Client card's centre, since the card is 130 px wide), descending into the inter-line gap; `.loopback-arrowhead` is `arrow_upward` placed just below the Client's bottom edge, tip pointing into the card. `.loopback-horizontal::before` extends from Response's left edge back to that vertical line. The old `padding-left: 28px` on `.chain-body--looped` is gone.
- **All cards are 120 px tall** (was 95 px) with `align-items: flex-end`. Card content uses `display: flex; flex-direction: column`; the footer pins to the bottom via `margin-top: auto`.

**Geometry (so the dashed lines stay aligned with the cards):**

| Element | Value | Rationale |
|---|---|---|
| `.flow-card { height }` | 120 | Iconified: 4+56+14+16+4 = 94 (26 auto-gap). Non-iconified: 8+26+16+16+16+8 = 90 (30 auto-gap). |
| `.chain-line { margin-bottom }` | 40 | Inter-line gap; arrows occupy this strip. |
| `.stretch-arrow { height }` | 120 | Matches card height; horizontal dashed at `top: 60` (card middle). |
| `.stretch-arrow::after` | `top: 60`, `height: 78` | Vertical stub from card-middle down to y=138 inside the stretch-arrow box. |
| `.stretch-head` | `top: 138`, font-size 22 | Tip (bottom of `arrow_downward`) lands at chain-body y=198 = line 2 top. |
| `.loopback-horizontal { height }` | 120, `::before { top: 60 }` | Dashed line at line 2 card middle (chain-body y≈296). |
| `.loopback-arrowhead` | `top: 165`, `left: 54` | 22 px icon centred on x=65 (Client centre); tip 7 px below Client's bottom (y=158). |
| `.loopback-vertical` | `top: 187`, `height: 109` | Goes from below the arrow to line 2 middle (y=296). |
| `.arrow` (chevrons between cards) | `height: 120` | Keeps them vertically centred on cards. |

**Cards — runtime enrichments:**

- **Backend card** reads the invoker's HTTP child (parent=invoker span, has `http.url`) and stores `httpMethod`, `httpUrl`, and `statusCode`. The status code renders as a colored pill (`success`/`redirect`/`client-error`/`server-error`) in the card footer; method+URL move out of the subtitle and into the detail panel as dedicated metric cards. The card subtitle falls back to the `Invoker` attribute or "Backend call".
- **Policy cards — icon + tooltip:** on `ngOnInit`, the component calls `GET {org.v2BaseURL}/plugins/policies?expand=icon` and indexes the `PolicyPlugin[]` by id. Policy cards then render the policy's base64-data-url icon at **56 px** centred in the card (no type badge), with a `matTooltip` showing the friendly policy name. Non-iconified cards keep the original badge + label + subtitle layout. The chain renders first; the policy fetch enriches it once it resolves (graceful error fallback to badge+label).
- **Request card** uses an inbound-HTTP-server finder: `http.method` present, no `http.url` (rules out the callout's HTTP CLIENT span), no `Invoker`, and at least one server marker (`client.address` / `net.host.name` / `http.target`). Among matches, the **earliest start time wins** so the true root span is picked even when downstream spans look superficially server-shaped.
- **Response card** sets `span: null` — the synthetic aggregation has no span-level attributes worth showing in the detail panel.

### 5.5 Flow view — detail panel (bottom)

Selecting a card populates a detail section below the chain:

- **Header**: type badge + operation name. Below the title, an italic `detail-description` line surfaces `gravitee.policy.description` when the selected policy span carries it.
- **Metric cards**: Duration, Service. For backend cards, additional `Method` and `URL` cards (the URL card is wider and monospaced). When present, a `Tokens` card shows `gen_ai.usage.input_tokens` + `output_tokens`.
- **Exception block** (errored cards only): a red-bordered panel showing `exception.type`, `exception.message`, and a collapsible `<details>` with the stacktrace — pulled from the span's `exception` event (see §5.7).
- **Verbose tracing diff**: when the selected policy span has both `gravitee.policy.pre` and `gravitee.policy.post` events, the panel renders a 3-column `Attribute / Before / After` table with row-level highlighting — `MODIFIED` (amber), `ADDED` (green), `REMOVED` (red), `UNCHANGED` (no badge). Changed rows sort to the top.
- **Span events fallback**: when pre/post events are absent, any other events on the span are listed with their attributes.
- **Span attributes**: key/value table at the bottom.
- **Placeholder branches**: for cards with no span (Client, Response), the detail panel shows the card's icon + "No additional details for the {label} card." instead of the generic "Select a card…" message.

**Duration formatters** (so sub-millisecond spans don't collapse to `0ms`):

- `formatMs(ms)`: `<1` → `Nµs` · `<10` → `N.Nms` · `<1000` → `Nms` · else seconds.
- `formatDuration(nanos)`: cascades through `ns` → `µs` → `ms` (with one decimal under 10ms) → seconds.

### 5.6 Debug view — reuse of the existing debug-mode UI

The debug view reuses `DebugModeResponseComponent` from `management/api/debug-mode` so the look-and-feel of the existing "Debug mode" feature is reused for trace inspection.

- `DebugModeModule` was updated to **export** `DebugModeResponseComponent` (previously only `DebugModeComponent` was exported).
- `trace-to-debug-response.ts` projects a `Trace` onto the `DebugResponse` shape consumed by the debug-mode UI. Mapping strategy:
  - Root HTTP **SERVER** span → parent-less span with `http.method` → supplies `request`/`response` envelope (method, path, status).
  - Invoker span (`Invoker=endpoint-invoker`) + its HTTP CLIENT child → `backendResponse` (url, method, status).
  - Policy + Security spans → policy debug steps.
    - `gravitee.policy.post` event attributes supply `output.headers` / `output.attributes`.
    - **Running-state propagation**: the mapper walks request- and response-phase policies in order, carrying a `ParsedState` (headers + attributes). When a span has no `gravitee.policy.post` event (security spans, or any policy the gateway didn't instrument verbosely), the step **reuses the previous running state** so the debug-mode diff shows "unchanged" for that step instead of wrongly flagging every header/attribute as removed. Span-level attributes are still merged on top so security decisions surface in the inspector.
    - Status: `otel.status_code=ERROR` → `ERROR`; `gravitee.policy.trigger.executed=false` → `SKIPPED`; else `COMPLETED`.
    - Stage: `SECURITY` for security spans, else derived from parent flow name (`plan-*` → `PLAN`, `organization-*` → `PLATFORM`, otherwise `API`).
  - Header prefix normalisation: `http.request.header.*` → request headers, `http.response.header.*` → response headers, all other non-meta keys are kept verbatim (preserving `gravitee.attribute.*` prefix for consistency with the trace).
- `DebugViewComponent` is a standalone wrapper that imports `DebugModeModule`, runs the mapper on input change, and embeds `<debug-mode-response>` inside a `position: relative` slot so the component's internal `height: 100%` resolves correctly against a `min-height: 700px` container.

### 5.7 Error visualisation across views

Errors are surfaced consistently from the list down to individual span bars:

- **Backend**: see §4.1 — `Trace.hasError` is computed by `searchTraces` (second TraceQL pass with `status = error`) and by `convertToTrace` (any span with `otel.status_code=ERROR`). The OTLP status code switch normalises both numeric (`"0"`/`"1"`/`"2"`) and proto-enum (`STATUS_CODE_UNSET/OK/ERROR`) forms before exposing them to the frontend.
- **List**: `Status` column shows a green `OK` / red `Error` / neutral `—` badge driven by `trace.hasError`.
- **Flow view**: `FlowCard.hasError` is set when the span carries `otel.status_code` of `ERROR` / `STATUS_CODE_ERROR` / `2`. **Request and Response cards are skipped** in this pass because OTLP propagates ERROR up the parent chain and would otherwise light up both end-cards on every failure. Errored cards get a red border, an `error` icon in the top-right, and a tooltip with the exception message. The detail panel adds a red `Exception` block (type · message · collapsible stacktrace) sourced from the span's `exception` event.
- **Timeline view**: span bars for errored spans render with a red background and an `error` icon overlay; the selected-span status pill in the detail panel flips between `OK`/`ERROR` via the same `isError(span)` helper.

---

## 6. Dependencies added

- `gravitee-apim-console-webui` / root `package.json`: no new runtime dependencies after cleanup. (d3 was added for the graph view and removed when the graph view was dropped from the POC.)
- No backend dependencies were added — only `java.net.http` (JDK) and Jackson (already transitive via common).

---

## 7. How to run the POC

### 7.1 Prerequisites

- Docker + Docker Compose (for Tempo).
- A Gravitee gateway already configured to emit OTel traces (the OTel tracer must be enabled in `gravitee.yml` of the gateway, and verbose tracing is recommended on the API — see §7.5).
- A built `gravitee-apim-rest-api` and `gravitee-apim-console-webui` from this branch.

### 7.2 Start Tempo with Docker Compose

The example stack runs Tempo in monolithic mode with local filesystem storage. Drop the two files below into a folder (e.g. `./tempo-poc/`) and run `docker compose up -d`.

**`docker-compose.yml`**

```yaml
version: "3.8"

services:
  tempo:
    image: grafana/tempo:2.5.0
    container_name: tempo
    command: ["-config.file=/etc/tempo.yaml"]
    volumes:
      - ./tempo.yaml:/etc/tempo.yaml:ro
      - tempo-data:/var/tempo
    ports:
      - "3200:3200"   # tempo HTTP API (used by the REST API, value of tracing.tempo.url)
      - "4317:4317"   # OTLP gRPC ingest (point the gateway here)
      - "4318:4318"   # OTLP HTTP ingest
    restart: unless-stopped

  # Optional — Grafana with Tempo datasource pre-wired, for cross-checking traces.
  grafana:
    image: grafana/grafana:11.1.0
    container_name: grafana
    environment:
      - GF_AUTH_ANONYMOUS_ENABLED=true
      - GF_AUTH_ANONYMOUS_ORG_ROLE=Admin
      - GF_AUTH_DISABLE_LOGIN_FORM=true
    ports:
      - "3000:3000"
    depends_on:
      - tempo

volumes:
  tempo-data:
```

**`tempo.yaml`**

```yaml
server:
  http_listen_port: 3200

distributor:
  receivers:
    otlp:
      protocols:
        grpc:
          endpoint: 0.0.0.0:4317
        http:
          endpoint: 0.0.0.0:4318

ingester:
  trace_idle_period: 10s
  max_block_duration: 5m

compactor:
  compaction:
    block_retention: 24h

storage:
  trace:
    backend: local
    local:
      path: /var/tempo/traces
    wal:
      path: /var/tempo/wal

# Required so the list page's "errors only" pass (TraceQL `status = error`)
# and the `select(span.http.target, span.http.method)` projection work.
metrics_generator_enabled: false
```

Sanity-check Tempo is reachable from the host running the REST API:

```bash
curl -s http://localhost:3200/ready          # → "ready"
curl -s 'http://localhost:3200/api/search?limit=1'
```

Point the gateway's OTel exporter at `http://<docker-host>:4317` (gRPC) or `:4318` (HTTP) — that is the ingest path. The REST API talks to `:3200` only.

### 7.3 Configure the REST API to query Tempo

The infra adapter reads `tracing.tempo.url` (default `http://localhost:3200`). Both the `gravitee.yml` form and the env-var form are accepted — pick one.

**Option A — `gravitee.yml`** (file lives at `gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/src/main/resources/config/gravitee.yml` for a packaged distribution, or under `config/` next to your running REST API):

```yaml
# ──────────────────────────────────────────────────────────────
# Tracing (POC) — backend used by the Observability → Tracing UI
# ──────────────────────────────────────────────────────────────
tracing:
  tempo:
    # Base URL of the Tempo HTTP API. The REST API hits
    # /api/traces/{id} and /api/search against this host.
    # Default: http://localhost:3200
    url: http://localhost:3200
```

**Option B — environment variable** (Spring relaxed binding maps the property name to `GRAVITEE_TRACING_TEMPO_URL`):

```bash
export GRAVITEE_TRACING_TEMPO_URL=http://localhost:3200
```

If both are set, the env var wins (standard Spring property precedence).

### 7.4 Build & launch

1. Build APIM normally (`mvn -T1C -DskipTests install` at the repo root, or build only the REST API + console modules).
2. Start the REST API. The tracing endpoints are wired automatically: the use cases via `@UseCase`, the Tempo adapter via `@Service` component scan, and the JAX-RS resource via the explicit `register(TracingResource.class)` in `GraviteeManagementV2Application`.
3. Smoke-test the backend wiring:

   ```bash
   curl -s -u admin:admin \
     'http://localhost:8083/management/v2/environments/DEFAULT/tracing/traces?limit=5' | jq
   ```

4. Start the console (`yarn serve` in `gravitee-apim-console-webui`) and navigate to **Observability → Tracing**.

### 7.5 Use the feature

- Pick a period and (optionally) an API in the filter bar; the list shows the latest matching traces with a Status column.
- Click a row to open the trace detail; switch between **Flow / Timeline / Debug** views.
- For the Flow view's verbose pre/post diff and the full Debug view inspector, the Gravitee gateway must have **verbose OTel tracing** enabled on the API (the `gravitee.policy.pre` / `gravitee.policy.post` events are what the diff is computed from).

---

## 8. Known limitations / TODOs

- **No DTO layer in v2 REST**: resources return domain records directly. A dedicated OpenAPI spec + generated DTOs + MapStruct mappers should be added before productising, following the pattern of `openapi-logs.yaml` / `LogsEngineMapper`.
- **No multi-tenancy scoping**: TraceQL does not currently constrain traces by env/org. The infra impl has a TODO; the fix is to inject a resource-level filter (e.g. `resource.gravitee.environment = "{envId}"`) if the gateway emits that attribute.
- **No license / permission gate beyond `environment-platform-r`**: matches the demo scope; productisation may want a dedicated permission.
- **No tests**: one happy-path spec per layer is sufficient for the POC, none was committed. Backend use cases and the graph builder are easy to test in isolation; the mapper `trace-to-debug-response.ts` is a pure function and deserves a unit test.
- **Debug view limitations when verbose tracing is off**: traces without `gravitee.policy.pre`/`.post` events will produce an inspector with only HTTP properties and span-level attributes. This is a data limitation, not a mapping one.
- **Tempo-only backend**: the port abstraction is in place but there is only one implementation.
- **Graph view dropped**: the agent/LLM/MCP graph from the original extension was removed from the trace detail (the detail keeps the Flow, Timeline, and Debug views) to keep the POC focused on the Gravitee-gateway-centric trace shape. `TracingGraph`/`TracingNode`/`TracingEdge` models and the `GetTracingGraphUseCase` remain on the backend — the UI still calls them for the Flow view's agent/LLM/MCP classifiers, and the graph view could be restored later.

---

## 9. Migration checklist (done)

- [x] Phase 1 — Backend domain + use cases + Tempo infra impl.
- [x] Phase 2 — v2 REST resource registered + sub-resource wired under `EnvironmentResource`.
- [x] Phase 3 — UI service + list page + side-nav entry + shared model files.
- [x] Phase 4 — Trace detail + Flow view + Timeline view.
- [x] Phase 5 — Flow view iteration: flow groups, verbose pre/post diff table, loopback, two-line layout.
- [x] Phase 6 — Debug view (reuse `DebugModeResponseComponent`).
- [x] Phase 7 — Flow view polish: clean L-shaped corner arrows, 120 px cards, 56 px policy icons via `/plugins/policies?expand=icon`, status-code pill on backend card, HTTP method/URL moved to the detail panel, `gravitee.policy.description` shown under the title, `formatFlowName()` shortens `organization-all*` → `org` and strips trailing `-/`.
- [x] Phase 8 — List page polish: filter bar aligned with Logs (`Period · API · Refresh`), API picker via `gio-select-search` + `ApiV2Service`, default page size 10, `API` column header (renamed from "Root Service").
- [x] Phase 9 — Error visualisation end-to-end: backend `Trace.hasError` (TraceQL `status = error` second pass + per-span scan), OTLP status normalisation for both numeric and `STATUS_CODE_*` forms, list status badge, red border + exception block in flow view, red span bars + status pill in timeline view.
- [x] Phase 10 — Request/Response card hardening: Request card uses an inbound-server-span finder (no `http.url`, requires server marker, earliest start wins) so http-callout client spans can't hijack it; Response card has `span: null` to avoid surfacing meaningless aggregation attributes.
- [x] Phase 11 — Backend `rootOperation` derivation: `convertToTrace` and `searchTraces` both prefer `http.target` (+ `http.method`) over `span.name`/`rootTraceName` so policies mutating `http.route` (e.g. http-callout) can't corrupt the displayed path in the list or the Request card subtitle.
- [x] Phase 12 — Debug view running-state propagation: spans without a `gravitee.policy.post` event reuse the previous step's `ParsedState` so the diff shows "unchanged" for security spans (and any other policy with non-verbose tracing) instead of wrongly reporting every header/attribute as removed.

---

## 10. File inventory (new/modified)

### Backend — new files

```
gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/tracing/
  model/Trace.java
  model/TraceSpan.java
  model/TraceEvent.java
  model/TracingGraph.java
  model/TracingNode.java
  model/TracingEdge.java
  model/EdgeSpan.java
  model/TraceSearchCriteria.java
  query_service/TracingQueryService.java
  domain_service/TracingGraphBuilder.java
  use_case/SearchTracesUseCase.java
  use_case/GetTraceUseCase.java
  use_case/GetTracingGraphUseCase.java

gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/infra/query_service/tracing/
  TempoTracingQueryService.java
  TempoHttpClient.java
  TempoTraceResponse.java
  TempoSearchResponse.java

gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/tracing/
  TracingResource.java
```

### Backend — modified files

```
gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/
  GraviteeManagementV2Application.java        (register TracingResource)
  resource/installation/EnvironmentResource.java  (@Path("/tracing") sub-resource)
```

### Frontend — new files

```
gravitee-apim-console-webui/src/services-ngx/
  tracing-v2.service.ts

gravitee-apim-console-webui/src/management/observability/tracing/
  tracing.model.ts
  entity-colors.ts
  tracing-list.component.ts
  tracing-list.component.scss
  trace-detail/trace-detail.component.ts
  trace-detail/trace-detail.component.scss
  trace-detail/timeline-view/timeline-view.component.ts
  trace-detail/flow-view/flow-view.component.ts
  trace-detail/debug-view/debug-view.component.ts
  trace-detail/debug-view/trace-to-debug-response.ts
```

### Frontend — modified files

```
gravitee-apim-console-webui/src/management/observability/observability.module.ts
  (tracing routes)

gravitee-apim-console-webui/src/components/gio-side-nav/gio-side-nav.component.ts
  (Tracing menu entry)

gravitee-apim-console-webui/src/management/api/debug-mode/debug-mode.module.ts
  (export DebugModeResponseComponent so the Debug view can reuse it)
```
