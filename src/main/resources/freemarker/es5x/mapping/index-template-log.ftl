<#ftl output_format="JSON">
{
    "template": "${indexName}-*",
    "settings": {
        "index.number_of_shards":${numberOfShards},
        "index.number_of_replicas":${numberOfReplicas},
        "index.refresh_interval": "${refreshInterval}"
    },
    "mappings": {
        "log": {
            "properties": {
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
}