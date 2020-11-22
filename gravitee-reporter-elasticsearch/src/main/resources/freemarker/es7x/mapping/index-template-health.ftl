<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}-*"],
    "settings": {
        <#if indexLifecyclePolicyHealth??>"${indexLifecyclePolicyPropertyName}": "${indexLifecyclePolicyHealth}",</#if>
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
                "api": {
                    "type": "keyword"
                },
                "available": {
                    "type": "boolean",
                    "index": false
                },
                "endpoint": {
                    "type": "keyword"
                },
                "gateway": {
                    "type": "keyword"
                },
                "response-time": {
                    "type": "integer"
                },
                "state": {
                    "type": "integer",
                    "index": false
                },
                "transition": {
                    "type": "boolean"
                },
                "steps": {
                    "type": "object",
                    "enabled": false
                },
                "success": {
                    "type": "boolean",
                    "index": false
                }
            }
    }
}
