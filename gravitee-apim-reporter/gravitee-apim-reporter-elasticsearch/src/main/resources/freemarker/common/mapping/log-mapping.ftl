            "properties": {
                "@timestamp": {
                    "type": "date"
                },
                "api": {
                    "type": "keyword"
                },
                "api-name": {
                    "type": "keyword"
                },
                "client-request": {
                    "type": "object",
                    "properties": {
                        "body": <@bodyField.mapping analyzer="gravitee_body_analyzer"/>,
                        "headers":  {
                           "enabled":  false,
                           "type": "object"
                       }
                    }
                },
                "client-response": {
                    "type": "object",
                    "properties": {
                        "body": <@bodyField.mapping analyzer="gravitee_body_analyzer"/>,
                        "headers":  {
                           "enabled":  false,
                           "type": "object"
                       }
                    }
                },
                "proxy-request": {
                    "type": "object",
                    "properties": {
                        "body": <@bodyField.mapping analyzer="gravitee_body_analyzer"/>,
                        "headers":  {
                           "enabled":  false,
                           "type": "object"
                       }
                    }
                },
                "proxy-response": {
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
