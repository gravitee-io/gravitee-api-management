{
    "settings": {
        "index.number_of_shards":${numberOfShards},
        "index.number_of_replicas":${numberOfReplicas},
        "index.refresh_interval": "${refreshInterval}"
    },
    "template": "${indexName}-*",
    "mappings": {
        "request": {
            "properties": {
                "@timestamp": {
                    "format": "strict_date_optional_time||epoch_millis",
                    "type": "date"
                },
                "api": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "api-key": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "api-response-time": {
                    "type": "integer"
                },
                "application": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "client-request-headers": {
                    "enabled": false,
                    "type": "object"
                },
                "client-response-headers": {
                    "enabled": false,
                    "type": "object"
                },
                "endpoint": {
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
                "local-address": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "method": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "plan": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "proxy-latency": {
                    "type": "integer"
                },
                "proxy-request-headers": {
                    "enabled": false,
                    "type": "object"
                },
                "proxy-response-headers": {
                    "enabled": false,
                    "type": "object"
                },
                "remote-address": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "request-content-length": {
                    "type": "integer"
                },
                "response-content-length": {
                    "type": "integer"
                },
                "response-time": {
                    "type": "integer"
                },
                "status": {
                    "type": "short"
                },
                "subscription": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "tenant": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "transaction": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "uri": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "user": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "path": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "mapped-path": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "host": {
                    "index": "not_analyzed",
                    "type": "string"
                },
                "user-agent": {
                    "index": "not_analyzed",
                    "type": "string"
                }
            }
        }
    }
}