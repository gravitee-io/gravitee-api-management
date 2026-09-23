<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}*"],
    "data_stream": {},
    "template": {
        "settings": {
            <#if indexLifecyclePolicyDecisions?has_content>"index.plugins.index_state_management.policy_id": "${indexLifecyclePolicyDecisions?json_string}",</#if>
            "index.number_of_shards":${numberOfShards},
            "index.number_of_replicas":${numberOfReplicas},
            "index.refresh_interval": "${refreshInterval}"
        },
        "mappings": {
            <#include "../../common/mapping/decisions-mapping.ftl">
        }
    },
    "priority": 9344593,
    "_meta": {
        "description": "Template for decisions time series data stream"
    }
}
