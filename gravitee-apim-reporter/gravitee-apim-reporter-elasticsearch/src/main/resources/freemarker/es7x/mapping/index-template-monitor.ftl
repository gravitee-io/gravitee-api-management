<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}*"],
    "settings": {
        <#if indexLifecyclePolicyMonitor??>"${indexLifecyclePolicyPropertyName}": "${indexLifecyclePolicyMonitor}",</#if>
        <#if indexLifecyclePolicyMonitor??>"${indexLifecycleRolloverAliasPropertyName}": "${indexName}",</#if>
        "index.number_of_shards":${numberOfShards},
        "index.number_of_replicas":${numberOfReplicas},
        "index.refresh_interval": "${refreshInterval}"
        <#if extendedSettingsTemplate??>,<#include "/${extendedSettingsTemplate}"></#if>
    },
    "mappings": {
            "properties": {
                "os": {
                    "properties": {
                        "cpu": {
                            "properties": {
                                "load_average": {
                                    "properties": {
                                        "1m": {
                                            "type": "float"
                                        },
                                        "5m": {
                                            "type": "float"
                                        },
                                        "15m": {
                                            "type": "float"
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                "gateway": {
                    "type": "keyword"
                },
                "hostname": {
                    "type": "keyword"
                }
            }
    }
}
