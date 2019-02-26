<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}-*"],
    "settings": {
        "index.number_of_shards":${numberOfShards},
        "index.number_of_replicas":${numberOfReplicas},
        "index.refresh_interval": "${refreshInterval}"
    },
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
                        "continent_name":{
                            "type": "keyword",
                            "index": true
                        },
                        "country_iso_code":{
                            "type": "keyword",
                            "index": true
                        },
                        "region_name":{
                            "type": "keyword",
                            "index": true
                        },
                        "city_name":{
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
                },
                "user_agent": {
                    "properties": {
                        "name": {
                            "type": "keyword",
                            "index": true
                        },
                        "os_name": {
                            "type": "keyword",
                            "index": true
                        }
                    }
                }
            }
        }
    }
}