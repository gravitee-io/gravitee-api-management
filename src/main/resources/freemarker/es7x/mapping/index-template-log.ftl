<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}*"],
    "settings": {
        <#if indexLifecyclePolicyLog??>"${indexLifecyclePolicyPropertyName}": "${indexLifecyclePolicyLog}",</#if>
        <#if indexLifecyclePolicyLog??>"index.lifecycle.rollover_alias": "${indexName}",</#if>
        "index.number_of_shards":${numberOfShards},
        "index.number_of_replicas":${numberOfReplicas},
        "index.refresh_interval": "${refreshInterval}"
        <#if !(extendedSettingsTemplate.analysis)??>,
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
        <#if extendedSettingsTemplate??>,<#include "/${extendedSettingsTemplate}"></#if>
    },
    "mappings": {
            "properties": {
                "@timestamp": {
                    "type": "date"
                },
                "api": {
                    "type": "keyword"
                },
                "api-name": {
                    "type": "keyword"
                },
                "client-request": {
                    "type": "object",
                    "properties": {
                        "body":{
                            "type": "text",
                            "analyzer": "gravitee_body_analyzer"
                        },
                        "headers":  {
                           "enabled":  false,
                           "type": "object"
                       }
                    }
                },
                "client-response": {
                    "type": "object",
                    "properties": {
                        "body":{
                            "type": "text",
                            "analyzer": "gravitee_body_analyzer"
                        },
                        "headers":  {
                           "enabled":  false,
                           "type": "object"
                       }
                    }
                },
                "proxy-request": {
                    "type": "object",
                    "properties": {
                        "body":{
                            "type": "text",
                            "analyzer": "gravitee_body_analyzer"
                        },
                        "headers":  {
                           "enabled":  false,
                           "type": "object"
                       }
                    }
                },
                "proxy-response": {
                    "type": "object",
                    "properties": {
                        "body":{
                            "type": "text",
                            "analyzer": "gravitee_body_analyzer"
                        },
                        "headers": {
                            "enabled":  false,
                            "type": "object"
                        }
                    }
                }
            }
    }
}
