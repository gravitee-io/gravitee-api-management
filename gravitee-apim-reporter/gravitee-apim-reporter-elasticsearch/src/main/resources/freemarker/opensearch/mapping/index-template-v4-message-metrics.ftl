<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}*"],
    "template": {
        "settings": {
            <#if indexLifecyclePolicyRequest??>"index.plugins.index_state_management.policy_id": "${indexLifecyclePolicyRequest}",</#if>
            <#if indexLifecyclePolicyRequest??>"index.plugins.index_state_management.rollover_alias": "${indexName}",</#if>
            "index.number_of_shards":${numberOfShards},
            "index.number_of_replicas":${numberOfReplicas},
            "index.refresh_interval": "${refreshInterval}"
            <#if extendedSettingsTemplate??>,<#include "/${extendedSettingsTemplate}"></#if>
        },
        "mappings": {
            "properties": {
                "gateway": {
                    "type": "keyword"
                },
                "org-id": {
                    "type": "keyword"
                },
                "env-id": {
                    "type": "keyword"
                },
                "@timestamp": {
                    "type": "date"
                },
                "api-id": {
                    "type": "keyword"
                },
                "api-name": {
                    "type": "keyword"
                },
                "request-id": {
                    "type": "keyword"
                },
                "client-identifier": {
                    "type": "keyword"
                },
                "correlation-id": {
                    "type": "keyword"
                },
                "parent-correlation-id": {
                    "type": "keyword"
                },
                "operation": {
                    "type": "keyword"
                },
                "connector-type": {
                    "type": "keyword"
                },
                "connector-id": {
                    "type": "keyword"
                },
                "content-length": {
                    "type": "integer"
                },
                "count": {
                    "type": "integer"
                },
                "error-count": {
                    "type": "integer"
                },
                "count-increment": {
                    "type": "integer"
                },
                "error-count-increment": {
                    "type": "integer"
                },
                "error": {
                    "type": "boolean"
                },
                "gateway-latency-ms": {
                    "type": "integer"
                }
                <#if extendedRequestMappingTemplate??>,<#include "/${extendedRequestMappingTemplate}"></#if>
            },
            "dynamic_templates": [
                {
                    "strings_as_keywords": {
                        "path_match": "custom.*",
                        "match_mapping_type": "string",
                        "mapping": {
                            "type": "keyword"
                        }
                    }
                },
                {
                    "additional_long_metrics": {
                        "path_match": "additional-metrics.long_*",
                        "match_mapping_type": "long",
                        "mapping": {
                            "type": "long"
                        }
                    }
                },
                {
                    "additional_keyword_metrics": {
                        "path_match": "additional-metrics.keyword_*",
                        "match_mapping_type": "string",
                        "mapping": {
                            "type": "keyword"
                        }
                    }
                },
                {
                    "additional_boolean_metrics": {
                        "path_match": "additional-metrics.bool_*",
                        "mapping": {
                            "type": "boolean"
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
                    "additional_int_metrics": {
                        "path_match": "additional-metrics.int_*",
                        "mapping": {
                            "type": "integer"
                        }
                    }
                },
                {
                    "additional_string_metrics": {
                        "path_match": "additional-metrics.string_*",
                        "mapping": {
                            "type": "text"
                        }
                    }
                },
                {
                    "additional_json_metrics": {
                        "path_match": "additional-metrics.json_*",
                        "mapping": {
                            "type": "text"
                        }
                    }
                }
            ]
        }
    }
}
