<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}*"],
    "data_stream": {},
    "template": {
        "mappings": {
            <#include "../../common/mapping/decisions-mapping.ftl">
        }
    },
    "priority": 9344593,
    "_meta": {
        "description": "Template for decisions time series data stream"
    }
}
