<#ftl output_format="JSON">
{
    "template": "${indexName}-*",
    "settings": {
        "index.number_of_shards":${numberOfShards},
        "index.number_of_replicas":${numberOfReplicas},
        "index.refresh_interval": "${refreshInterval}"
    },
    "mappings": {
        "monitor": {
            "properties": {
                "gateway": {
                    "type": "keyword"
                },
                "hostname": {
                    "type": "keyword"
                }
            }
        }
    }
}