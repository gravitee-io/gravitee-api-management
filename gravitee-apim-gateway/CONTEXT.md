# Gateway

The runtime component customer traffic hits first when entering the Gravitee ecosystem. Deploys API Definitions as live Reactables and applies Governance to requests/responses flowing between client and backend.

## Language

**API Definition**:
The versioned (v2/v4) configuration object describing a customer API — its plans, flows, endpoints, and policies. Static config, not a running thing.
_Avoid_: API config, API spec, API.

**Reactable**:
The live runtime entity the gateway builds from an API Definition and routes traffic through. Matches the code type `Reactable`/`ReactableApi` (gateway-reactor module).
_Avoid_: Deployed API, Gateway API, live API.

**Governance**:
The act of applying plugins/policies to a customer API's request/response at the gateway. Strictly runtime enforcement — distinct from API Score, which is a design-time linting/quality feature in the rest-api Management API.
_Avoid_: enforcement, policy execution (as a standalone term — use Governance).

**Policy**:
A reusable plugin/rule type the Gateway can execute (rate-limiting, header transform, TLS enforcement, etc.). The behavior; not a configured instance of it.
_Avoid_: Step (a Policy's configured usage-site — see below).

**Step**:
A single configured invocation of a Policy inside a Flow — carries its own `condition` and configuration. The unit a Flow is built from.

**Flow**:
An ordered chain of Steps, grouped by phase (`request`/`response` for HTTP traffic, `subscribe`/`publish` for message/native traffic), that Governance executes between Entrypoint and Endpoint. Scoped to matching traffic by a Selector. Two scopes exist: API-level (on the API/Plan itself) and Organization-level (shared across every API in an Organization, run before/around API-level flows).

**Selector**:
The match rule that scopes a Flow to specific traffic: `HTTP` (path+method), `CHANNEL` (pub/sub operation+entrypoint), `CONDITION` (EL expression), or `MCP`.

**Entrypoint**:
The protocol and configuration a client uses to reach the Gateway for a given API — the client-facing side of an API Definition.

**Endpoint**:
The protocol and configuration the Gateway uses to reach the backend for a given API — the backend-facing side. In v4 APIs, Entrypoint and Endpoint are decoupled, enabling protocol mediation (e.g. a Message API exposing HTTP to the client while talking Kafka to the backend).

## Referenced concepts (owned by rest-api)

Plan, Subscription, Application, Organization, and API Product are created/updated/deleted via the rest-api Management API; the Gateway only reads and enforces them against traffic (HTTP/HTTPS, TCP, or messaging protocols). Full definitions live in [rest-api/CONTEXT.md](../gravitee-apim-rest-api/CONTEXT.md).
