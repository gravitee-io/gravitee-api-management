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
                "plan": {
                    "type": "keyword"
                },
                "proxy-latency": {
                    "type": "integer",
                    "index": false
                },
                "remote-address": {
                    "type": "ip",
                    "index": false
                },
                "geoip" : {
                    "properties": {
                        "continent_name": {
                            "type": "keyword",
                            "index": true
                        },
                        "country_iso_code": {
                            "type": "keyword",
                            "index": true
                        },
                        "region_name": {
                            "type": "keyword",
                            "index": true
                        },
                        "city_name": {
                            "type": "keyword",
                            "index": true
                        },
                        "location": {
                            "type": "geo_point"
                        }
                    }
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
                    "type": "keyword"
                },
                "path": {
                    "type": "keyword"
                },
                "mapped-path": {
                    "type": "keyword"
                },
                "host": {
                    "type": "keyword"
                },
                "user-agent": {
                    "type": "keyword"
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
        "index.refresh_interval": "${refreshInterval}"
    },
    "template": "${indexName}-*"
}