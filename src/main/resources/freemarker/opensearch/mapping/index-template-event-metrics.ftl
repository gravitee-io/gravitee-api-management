<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}*"],
    "data_stream": {},
    "settings": {
        "index.plugins.index_state_management.policy_id": "event-metrics-ism-policy"
    },
    "mappings": {
        <#include "../../common/mapping/event-metrics-mapping.ftl">
    },
    "priority": 9344593,
    "_meta": {
        "description": "Template for event metrics time series data stream"
    }
}
