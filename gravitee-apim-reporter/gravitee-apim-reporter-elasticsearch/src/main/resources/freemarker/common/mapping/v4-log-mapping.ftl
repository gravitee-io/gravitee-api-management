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
                "api-product-id": {
                    "type": "keyword"
                },
                "request-id": {
                    "type": "keyword"
                },
                "trace-id": {
                    "type": "keyword"
                },
                "span-id": {
                    "type": "keyword"
                },
                "client-identifier": {
                    "type": "keyword"
                },
                "request-ended": {
                    "type": "boolean"
                },
                "connection-phase": {
                    "type": "keyword"
                },
                "entrypoint-request": {
                    "type": "object",
                    "properties": {
                        "body": <@bodyField.mapping analyzer="gravitee_body_analyzer"/>,
                        "headers":  {
                           "enabled":  false,
                           "type": "object"
                       }
                    }
                },
                "entrypoint-response": {
                    "type": "object",
                    "properties": {
                        "body": <@bodyField.mapping analyzer="gravitee_body_analyzer"/>,
                        "headers":  {
                           "enabled":  false,
                           "type": "object"
                       }
                    }
                },
                "endpoint-request": {
                    "type": "object",
                    "properties": {
                        "body": <@bodyField.mapping analyzer="gravitee_body_analyzer"/>,
                        "headers":  {
                           "enabled":  false,
                           "type": "object"
                       }
                    }
                },
                "endpoint-response": {
                    "type": "object",
                    "properties": {
                        "body": <@bodyField.mapping analyzer="gravitee_body_analyzer"/>,
                        "headers": {
                            "enabled":  false,
                            "type": "object"
                        }
                    }
                }
            }
