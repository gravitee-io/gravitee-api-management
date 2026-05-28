# Tracing API — wire contract

Two REST endpoints exposed by `gravitee-gamma-rest-api` for the per-API trace explorer. Use this
document to generate the UI types and HTTP clients; the response shapes here are normative and
pinned by tests in `TracingResourceTest` / domain use case tests.

> **Machine-readable schema:** see
> [`src/main/resources/openapi/openapi-tracing.yaml`](src/main/resources/openapi/openapi-tracing.yaml).
> Feed it to `openapi-typescript` / `openapi-generator` to codegen typed clients. This Markdown
> file is the human-readable narrative; the YAML is the source of truth for tooling.

The `Span` shape is **structurally aligned** with `@gravitee/gamma-lib-observability`'s `TraceSpan`
so a UI built on the shared lib can consume this response without an adapter — see the type table
in **§ Notes for UI integration** below.

## Base path

Both endpoints are mounted under the gamma application:

```
/gamma/organizations/{orgId}/environments/{envId}/observability/traces
```

`orgId` and `envId` accept either a canonical id or an hrid (the server resolves `envId` to the
canonical id before processing).

The caller must always supply two required query parameters:

| Name | Type | Description |
| --- | --- | --- |
| `module` | string | OTel `gravitee.module` value the calling UI cares about — `"aim"` for the LLM / MCP proxy reactors, `"apim"` for the APIM gateway APIs, etc. Without it the request is rejected with **400**. |
| `apiId` | string | API id to scope the query on. Required to keep queries bounded (per-user API scopes can grow to hundreds of APIs) and to defend against trace-id guessing on the detail endpoint. Without it the request is rejected with **400**. |

---

## 1. Search traces — `GET /`

Lists traces matching the scope, ordered by start time descending, paginated.

### Additional query parameters

| Name | Type | Default | Description |
| --- | --- | --- | --- |
| `start` | long (epoch ms) | `end - 24h` | Window lower bound. When both `start` and `end` are omitted, defaults to "last 24h ending at server-now". |
| `end` | long (epoch ms) | server-now | Window upper bound. |
| `page` | int | `0` | Page index (0-based). |
| `perPage` | int | `20` | Page size. |

### 200 OK response

```ts
interface SearchTracesResponse {
  content: TraceSummary[];
  pageNumber: number;       // 0-based, echoes the requested `page`
  pageElements: number;     // count in this page (≤ perPage)
  totalElements: number;    // total matching the scope across all pages
}

interface TraceSummary {
  traceId: string;
  startTimeEpochMs: number; // ms since epoch — pass directly to `new Date(value)`
  durationNanos: number;    // total trace duration
  rootServiceName: string;  // service of the root span
  rootOperationName: string;
  hasError: boolean;        // true iff any span has status === "error"
}
```

Example:

```json
{
  "content": [
    {
      "traceId": "8b1f2e7a3c4d5e6f0a1b2c3d4e5f6a7b",
      "startTimeEpochMs": 1779379000000,
      "durationNanos": 1234000,
      "rootServiceName": "gateway",
      "rootOperationName": "GET /pets",
      "hasError": false
    }
  ],
  "pageNumber": 0,
  "pageElements": 1,
  "totalElements": 42
}
```

### Errors

- **400 Bad Request** — missing or blank `module` / `apiId`.

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
  name: string;                                      // stable id — echoes back as `FilterCondition.field`
  label: string;
  type: 'keyword' | 'string' | 'number' | 'enum' | 'boolean';
  operators: Array<                                  // mirrors @gravitee/gamma-lib-observability FilterOperator
    'eq' | 'neq' | 'contains' | 'not_contains' | 'starts_with' | 'ends_with'
    | 'gt' | 'gte' | 'lt' | 'lte' | 'in' | 'not_in' | 'exists' | 'not_exists'
  >;
  enumValues?: string[];                             // present iff type === 'enum'
  range?: { min?: number; max?: number };            // present iff type === 'number' AND a finite bound applies
}
```

Example:

```json
{
  "data": [
    {
      "name": "STATUS",
      "label": "Status",
      "type": "enum",
      "operators": ["eq", "in"],
      "enumValues": ["unset", "ok", "error"]
    },
    {
      "name": "DURATION_NANOS",
      "label": "Duration",
      "type": "number",
      "operators": ["gt", "gte", "lt", "lte"],
      "range": { "min": 0 }
    }
  ]
}
```

### Why it works this way

The list is aggregated server-side via a Java SPI (`TraceFilterContributor`) discovered through
`java.util.ServiceLoader` at boot. Each gamma module ships its own filter contributions in its
own JAR — `gravitee-gamma-rest-api` itself only ships the common cross-module entries. To add
filters for a new module:

1. Implement `io.gravitee.gamma.rest.core.tracing.port.service_provider.TraceFilterContributor`
   in the module's Maven artifact. Return the module's `gravitee.module` value from `moduleId()`,
   and the list of filter specs from `getFilters()`.
2. Declare the impl class in the module's
   `META-INF/services/io.gravitee.gamma.rest.core.tracing.port.service_provider.TraceFilterContributor`.

No Spring wiring, no runtime registration, no rest-api code change.

---

## 3. Get trace detail — `GET /{traceId}`

Fetches the full span tree for a single trace, with OTel events and gateway payload logs already
stitched onto their parent span by `spanId`.

### Path parameter

| Name | Description |
| --- | --- |
| `traceId` | OTel trace id (16-byte lower-hex). |

### 200 OK response

```ts
interface TraceDetail {
  traceId: string;
  startTimeEpochMs: number;   // ms since epoch — root span start time
  durationNanos: number;      // root span duration
  rootServiceName: string;
  rootOperationName: string;
  hasError: boolean;
  spans: Span[];              // arbitrary order; build the tree client-side via parentSpanId
}

interface Span {
  spanId: string;
  parentSpanId?: string;                // omitted from JSON when null ⇒ trace root
  operationName: string;
  serviceName: string;
  startTimeEpochMs: number;             // ms since epoch
  durationNanos: number;
  status: 'unset' | 'ok' | 'error';     // promoted from OTel `status.code`
  kind: 'internal' | 'server' | 'client' | 'producer' | 'consumer';
  attributes: Record<string, string>;   // span attributes (e.g. "http.method")
  events: SpanEvent[];                  // sorted by timestamp ascending
  payloadLogs: PayloadLog[];            // sorted by timestamp ascending
}

interface SpanEvent {
  name: string;                         // OTel event.name (e.g. "gravitee.policy.pre")
  timestampEpochMs: number;             // ms since epoch
  attributes: Record<string, string>;
}

interface PayloadLog {
  timestampEpochMs: number;             // ms since epoch
  severity: string;                     // log severity ("INFO", "ERROR", ...)
  body: string;                         // captured request / response body, as text
  attributes: Record<string, string>;
}
```

Example:

```json
{
  "traceId": "8b1f2e7a3c4d5e6f0a1b2c3d4e5f6a7b",
  "startTimeEpochMs": 1779379000000,
  "durationNanos": 1234000,
  "rootServiceName": "gateway",
  "rootOperationName": "GET /pets",
  "hasError": false,
  "spans": [
    {
      "spanId": "0a1b2c3d4e5f6a7b",
      "operationName": "GET /pets",
      "serviceName": "gateway",
      "startTimeEpochMs": 1779379000000,
      "durationNanos": 1234000,
      "status": "ok",
      "kind": "server",
      "attributes": {
        "http.method": "GET",
        "http.url": "https://example.com/pets"
      },
      "events": [
        {
          "name": "gravitee.policy.pre",
          "timestampEpochMs": 1779379000350,
          "attributes": { "gravitee.policy.id": "rate-limit" }
        }
      ],
      "payloadLogs": [
        {
          "timestampEpochMs": 1779379000400,
          "severity": "INFO",
          "body": "{\"name\":\"snowflake\"}",
          "attributes": { "gravitee.payload.kind": "request" }
        }
      ]
    }
  ]
}
```

Note that `parentSpanId` is absent from the root span — the lib's `TraceSpan.parentSpanId` is
`string | undefined`, so omission marks the root.

### Errors

- **400 Bad Request** — missing or blank `module` / `apiId`.
- **404 Not Found** — trace doesn't exist, OR the scope filter rejected every span (collapsed from
  the caller's perspective — the server never reveals "a trace with this id exists but in a
  different api / module / env").

---

## Notes for UI integration

### Type alignment with `@gravitee/gamma-lib-observability`

The `Span` shape above is the **canonical lib shape**, not a backend-specific dialect. Field-by-field:

| Field | Lib `TraceSpan` | This wire | Notes |
| --- | --- | --- | --- |
| `startTimeEpochMs` | `number` | `number` | ms since epoch; safe as JS Number forever |
| `durationNanos` | `number` | `number` | ns; max ~104 days as JS Number; matches OTel native |
| `status` | `'unset' \| 'ok' \| 'error'` | same | Promoted from `attributes['otel.status_code']` server-side |
| `kind` | `'internal' \| 'server' \| 'client' \| 'producer' \| 'consumer'` | same | Currently defaults to `'internal'` until the tracing SPI surfaces `span.kind` natively — flagged in the gamma adapter |
| `parentSpanId` | `string \| undefined` | same | Field omitted from JSON when null |
| `attributes` | `Record<string, AttributeValue>` | `Record<string, string>` | The lib's `AttributeValue = string \| number \| boolean \| arrays`. Our wire emits strings — structurally a subset of the lib's type, so no cast needed. |
| `events`, `payloadLogs` | (extension — lib adds optional fields) | required arrays in detail responses | Always present in detail responses (possibly empty); not present in summary rows |

### Practical guidance

- **`startTimeEpochMs`, `timestampEpochMs`** → pass directly to `new Date(value)` or `dayjs(value)`.
  No parsing required.
- **`durationNanos`** is nanoseconds as a JSON `number`. For display divide by `1_000_000` for ms
  or `1_000_000_000` for s. Don't sum across spans without keeping the ns precision.
- **Root span detection**: a span without `parentSpanId` is the trace root. If no span omits
  `parentSpanId` (the trace's real root wasn't ingested), the summary fields still resolve from
  the chronologically-earliest span — render a "partial trace" badge by detecting
  `!spans.some(s => s.parentSpanId === undefined)`.
- **`hasError`** is precomputed server-side from `status === 'error'` on any span. Don't
  re-derive it client-side; the server walks all spans, including ones the UI may filter out.
- **Building the span tree client-side**: spans come in arbitrary order. Group by `parentSpanId`,
  then descend recursively from the root.
- **Pagination is page-number-based** (`page`, `perPage`); the response carries `totalElements`
  for "Page X of N" displays. Pages beyond `totalElements / perPage` return an empty `content`
  array, not an error.
