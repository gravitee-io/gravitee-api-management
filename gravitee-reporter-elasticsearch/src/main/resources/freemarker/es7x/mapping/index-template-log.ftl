<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}-*"],
    "settings": {
        <#if indexLifecyclePolicyLog??>"${indexLifecyclePolicyPropertyName}": "${indexLifecyclePolicyLog}",</#if>
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
                "@timestamp": {
                    "type": "date"
                },
                "api": {
                    "type": "keyword"
                },
                "client-request": {
                    "type": "object",
                    "properties": {
                        "body":{
                            "type": "text"
                        }
                    }
                },
                "client-response": {
                    "type": "object",
                    "properties": {
                        "body":{
                            "type": "text"
                        }
                    }
                },
                "proxy-request": {
                    "type": "object",
                    "properties": {
                        "body":{
                            "type": "text"
                        }
                    }
                },
                "proxy-response": {
                    "type": "object",
                    "properties": {
                        "body":{
                            "type": "text"
                        }
                    }
                }
            }
    }
}
