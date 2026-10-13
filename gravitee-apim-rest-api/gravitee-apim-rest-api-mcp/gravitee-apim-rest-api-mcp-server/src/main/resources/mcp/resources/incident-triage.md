# Looking for trouble on a Gravitee organization

A method, not a rule. Every step is a question to the data; the thresholds are yours, and they depend on the API, the hour and the volume. Say what you base each conclusion on.

## 1. Orient

`list_environments`, then `list_apis` for the environment at hand. `describe_metrics` once, to know the vocabulary: for HTTP traffic, `HTTP_REQUESTS` (COUNT), `HTTP_GATEWAY_RESPONSE_TIME` (the time to answer the consumer, backend included), `HTTP_ENDPOINT_RESPONSE_TIME` (the time spent waiting for the backend); message, Kafka, LLM and authorization traffic have their own.

## 2. Compare with the range just before

`query_analytics` with `compareWith: previous` is the cheapest way to see what moved. A useful first query, in `facets` mode: `HTTP_REQUESTS` COUNT by `API` then `HTTP_STATUS_CODE_GROUP`, with `HTTP_GATEWAY_RESPONSE_TIME` P95 beside it. Read every rate against its volume: a hundred percent of three requests is three requests. Keep 4xx and 5xx apart: 4xx means the gateway or the backend rejected the request (a credential, a quota, bad input), 5xx means something failed. A response time that rises with `HTTP_ENDPOINT_RESPONSE_TIME` points at the backend; one that rises without it points at the gateway.

The previous range is not always a fair baseline: the hour before a peak, the Sunday before a Monday. Widen or move the window (`until`) when it is not.

## 3. Localize

Split what moved along one dimension at a time, still comparing with the previous range: by `HTTP_STATUS` to see the exact code; by `APPLICATION` to see which consumer (the application `1`, labelled Unknown, is traffic the gateway rejected before it could tell who sent it); by `CONSUMER_IP` to tell a new client from an old one; by `HTTP_PATH` to see whether one operation is concerned; by `GATEWAY` to see whether one gateway is off. `list_filter_values` gives the ids a filter wants.

## 4. Date

`query_analytics` in `time_series` mode over the same metrics, with an interval that gives about thirty points. The first point that departs from the previous range is the onset.

## 5. Explain

`search_logs` around the onset, filtered on the status class or the API: the `errorKey` names why the gateway rejected a request, the `errorComponent` names which policy or connector did; no error key means the backend answered that status itself. Then `list_changes` on the API with a window that covers the onset: a deployment, a plan change, a closed subscription or a revoked key seconds before it is the first suspect.

## Shapes seen before

- A consumer's credential stopped working: 4xx on one API, status 401 or 403; by application the consumer vanishes and Unknown appears at the same rate; same consumer IP on both sides; a security error key; `APIKEY_REVOKED`, `APIKEY_EXPIRED`, `SUBSCRIPTION_CLOSED` or a plan change in `list_changes`.
- A backend fails: 5xx on one API; no error key when the backend answered, a gateway error key when it did not (timeout, connection refused).
- A backend slows down: `HTTP_ENDPOINT_RESPONSE_TIME` rising as much as `HTTP_GATEWAY_RESPONSE_TIME`.
- A deployment broke an API: onset seconds after a `PUBLISH_API`.
- One gateway is off: by `GATEWAY`, one carries the errors or lost its traffic.
- A consumer went away: fewer requests, no errors; by application, one vanished and nothing replaced it.
