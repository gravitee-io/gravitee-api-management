"properties": {
    "gw-id": {
        "type": "keyword",
        "time_series_dimension": true
    },
    "org-id": {
        "type": "keyword",
        "time_series_dimension": true
    },
    "env-id": {
        "type": "keyword",
        "time_series_dimension": true
    },
    "api-id": {
        "type": "keyword",
        "time_series_dimension": true
    },
    "plan-id": {
        "type": "keyword",
        "time_series_dimension": true
    },
    "app-id": {
        "type": "keyword",
        "time_series_dimension": true
    },
    "topic": {
        "type": "keyword",
        "time_series_dimension": true
    },
    "doc-type": {
        "type": "keyword",
        "time_series_dimension": true
    },
    "operation": {
        "type": "keyword",
        "time_series_dimension": true
    },
    "downstream-publish-messages-total": {
        "type": "integer",
        "time_series_metric": "counter"
    },
    "downstream-publish-message-bytes": {
        "type": "long",
        "time_series_metric": "counter"
    },
    "upstream-publish-messages-total": {
        "type": "integer",
        "time_series_metric": "counter"
    },
    "upstream-publish-message-bytes": {
        "type": "long",
        "time_series_metric": "counter"
    },
    "downstream-subscribe-messages-total": {
        "type": "integer",
        "time_series_metric": "counter"
    },
    "downstream-subscribe-message-bytes": {
        "type": "long",
        "time_series_metric": "counter"
    },
    "upstream-subscribe-messages-total": {
        "type": "integer",
        "time_series_metric": "counter"
    },
    "upstream-subscribe-message-bytes": {
        "type": "long",
        "time_series_metric": "counter"
    },
    "downstream-active-connections": {
        "type": "integer",
        "time_series_metric": "gauge"
    },
    "upstream-active-connections": {
        "type": "integer",
        "time_series_metric": "gauge"
    },
    "upstream-authenticated-connections": {
        "type": "integer",
        "time_series_metric": "gauge"
    },
    "downstream-authenticated-connections": {
        "type": "integer",
        "time_series_metric": "gauge"
    },
    "downstream-authentication-failures-total": {
        "type": "integer",
        "time_series_metric": "counter"
    },
    "upstream-authentication-failures-total": {
        "type": "integer",
        "time_series_metric": "counter"
    },
    "downstream-authentication-successes-total": {
        "type": "integer",
        "time_series_metric": "counter"
    },
    "upstream-authentication-successes-total": {
        "type": "integer",
        "time_series_metric": "counter"
    },
    "@timestamp": {
        "type": "date"
    },
    "request-durations-millis": {
        "type": "long",
        "time_series_metric": "gauge"
    },
    "endpoint-durations-millis": {
        "type": "long",
        "time_series_metric": "gauge"
    },
    "response-durations-millis": {
        "type": "long",
        "time_series_metric": "gauge"
    },
    "requests-total": {
        "type": "integer",
        "time_series_metric": "counter"
    },
    "endpoint-requests-total": {
        "type": "integer",
        "time_series_metric": "counter"
    },
    "endpoint-responses-total": {
        "type": "integer",
        "time_series_metric": "counter"
    },
    "responses-total": {
        "type": "integer",
        "time_series_metric": "counter"
    }
}