<#ftl output_format="JSON">
<#--
    One data stream template for every type and every classic cluster: the mappings come from common/mapping/, as
    the es9x and serverless templates. The pattern is the data stream name itself, so that an index of another mode
    with the same prefix never becomes a data stream.
-->
<#import "../common/mapping/log-body-field.ftl" as bodyField>
{
    "index_patterns": ["${indexName}"],
    "data_stream": {},
    "priority": 9344593,
    "template": {
        "settings": {
            <#if lifecyclePolicy??>"${lifecyclePolicyPropertyName?json_string}": "${lifecyclePolicy?json_string}",</#if>
            "index.number_of_shards": ${numberOfShards},
            "index.number_of_replicas": ${numberOfReplicas},
            "index.refresh_interval": "${refreshInterval}"
            <#if bodyAnalysis>,
            "analysis": {
                "analyzer": {
                    "gravitee_body_analyzer": {
                        "type": "custom",
                        "tokenizer": "whitespace",
                        "filter": [
                            "lowercase"
                        ]
                    }
                }
            }
            </#if>
            <#if extendedSettings && extendedSettingsTemplate??>,<#include "/${extendedSettingsTemplate}"></#if>
        },
        "mappings": {
            <#include mapping>
        }<#if dataRetention??>,
        "lifecycle": {
            "data_retention": "${dataRetention?json_string}"
        }</#if>
    }
}
