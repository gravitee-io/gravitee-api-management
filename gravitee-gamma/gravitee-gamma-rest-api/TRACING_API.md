# Tracing API — wire contract

Four REST endpoints exposed by `gravitee-gamma-rest-api` for the per-API trace explorer. This
document is the human-readable narrative; the response shapes here are normative and pinned by
tests in `TracingResourceTest` / domain use case tests.

> **Machine-readable schema:** see
> [`src/main/resources/openapi/openapi-tracing.yaml`](src/main/resources/openapi/openapi-tracing.yaml).
> Feed it to `openapi-typescript` / `openapi-generator` to codegen typed clients.

## Base path

All endpoints are mounted under the gamma application:

```
/gamma/organizations/{orgId}/environments/{envId}/observability/traces
```

`orgId` and `envId` accept either a canonical id or an hrid (the server resolves `envId` to the
canonical id before processing).

### Per-API scope, mounted once

The trace explorer is mounted at a **non-module-namespaced URL** so every gamma module's UI calls
the same endpoint. The per-call scope is:

| Endpoint | Scope dimension |
| --- | --- |
| `POST /search` | required `apiId` in the body |
| `GET /{traceId}` | required `apiId` query param |
| `GET /filters/definition` | optional `module` query param |
| `GET /filters/{name}/values` | optional `module` query param |

**Why `apiId` and not `module` on search/detail.** Each API has a type (`LLM_PROXY` → AIM,
`HTTP_PROXY` → APIM, …) that defines its module. The `apiId` is unique across modules — filtering
on `gravitee.api.id` server-side gives module-correct results without a redundant `gravitee.module`
filter. `module` stays on the filter-discovery endpoints because their entire purpose is "which
filters does this module's contributor expose?" — different from query scoping.

**Why `apiId` is required, not optional.** Per-user authorization is at the per-API level (users
see APIs they created or were granted access to). The backend cannot enumerate a user's accessible
APIs into a wide OR-filter at search time because that pattern doesn't work for Tempo (which
doesn't efficiently OR over hundreds of api-ids). Instead the API picker is on the UI side — the
user picks one of their visible APIs from a mandatory dropdown before any search runs. On the
detail endpoint `apiId` is an additional security defense: a known trace id from API_B can't be
fetched via API_A's scope, because the server-side resource-attribute filter rejects spans from
the wrong API and the endpoint returns 404 (collapsing "doesn't exist" and "wrong scope" so
cross-API existence cannot be probed).

## Lib ↔ Wire translation

The wire matches the apim management v2 analytics + logs surface. The lib's TS types
(`@gravitee/gamma-lib-observability`) deliberately diverge for UI ergonomics; the lib's HTTP
source translates between the two. The data source is the integration boundary — consumers should
not assume the lib types match the wire field-for-field.

| Lib TS (UI model)                          | Wire                                            | Translated by                                                    |
| ------------------------------------------ | ----------------------------------------------- | ---------------------------------------------------------------- |
| `FilterCondition.field`                    | `name`                                          | lib's `toWireFilter` (`src/data-sources/filter-serialization.ts`)|
| `FilterCondition.value: string[]`          | `value: string \| string[] \| number`           | lib's `toWireFilter` — polymorphic per operator                  |
| `FilterCondition.operator: 'eq'` (lower)   | `"EQ"` (UPPER)                                  | lib's `toWireFilter` — uppercased                                |
| `FilterCondition.label / valueLabels`      | (omitted)                                       | lib synthesises from `TraceFilterSpec` for enums, raw value otherwise |
| `TraceSpan.startTime / endTime / duration` | `startTimeEpochMs` + `durationNanos`            | lib adapter (renames; computes `endTime = start + duration/1e6`) |
| `TraceSpan.attributes: AttributeValue`     | `attributes: Map<String,String>`                | lib adapter (lossy — typed attributes tracked as a follow-up)    |
| `WireTraceSummary.status: 'unset'\|'ok'\|'error'` | `status` (same lowercase union)         | none — backend emits the lib's exact vocabulary                  |
| `WireTraceSummary.spanCount: number`       | `spanCount: int`                                | none — backend emits the bucket `doc_count` (list) / `spans.length` (detail) |

**`endTime` is intentionally not emitted** on the wire — it's purely derivable as
`startTimeEpochMs + durationNanos / 1_000_000`. Emitting it would risk drift between stored and
computed values and adds no information.

**`durationNanos` is nanosecond-precision on purpose.** Fast spans (validation hooks, service-mesh
ops, internal policy steps) are sub-millisecond — rounding to ms would round them all to 0 and
lose the resolution needed for waterfall layout and percentile analysis.

**Time on the request side**: `from` / `to` are ISO-8601 strings, matching the apim management v2
`TimeRange` convention. The mixed units (ISO for time-range, epoch-ms for span timestamps,
nanoseconds for durations) follow OTel-native conventions on the response side and the apim
management v2 convention on the request side.

---

## 1. Search traces — `POST /search?page=&perPage=`

Lists traces matching the scope, ordered by start time descending, paginated. Page + page size
travel in the **query string** (matches apim's `/logs/search` convention); scope, time, and
filters travel in the **body**.

### Request body

```ts
interface SearchTracesRequest {
  apiId: string;                       // required
  timeRange?: {
    from: string;                      // ISO-8601 (e.g. "2026-05-29T00:00:00Z")
    to: string;
  };
  filters?: FilterCondition[];
}

interface FilterCondition {
  name: string;                        // e.g. "HTTP_METHOD"
  operator: 'EQ' | 'NEQ' | 'IN' | 'NOT_IN' | 'GTE' | 'LTE' | 'CONTAINS' | …;
  value: string | string[] | number;   // polymorphic per operator
}
```

When the time window is not pinned, the use case defaults to the last 24h ending at server-now.
When only `to` is provided, `from` defaults to `to - 24h`.

### 200 OK response

```ts
interface SearchTracesResponse {
  data: TraceSummary[];
  pagination: {
    totalCount: number;
    page: number;                      // 1-based
    pageCount: number;                 // ceil(totalCount / perPage)
  };
}

interface TraceSummary {
  traceId: string;
  startTimeEpochMs: number;
  durationNanos: number;
  rootServiceName: string;
  rootOperationName: string;
  status: 'unset' | 'ok' | 'error';  // trace-level rollup; any ERROR span → 'error', else root's status
  spanCount: number;                  // total spans for the trace in the search window
}
```

Example request:

```http
POST /gamma/organizations/DEFAULT/environments/DEFAULT/observability/traces/search?page=1&perPage=20

{
  "apiId": "api-1",
  "timeRange": { "from": "2026-05-29T00:00:00Z", "to": "2026-05-30T00:00:00Z" },
  "filters": [
    { "name": "HTTP_STATUS_CODE", "operator": "EQ", "value": "500" }
  ]
}
```

Example response:

```json
{
  "data": [
    {
      "traceId": "8b1f2e7a3c4d5e6f0a1b2c3d4e5f6a7b",
      "startTimeEpochMs": 1779379000000,
      "durationNanos": 1234000,
      "rootServiceName": "gateway",
      "rootOperationName": "GET /pets",
      "status": "error",
      "spanCount": 12
    }
  ],
  "pagination": { "totalCount": 42, "page": 1, "pageCount": 3 }
}
```

### Errors

- **400 Bad Request**:
  - `tracing.scope.missing` — `apiId` is missing / blank in the body.
  - `tracing.filter.unknown_name` — a `FilterCondition.name` isn't in the registry.
  - `tracing.filter.unsupported_operator` — the operator is listed for the filter but the
    translator doesn't handle it yet. Today the MVP only translates `EQ`; the follow-up PR
    adds `IN` / range / etc.

---

## 2. Discover available filters — `GET /filters/definition`

Self-describing list of filter specs the UI uses to build its chip palette without hardcoded
knowledge of what the backend supports.

### Query parameter

| Name | Required | Description |
| --- | --- | --- |
| `module` | **no** | When omitted, returns only cross-module filters. When supplied, additionally pulls in the module-specific contributor's filters (last-write-wins by `name`). |

### 200 OK response

```ts
interface TraceFilterSpecsResponse {
  data: TraceFilterSpec[];
}

interface TraceFilterSpec {
  name: string;                                      // stable id, echoed back as wire `name`
  label: string;
  type: 'keyword' | 'string' | 'number' | 'enum' | 'boolean';
  operators: Array<                                  // UPPERCASE on the wire
    'EQ' | 'NEQ' | 'CONTAINS' | 'NOT_CONTAINS' | 'STARTS_WITH' | 'ENDS_WITH'
    | 'GT' | 'GTE' | 'LT' | 'LTE' | 'IN' | 'NOT_IN' | 'EXISTS' | 'NOT_EXISTS'
  >;
  enumValues?: string[];                             // present iff type === 'enum'
  range?: { min?: number; max?: number };            // present iff type === 'number' AND a finite bound applies
}
```

Example (MVP — what the server returns today):

```json
{
  "data": [
    { "name": "HTTP_METHOD", "label": "HTTP method", "type": "enum", "operators": ["EQ"], "enumValues": ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"] },
    { "name": "HTTP_STATUS_CODE", "label": "HTTP status code", "type": "number", "operators": ["EQ"], "range": { "min": 100, "max": 599 } },
    { "name": "HTTP_ROUTE", "label": "HTTP route", "type": "string", "operators": ["EQ"] }
  ]
}
```

### MVP scope

Today's discovery surface is deliberately narrow — the server only exposes filters / operators
that the search endpoint's translator can actually route to ES. The follow-up PR extends the
`TraceFilterContributor` SPI with per-filter translation logic and lifts:

- **More operators** on the surviving three filters — `IN` / `NOT_IN`, `GTE` / `LTE` on
  `HTTP_STATUS_CODE`, `CONTAINS` on `HTTP_ROUTE`.
- **Five more filters** that need richer query shapes: `STATUS` (top-level `status.code`),
  `HAS_ERROR` (trace-wide aggregation), `DURATION_NANOS` (top-level `duration` range),
  `OPERATION_NAME` (top-level `name`), `SPAN_KIND` (top-level `kind`).

`SERVICE_NAME` is intentionally not exposed today — with the gateway as the sole OTel emitter,
every span carries the same `service.name` value. Reconsidered when multi-source ingestion lands.

---

## 3. List allowed values for a filter — `GET /filters/{filterName}/values`

Returns a paginated list of allowed values for a single filter. The UI uses this to power
chip-autocomplete suggestions next to the filter palette returned by `GET /filters/definition`.
Behaviour is **type-driven**, mirroring the apim management v2
`GET /observability/filters/{name}/values`.

### Path parameter

| Name | Description |
| --- | --- |
| `filterName` | Must match a `TraceFilterSpec.name` from `GET /filters/definition`. Anything else → `404`. |

### Query parameters

| Name | Required | Description |
| --- | --- | --- |
| `module` | no | When set, picks up the module-specific filter contributor's entries too. |
| `query` | no | Case-insensitive substring filter applied to the value list. |
| `page` | no | **1-based** page number (default `1`). |
| `perPage` | no | Page size (default `10`, server-side max `100`). |

### 200 OK response

Same envelope as the search endpoint:

```ts
interface TraceFilterValuesResponse {
  data: { value: string; label?: string }[];
  pagination: {
    totalCount: number;
    page: number;
    pageCount: number;
  };
}
```

`label` is omitted when the value is already display-friendly. The MVP only exposes ENUM
filters whose values ARE the labels, so every emitted entry has just `value` today.

### Type-driven behaviour

| Filter type | Behaviour |
| --- | --- |
| `enum` | Returns the spec's `enumValues`, optionally substring-filtered by `query`, then paginated. |
| `keyword` | Would return distinct values from the data store. **No keyword filter ships in the MVP**, so the use case throws `UnsupportedFilterException.valueListingNotSupported` for this branch today. The follow-up PR enables the data-store path. |
| `number` / `string` / `boolean` | No enumerable value pool → returns `400` with `technicalCode: tracing.filter.value_listing_not_supported`. |

### Label resolution

The wire doesn't carry a `valueLabels` array on `FilterCondition`. The lib (and any other client)
populates labels however it needs to:

- **ENUM filters** — lib reads the label from the cached `TraceFilterSpec.enumValues` for the same
  filter name. No round-trip needed.
- **NUMBER / STRING filters** — no label concept; the lib gracefully falls back to displaying the
  raw value (see `@gravitee/gamma-lib-observability`'s `normalize-filter-value.ts`).
- **KEYWORD filters** — would benefit from a bulk-resolve endpoint analogous to apim analytics'
  `POST /environments/{envId}/observability/filters/resolve`. **Not implemented in the MVP**
  (no keyword filter exists yet). When the first one ships (e.g. AIM's `LLM_MODEL`), either reuse
  apim's resolver for shared keys (e.g. `API_ID`) or add a tracing-side `POST /filters/resolve`
  endpoint following the same shape.

### Errors

| Status | When | Technical code |
| --- | --- | --- |
| `400` | Type doesn't support value listing. | `tracing.filter.value_listing_not_supported` |
| `404` | `filterName` not in the registry. | *(none — `NotFoundDomainException` envelope)* |

---

## 4. Get trace detail — `GET /{traceId}?apiId=…`

Fetches the full span tree for a single trace, with OTel events and gateway payload logs already
stitched onto their parent span by `spanId`.

### Path parameter

| Name | Description |
| --- | --- |
| `traceId` | OTel trace id (16-byte lower-hex). |

### Query parameter (required)

| Name | Description |
| --- | --- |
| `apiId` | API id. Required as a security defense — see the "Per-API scope" section at the top. |

### 200 OK response

```ts
interface TraceDetail {
  traceId: string;
  startTimeEpochMs: number;   // ms since epoch — root span start time
  durationNanos: number;      // root span duration in ns
  rootServiceName: string;
  rootOperationName: string;
  status: 'unset' | 'ok' | 'error';  // same rollup as TraceSummary.status
  spanCount: number;                  // equal to spans.length
  spans: Span[];
}

interface Span {
  traceId: string;            // echoed on every span (OTel-native + lib requires it)
  spanId: string;
  parentSpanId?: string;      // omitted when null = root span
  operationName: string;
  serviceName: string;
  startTimeEpochMs: number;
  durationNanos: number;
  status: 'unset' | 'ok' | 'error';
  kind: 'internal' | 'server' | 'client' | 'producer' | 'consumer';
  attributes: Record<string, string>;
  events: SpanEvent[];        // OTel events stitched in by spanId
  payloadLogs: PayloadLog[];  // gateway-captured payload logs stitched in by spanId
}

interface SpanEvent {
  name: string;
  timestampEpochMs: number;
  attributes: Record<string, string>;
}

interface PayloadLog {
  timestampEpochMs: number;
  severity: string;
  body: string;
  attributes: Record<string, string>;
}
```

### Errors

- **400** if `apiId` is missing.
- **404** if the trace doesn't exist OR exists but in a different `apiId` scope (collapsed).

---

## Notes for UI integration

### Type alignment with `@gravitee/gamma-lib-observability`

See the Lib ↔ Wire translation table at the top. The short version: the lib's data source is the
adapter boundary. It translates `field`↔`name`, `value` polymorphism, operator casing, and the
`startTime`/`endTime`/`duration` rename. Consumers that build on top of the lib's UI types
(`FilterCondition`, `TraceSpan`) don't see the wire directly.

### Practical guidance

- **Time**: always use absolute ISO-8601 on the wire (`timeRange.from` / `to`). Compute
  `endTime = startTimeEpochMs + durationNanos / 1_000_000` client-side when needed.
- **Pagination**: 1-based. `pageCount` is provided server-side; "no more pages" is
  `page >= pageCount` or `data.length === 0`.
- **Operators on the wire are UPPERCASE.** Internal lib `FilterOperator` and any UI state should
  stay lowercase; uppercase at the wire boundary only.
- **Polymorphic `value`**: send a string for `EQ`/`NEQ`/`CONTAINS`, an array for `IN`/`NOT_IN`,
  a number for `GTE`/`LTE`. Discriminated by `operator` server-side.
