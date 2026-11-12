<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}*"],
    "data_stream": {},
    "template": {
        "mappings": {
            <#include "../../common/mapping/request-mapping.ftl">
        }
    }
}
