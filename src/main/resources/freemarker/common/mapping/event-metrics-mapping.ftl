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
    "request-durations-millis": {
        "type": "long"
    },
    "endpoint-durations-millis": {
        "type": "long"
    },
    "response-durations-millis": {
        "type": "long"
    },
    "requests-count-increment": {
        "type": "integer"
    },
    "endpoint-requests-count-increment": {
        "type": "integer"
    },
    "endpoint-responses-count-increment": {
        "type": "integer"
    },
    "responses-count-increment": {
        "type": "integer"
    }
}