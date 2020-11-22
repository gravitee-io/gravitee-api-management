<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}-*"],
    "settings": {
        <#if indexLifecyclePolicyMonitor??>"${indexLifecyclePolicyPropertyName}": "${indexLifecyclePolicyMonitor}",</#if>
        "index.number_of_shards":${numberOfShards},
        "index.number_of_replicas":${numberOfReplicas},
        "index.refresh_interval": "${refreshInterval}",
        "index.search.slowlog.threshold.query.warn": "${slowLogThresholdQueryWarn}",
        "index.search.slowlog.threshold.query.info": "${slowLogThresholdQueryInfo}",
        "index.search.slowlog.threshold.query.debug": "${slowLogThresholdQueryDebug}",
        "index.search.slowlog.threshold.query.trace": "${slowLogThresholdQueryTrace}",
        "index.search.slowlog.threshold.fetch.warn": "${slowLogThresholdFetchWarn}",
        "index.search.slowlog.threshold.fetch.info": "${slowLogThresholdFetchInfo}",
        "index.search.slowlog.threshold.fetch.debug": "${slowLogThresholdFetchDebug}",
        "index.search.slowlog.threshold.fetch.trace": "${slowLogThresholdFetchTrace}",
        "index.search.slowlog.level": "${slowLogLevel}"
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
