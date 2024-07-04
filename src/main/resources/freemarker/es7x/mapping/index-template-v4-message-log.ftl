<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}*"],
    "settings": {
        <#if indexLifecyclePolicyLog??>"${indexLifecyclePolicyPropertyName}": "${indexLifecyclePolicyLog}",</#if>
        <#if indexLifecyclePolicyLog??>"index.lifecycle.rollover_alias": "${indexName}",</#if>
        "index.number_of_shards":${numberOfShards},
        "index.number_of_replicas":${numberOfReplicas},
        "index.refresh_interval": "${refreshInterval}"
        <#if extendedSettingsTemplate??>,<#include "/${extendedSettingsTemplate}"></#if>
    },
    "mappings": {
        "properties": {
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
            "message": {
                "type": "object",
                "properties": {
                    "id": {
                        "type": "keyword"
                    },
                    "payload":{
                        "type": "text"
                    },
                    "headers":{
                        "enabled": false,
                        "type": "object"
                    },
                    "metadata":  {
                       "enabled": false,
                       "type": "object"
                    },
                    "error":  {
                        "type": "boolean"
                    }
                }
            }
        }
    }
}
