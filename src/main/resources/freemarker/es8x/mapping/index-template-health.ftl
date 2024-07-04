<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}*"],
    "template": {
        "settings": {
            <#if indexLifecyclePolicyHealth??>"${indexLifecyclePolicyPropertyName}": "${indexLifecyclePolicyHealth}",</#if>
            <#if indexLifecyclePolicyHealth??>"index.lifecycle.rollover_alias": "${indexName}",</#if>
            "index.number_of_shards":${numberOfShards},
            "index.number_of_replicas":${numberOfReplicas},
            "index.refresh_interval": "${refreshInterval}"
            <#if extendedSettingsTemplate??>,<#include "/${extendedSettingsTemplate}"></#if>
        },
        "mappings": {
            "properties": {
                "api": {
                    "type": "keyword"
                },
                "api-name": {
                    "type": "keyword"
                },
                "available": {
                    "type": "boolean",
                    "index": false
                },
                "endpoint": {
                    "type": "keyword"
                },
                "gateway": {
                    "type": "keyword"
                },
                "response-time": {
                    "type": "integer"
                },
                "state": {
                    "type": "integer",
                    "index": false
                },
                "transition": {
                    "type": "boolean"
                },
                "steps": {
                    "type": "object",
                    "enabled": false
                },
                "success": {
                    "type": "boolean",
                    "index": false
                }
            }
        }
    }
}
