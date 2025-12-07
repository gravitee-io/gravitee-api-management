<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}*"],
    "data_stream": {},
    "template": {
        "settings": {
            "index.mode": "time_series",
            "index.lifecycle.name": "event-metrics-ilm-policy"
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
