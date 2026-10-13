Gravitee API Management, one organization. Read-only tools over what the platform records about the traffic going through its gateways: the analytics engine (metrics of HTTP, message, Kafka, LLM, MCP, edge and authorization traffic), the request logs, and the control-plane changes on an API. The tools return numbers and records; what they mean is yours to judge.

Handles:
- list_environments, then list_apis: the environments you can read and the APIs in them. Every tool takes `environment` (id, HRID or name); omit it when you can read only one.
- describe_metrics: the catalog of the analytics engine, metric by metric, with the measures, facets and filters each takes. The names there are the names query_analytics takes.
- list_filter_values: the ids a filter takes, with their names.

Questions:
- query_analytics: one query, in measures, facets or time_series mode, over a window; `compareWith: previous` returns the same query over the range just before, so that you can see what moved.
- search_logs: the requests themselves, with the reason the gateway recorded when it rejected one.
- list_changes: what was done to an API in the control plane, and by whom.

Acting (only when the operator exposed the write scope on this mount):
- notify: tells the people APIM knows around an API (the owners or members of the subscribed applications, the subscribers) or a webhook what you decided to tell them. A call without confirm is a dry run that shows who would be reached; show it to the user before confirming.

Conventions:
- Time ranges are a length (`window`: 15m, 2h, 1d, default 1h) ending at `until` (ISO-8601 UTC, default now). Never pass epoch values.
- Units are in the catalog and in each result (NUMBER, MILLISECONDS, BYTES, PERCENT). Timestamps are ISO-8601 UTC.
- An API can be named by its name wherever an id is expected.
- You only see the environments and APIs the authenticated user can see.

The prompt `triage` and the resource gravitee://guides/incident-triage carry a method for looking for trouble; it is advice, not a rule.
