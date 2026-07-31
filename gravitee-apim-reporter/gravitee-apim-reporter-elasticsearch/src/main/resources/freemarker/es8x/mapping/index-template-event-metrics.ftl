<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}*"],
    "data_stream": {},
    "template": {
        "settings": {
            <#if indexLifecyclePolicyEventMetrics?has_content>"${indexLifecyclePolicyPropertyName?json_string}": "${indexLifecyclePolicyEventMetrics?json_string}",</#if>
            <#if indexLifecyclePolicyEventMetrics?has_content>"${indexLifecycleRolloverAliasPropertyName?json_string}": "${indexName}",</#if>
            "index.number_of_shards":${numberOfShards},
            "index.number_of_replicas":${numberOfReplicas},
            "index.refresh_interval": "${refreshInterval}"
        },
        "mappings": {
            <#include "../../common/mapping/event-metrics-mapping.ftl">
        }
    },
    "priority": 9344593,
    "_meta": {
        "description": "Template for event metrics time series data stream"
    }
}
