<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}*"],
    "data_stream": {},
    "template": {
        "settings": {
            <#if indexLifecyclePolicyAuthzDecisions?has_content>"index.plugins.index_state_management.policy_id": "${indexLifecyclePolicyAuthzDecisions?json_string}",</#if>
            "index.number_of_shards":${numberOfShards},
            "index.number_of_replicas":${numberOfReplicas},
            "index.refresh_interval": "${refreshInterval}"
        },
        "mappings": {
            <#include "../../common/mapping/authz-decisions-mapping.ftl">
        }
    },
    "priority": 9344593,
    "_meta": {
        "description": "Template for authorization decisions time series data stream"
    }
}
