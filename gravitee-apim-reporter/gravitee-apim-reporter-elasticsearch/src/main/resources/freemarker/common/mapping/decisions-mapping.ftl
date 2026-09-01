"dynamic_templates": [
    {
        "additional_long_metrics": {
            "path_match": "additional-metrics.long_*",
            "mapping": {
                "type": "long"
            }
        }
    },
    {
        "additional_int_metrics": {
            "path_match": "additional-metrics.int_*",
            "mapping": {
                "type": "integer"
            }
        }
    },
    {
        "additional_double_metrics": {
            "path_match": "additional-metrics.double_*",
            "mapping": {
                "type": "double"
            }
        }
    },
    {
        "additional_bool_metrics": {
            "path_match": "additional-metrics.bool_*",
            "mapping": {
                "type": "boolean"
            }
        }
    },
    {
        "additional_keyword_metrics": {
            "path_match": "additional-metrics.keyword_*",
            "mapping": {
                "type": "keyword",
                "ignore_above": 1024
            }
        }
    },
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
    "event-id": {
        "type": "keyword"
    },
    "case-id": {
        "type": "keyword"
    },
    "batch-id": {
        "type": "keyword"
    },
    "phase": {
        "type": "keyword"
    },
    "decision-point-type": {
        "type": "keyword"
    },
    "decision-point-id": {
        "type": "keyword"
    },
    "decision-point-version": {
        "type": "keyword"
    },
    "checkpoint": {
        "type": "keyword",
        "ignore_above": 1024
    },
    "caller": {
        "type": "keyword"
    },
    "subject-type": {
        "type": "keyword",
        "ignore_above": 1024
    },
    "subject-id": {
        "type": "keyword",
        "ignore_above": 1024
    },
    "actor-type": {
        "type": "keyword",
        "ignore_above": 1024
    },
    "actor-id": {
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
    "args-hash": {
        "type": "keyword",
        "ignore_above": 1024
    },
    "outcome": {
        "type": "keyword"
    },
    "enforced": {
        "type": "keyword"
    },
    "verdict": {
        "type": "keyword",
        "ignore_above": 1024
    },
    "indeterminate-cause": {
        "type": "keyword"
    },
    "confidence": {
        "type": "half_float"
    },
    "reasons": {
        "type": "keyword"
    },
    "matched-rules": {
        "type": "nested",
        "properties": {
            "id": { "type": "keyword" },
            "name": { "type": "keyword" },
            "effect": { "type": "keyword" }
        }
    },
    "transformed": {
        "type": "boolean"
    },
    "transformation-type": {
        "type": "keyword"
    },
    "required-approver": {
        "type": "keyword",
        "ignore_above": 1024
    },
    "decider-type": {
        "type": "keyword",
        "ignore_above": 1024
    },
    "decider-id": {
        "type": "keyword",
        "ignore_above": 1024
    },
    "channel": {
        "type": "keyword"
    },
    "request-id": {
        "type": "keyword"
    },
    "trace-id": {
        "type": "keyword"
    },
    "conversation-id": {
        "type": "keyword"
    },
    "mission-id": {
        "type": "keyword"
    },
    "status": {
        "type": "keyword"
    },
    "error-type": {
        "type": "keyword"
    },
    "duration-nanos": {
        "type": "long"
    },
    "waited-nanos": {
        "type": "long"
    }
}
