"properties": {
    "gw-id": {
        "type": "keyword"
    },
    "org-id": {
        "type": "keyword"
    },
    "env-id": {
        "type": "keyword"
    },
    "api-id": {
        "type": "keyword"
    },
    "plan-id": {
        "type": "keyword"
    },
    "app-id": {
        "type": "keyword"
    },
    "topic": {
        "type": "keyword"
    },
    "doc-type": {
        "type": "keyword"
    },
    "operation": {
        "type": "keyword"
    },
    "downstream-publish-messages-count-increment": {
        "type": "integer"
    },
    "downstream-publish-message-bytes-increment": {
        "type": "long"
    },
    "upstream-publish-messages-count-increment": {
        "type": "integer"
    },
    "upstream-publish-message-bytes-increment": {
        "type": "long"
    },
    "downstream-subscribe-messages-count-increment": {
        "type": "integer"
    },
    "downstream-subscribe-message-bytes-increment": {
        "type": "long"
    },
    "upstream-subscribe-messages-count-increment": {
        "type": "integer"
    },
    "upstream-subscribe-message-bytes-increment": {
        "type": "long"
    },
    "downstream-active-connections": {
        "type": "integer"
    },
    "upstream-active-connections": {
        "type": "integer"
    },
    "upstream-authenticated-connections": {
        "type": "integer"
    },
    "downstream-authenticated-connections": {
        "type": "integer"
    },
    "downstream-authentication-failures-count-increment": {
        "type": "integer"
    },
    "upstream-authentication-failures-count-increment": {
        "type": "integer"
    },
    "downstream-authentication-successes-count-increment": {
        "type": "integer"
    },
    "upstream-authentication-successes-count-increment": {
        "type": "integer"
    },
    "@timestamp": {
        "type": "date"
    },
    "upstream-durations-nanos": {
        "type": "long"
    },
    "endpoint-durations-nanos": {
        "type": "long"
    },
    "downstream-durations-nanos": {
        "type": "long"
    },
    "upstream-count-increment": {
        "type": "integer"
    },
    "endpoint-upstream-count-increment": {
        "type": "integer"
    },
    "endpoint-downstream-count-increment": {
        "type": "integer"
    },
    "downstream-count-increment": {
        "type": "integer"
    }
    ,
    "event-id": { "type": "keyword" },
    "status": { "type": "keyword" },
    "request-id": { "type": "keyword" },
    "caller": { "type": "keyword" },
    "target-pdp-id": { "type": "keyword" },
    "policy-generation": { "type": "long" },
    "batch-id": { "type": "keyword" },
    "batch-index": { "type": "integer" },
    "batch-size": { "type": "integer" },
    "subject-type": { "type": "keyword" },
    "subject-id": { "type": "keyword" },
    "action": { "type": "keyword" },
    "resource-type": { "type": "keyword" },
    "resource-id": { "type": "keyword" },
    "decision": { "type": "keyword" },
    "matched-policies": {
        "type": "nested",
        "properties": {
            "id": { "type": "keyword" },
            "name": { "type": "keyword" },
            "effect": { "type": "keyword" }
        }
    },
    "reasons": { "type": "keyword" },
    "search-type": { "type": "keyword" },
    "result-count": { "type": "integer" },
    "page-size": { "type": "integer" },
    "has-more": { "type": "boolean" },
    "error-type": { "type": "keyword" },
    "duration-nanos": { "type": "long" }
}
