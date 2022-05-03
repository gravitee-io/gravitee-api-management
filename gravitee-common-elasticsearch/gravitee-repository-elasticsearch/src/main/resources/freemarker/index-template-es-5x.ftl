<#ftl output_format="JSON">
{
    "mappings": {
        "request": {
            "properties": {
                "@timestamp": {
                    "type": "date"
                },
                "api": {
                    "type": "keyword"
                },
                "api-key": {
                    "type": "keyword",
                    "index": false
                },
                "api-response-time": {
                    "type": "integer"
                },
                "application": {
                    "type": "keyword"
                },
                "endpoint": {
                    "type": "keyword"
                },
                "gateway": {
                    "type": "keyword"
                },
                "local-address": {
                    "type": "keyword",
                    "index": false
                },
                "message": {
                    "type": "keyword",
                    "index": false
                },
                "method": {
                    "type": "short"
                },
                "path": {
                    "type": "keyword"
                },
                "plan": {
                    "type": "keyword"
                },
                "proxy-latency": {
                    "type": "integer",
                    "index": false
                },
                "remote-address": {
                    "type": "keyword"
                },
                "request-content-length": {
                    "type": "integer",
                    "index": false
                },
                "response-content-length": {
                    "type": "integer",
                    "index": false
                },
                "response-time": {
                    "type": "integer"
                },
                "status": {
                    "type": "short"
                },
                "tenant": {
                    "type": "keyword"
                },
                "transaction": {
                    "type": "keyword"
                },
                "uri": {
                    "type": "keyword",
                    "index": false
                }
            }
        },
        "monitor": {
            "properties": {
                "gateway": {
                    "type": "keyword"
                },
                "hostname": {
                    "type": "keyword"
                }
            }
        },
        "log": {
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
                            "type": "keyword"
                        }
                    }
                },
                "client-response": {
                    "type": "object",
                    "properties": {
                        "body":{
                            "type": "keyword"
                        }
                    }
                },
                "proxy-request": {
                    "type": "object",
                    "properties": {
                        "body":{
                            "type": "keyword"
                        }
                    }
                },
                "proxy-response": {
                    "type": "object",
                    "properties": {
                        "body":{
                            "type": "keyword"
                        }
                    }
                }
            }
        },
        "health": {
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
    },
    "settings": {
        "index.number_of_shards":${numberOfShards},
        "index.number_of_replicas":${numberOfReplicas},
        "refresh_interval": "1s"
    },
    "template": "${indexName}-*"
}