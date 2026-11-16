            "properties": {
                "@timestamp": {
                    "type": "date"
                },
                "api-id": {
                    "type": "keyword"
                },
                "api-name": {
                    "type": "keyword"
                },
                "request-id": {
                    "type": "keyword"
                },
                "client-identifier": {
                    "type": "keyword"
                },
                "correlation-id": {
                    "type": "keyword"
                },
                "parent-correlation-id": {
                    "type": "keyword"
                },
                "operation": {
                    "type": "keyword"
                },
                "connector-type": {
                    "type": "keyword"
                },
                "connector-id": {
                    "type": "keyword"
                },
                "message": {
                    "type": "object",
                    "properties": {
                        "id": {
                            "type": "keyword"
                        },
                        "payload": <@bodyField.mapping analyzer="gravitee_body_analyzer"/>,
                        "headers":{
                            "enabled": false,
                            "type": "object"
                        },
                        "metadata":  {
                           "enabled": false,
                           "type": "object"
                        },
                        "error":  {
                            "type": "boolean"
                        }
                    }
                }
            }
