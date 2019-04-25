<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}-*"],
    "settings": {
        "index.number_of_shards":${numberOfShards},
        "index.number_of_replicas":${numberOfReplicas},
        "index.refresh_interval": "${refreshInterval}"
    },
    "mappings": {
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
        }
    }
}