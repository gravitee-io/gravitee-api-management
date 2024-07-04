<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}*"],
    "settings": {
        <#if indexLifecyclePolicyRequest??>"${indexLifecyclePolicyPropertyName}": "${indexLifecyclePolicyRequest}",</#if>
        <#if indexLifecyclePolicyRequest??>"index.lifecycle.rollover_alias": "${indexName}",</#if>
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
            }
        ]
    }
}
