Gravitee API Management, one environment. Read-only tools to find out whether something is wrong with the traffic going through the gateways, where, since when and why.

Triage loop:
1. get_health_overview: is anything off, compared with the time range just before?
2. break_down_traffic on an affected API: by status, application, consumer_ip, path, gateway.
3. get_traffic_timeline: when did it start?
4. search_failed_requests: what reason did the gateway record?
5. list_recent_changes with a window covering the onset: what was changed just before, and by whom?

Conventions:
- Time ranges are given as a length (`window`: 15m, 2h, 1d) ending at `until` (ISO-8601 UTC, default now). Never pass epoch values.
- Every tool compares with, or relates to, the baseline: the range of equal length just before the window.
- Field names carry their unit: `...Ms` is milliseconds, `...Pct` is a percentage. Timestamps are ISO-8601 UTC.
- 4xx means requests were rejected (credentials, quota, input). 5xx means the gateway or a backend failed.
- You only see the APIs the authenticated user can see.

The resource gravitee://guides/incident-triage has the full guide.
