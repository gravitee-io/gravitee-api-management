# Incident triage on a Gravitee organization

## The loop

| Step | Question | Tool |
|---|---|---|
| Detect | Is anything off, in which environment, compared with the time range just before? | `get_health_overview` |
| Localize | Which API, status code, consumer, path, gateway? | `break_down_traffic` |
| Date | When did it start? | `get_traffic_timeline` |
| Explain | What reason did the gateway record? | `search_failed_requests` |
| Explain | What was changed just before, and by whom? | `list_recent_changes` |

Stop as soon as the evidence names a cause. State the cause, the evidence (numbers and timestamps) and the fix.

## Conventions

- **Time.** `window` is a length (`15m`, `2h`, `1d`) ending at `until`, an ISO-8601 UTC instant that defaults to now. No tool takes or returns epoch values. Timestamps in results are ISO-8601 UTC.
- **Baseline.** The range of equal length just before the window. A 15m window ending at 14:00 is compared with 13:30 to 13:45.
- **Units.** `...Ms` is milliseconds, `...Pct` is a percentage of requests.
- **Environment.** `get_health_overview` covers every environment you can read and ranks them. The other tools work on one environment: pass `environment` (id, HRID or name), or let `api` decide when that API exists in only one environment.
- **Visibility.** Results cover the environments and APIs the authenticated user can see, nothing else. Environments you cannot read are listed as omitted, so an empty result never hides them.

## Reading the numbers

- `responseTimeP95Ms` is the time to answer the consumer, backend included. `endpointResponseTimeP95Ms` is the time spent waiting for the backend. When both rise together the backend is slow; when only the first rises the time is spent in the gateway (policies, load).
- A finding needs at least 20 requests in the range it looks at. Below that, rates are noise.
- `SERVER_ERROR_RATE_UP`: 5xx at 1% or more, up by at least one point.
- `CLIENT_ERROR_RATE_UP`: 4xx at 5% or more, up by at least five points.
- `LATENCY_UP`: P95 response time at 1.5 times the baseline or more, up by at least 100 ms.
- `TRAFFIC_DROP`: half the requests of the baseline or fewer.

## Patterns

**A consumer's credential stopped working.** 4xx surge on one API, status 401 or 403. Broken down by application, the consumer's application vanishes and "Unknown (credential not resolved)" appears at the same rate: the gateway rejects the request before it can tell who sent it. Broken down by `consumer_ip`, the same address is on both sides. Failed requests carry a security error key. Look in `list_recent_changes` for `APIKEY_REVOKED`, `APIKEY_EXPIRED`, `SUBSCRIPTION_CLOSED`, `SUBSCRIPTION_PAUSED`, `PLAN_CLOSED` or a deployment that changed the plan's security.

**A backend fails.** 5xx surge on one API. Failed requests have no error key when the backend answered 5xx itself, or a gateway error key when it did not answer (timeout, connection refused). Break down by path to see whether one operation is concerned.

**A backend slows down.** `LATENCY_UP` with `endpointResponseTimeP95Ms` rising as much as `responseTimeP95Ms`.

**A deployment broke an API.** Onset seconds after a `PUBLISH_API` in `list_recent_changes`.

**One gateway is off.** Break the environment down by gateway: one gateway carries the errors or lost its traffic.

**A consumer went away.** `TRAFFIC_DROP` without errors. Break down by application: one application vanished and nothing replaced it. The cause is on the consumer's side.
