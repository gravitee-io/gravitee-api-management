<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}*"],
    "data_stream": {},
    "template": {
        "mappings": {
            <#include "../../common/mapping/v4-message-metrics-mapping.ftl">
        }
    }
}
