<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}-*"],
    "settings": {
        "index.number_of_shards":${numberOfShards},
        "index.number_of_replicas":${numberOfReplicas},
        "index.refresh_interval": "${refreshInterval}"
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
                    "enabled": false
                },
                "client-response": {
                    "type": "object",
                    "enabled": false
                },
                "proxy-request": {
                    "type": "object",
                    "enabled": false
                },
                "proxy-response": {
                    "type": "object",
                    "enabled": false
                }
            }
    }
}