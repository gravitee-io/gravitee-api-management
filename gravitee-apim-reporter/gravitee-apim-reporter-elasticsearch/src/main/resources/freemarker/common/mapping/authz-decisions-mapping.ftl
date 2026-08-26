"dynamic_templates": [
    {
        "strings_as_keywords": {
            "match_mapping_type": "string",
            "mapping": {
                "type": "keyword",
                "ignore_above": 1024
            }
        }
    }
],
"properties": {
    "@timestamp": {
        "type": "date"
    },
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
    "doc-type": {
        "type": "keyword"
    },
    "operation": {
        "type": "keyword"
    },
    "event-id": {
        "type": "keyword"
    },
    "status": {
        "type": "keyword"
    },
    "request-id": {
        "type": "keyword"
    },
    "caller": {
        "type": "keyword"
    },
    "target-pdp-id": {
        "type": "keyword"
    },
    "policy-generation": {
        "type": "long"
    },
    "batch-id": {
        "type": "keyword"
    },
    "batch-index": {
        "type": "integer"
    },
    "batch-size": {
        "type": "integer"
    },
    "subject-type": {
        "type": "keyword",
        "ignore_above": 1024
    },
    "subject-id": {
        "type": "keyword",
        "ignore_above": 1024
    },
    "action": {
        "type": "keyword",
        "ignore_above": 1024
    },
    "resource-type": {
        "type": "keyword",
        "ignore_above": 1024
    },
    "resource-id": {
        "type": "keyword",
        "ignore_above": 1024
    },
    "decision": {
        "type": "keyword"
    },
    "matched-policies": {
        "type": "nested",
        "properties": {
            "id": { "type": "keyword" },
            "name": { "type": "keyword" },
            "effect": { "type": "keyword" }
        }
    },
    "reasons": {
        "type": "keyword"
    },
    "search-type": {
        "type": "keyword"
    },
    "result-count": {
        "type": "integer"
    },
    "page-size": {
        "type": "integer"
    },
    "has-more": {
        "type": "boolean"
    },
    "error-type": {
        "type": "keyword"
    },
    "duration-nanos": {
        "type": "long"
    }
}
