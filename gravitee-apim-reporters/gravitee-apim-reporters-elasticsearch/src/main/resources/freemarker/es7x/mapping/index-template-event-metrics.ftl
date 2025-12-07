<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}*"],
    "settings": {
        "index.lifecycle.name": "event-metrics-ilm-policy"
    },
    "mappings": {
      <#include "../../common/mapping/event-metrics-mapping.ftl">
    }
}
