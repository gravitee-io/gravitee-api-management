<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}*"],
    "template": {
        "settings": {
            <#if indexLifecyclePolicyRequest?has_content>"${indexLifecyclePolicyPropertyName?json_string}": "${indexLifecyclePolicyRequest?json_string}",</#if>
            <#if indexLifecyclePolicyRequest?has_content>"${indexLifecycleRolloverAliasPropertyName?json_string}": "${indexName}",</#if>
            "index.number_of_shards":${numberOfShards},
            "index.number_of_replicas":${numberOfReplicas},
            "index.refresh_interval": "${refreshInterval}"
            <#if extendedSettingsTemplate??>,<#include "/${extendedSettingsTemplate}"></#if>
        },
        "mappings": {
            <#include "../../common/mapping/request-mapping.ftl">
        }
    }
}
