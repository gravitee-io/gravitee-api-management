<#ftl output_format="JSON">
{
    "template": "${indexName}-*",
    "settings": {
        "index.number_of_shards":${numberOfShards},
        "index.number_of_replicas":${numberOfReplicas},
        "index.refresh_interval": "${refreshInterval}"
    },
    "mappings": {
        "health": {
            "properties": {
                "@timestamp": {
                    "format": "strict_date_optional_time||epoch_millis",
                    "type": "date"
                },
                "api": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "gateway": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "hostname": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "message": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "method": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "state": {
                    "type": "integer"
                },
                "status": {
                    "type": "short"
                },
                "success": {
                    "type": "boolean"
                },
                "url": {
                    "index": "not_analyzed",
                    "type": "string"
                }
            }
        }
    }
}