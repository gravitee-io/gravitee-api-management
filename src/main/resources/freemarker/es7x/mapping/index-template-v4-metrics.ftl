<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}*"],
    "settings": {
        <#if indexLifecyclePolicyRequest??>"${indexLifecyclePolicyPropertyName}": "${indexLifecyclePolicyRequest}",</#if>
        <#if indexLifecyclePolicyRequest??>"${indexLifecycleRolloverAliasPropertyName}": "${indexName}",</#if>
        "index.number_of_shards":${numberOfShards},
        "index.number_of_replicas":${numberOfReplicas},
        "index.refresh_interval": "${refreshInterval}"
        <#if extendedSettingsTemplate??>,<#include "/${extendedSettingsTemplate}"></#if>
    },
    "mappings": {
        "properties": {
            "gateway": {
                "type": "keyword"
            },
            "@timestamp": {
                "type": "date"
            },
            "transaction-id": {
                "type": "keyword"
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
            "plan-id": {
                "type": "keyword"
            },
            "application-id": {
                "type": "keyword"
            },
            "subscription-id": {
                "type": "keyword"
            },
            "client-identifier": {
                "type": "keyword"
            },
            "tenant": {
                "type": "keyword"
            },
            "zone": {
                "type": "keyword"
            },
            "http-method": {
                "type": "short"
            },
            "local-address": {
                "type": "keyword",
                "index": false
            },
            "remote-address": {
                "type": "ip"
            },
            "host": {
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
            "user-agent": {
                "type": "keyword"
            },
            "user_agent": {
                "properties": {
                    "device": {
                        "properties": {
                            "name": {
                                "type": "keyword"
                            }
                        }
                    },
                    "name": {
                        "type": "keyword",
                        "index": true
                    },
                    "original": {
                        "type": "text"
                    },
                    "os": {
                        "properties": {
                            "full": {
                                "type": "text"
                            },
                            "name": {
                                "type": "keyword",
                                "index": true
                            },
                            "version": {
                                "type": "keyword",
                                "index": true
                            }
                        }
                    },
                    "os_name": {
                        "type": "keyword",
                        "index": true
                    },
                    "version": {
                        "type": "text"
                    }
                }
            },
            "request-content-length": {
                "type": "long",
                "index": false
            },
            "request-ended": {
                "type": "boolean"
            },
            "entrypoint-id": {
                "type": "keyword"
            },
            "endpoint": {
                "type": "keyword"
            },
            "endpoint-response-time-ms": {
                "type": "long"
            },
            "status": {
                "type": "short"
            },
            "response-content-length": {
                "type": "long",
                "index": false
            },
            "gateway-latency-ms": {
                "type": "integer"
            },
            "gateway-response-time-ms": {
                "type": "integer"
            },
            "ai-input-token": {
                "type": "integer"
            },
            "ai-output-token": {
                "type": "integer"
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
            "user": {
                "type": "keyword"
            },
            "security-type": {
                "type": "keyword",
                "index": true
            },
            "security-token": {
                "type": "keyword",
                "index": true
            },
            "error-message": {
                "type": "text"
            },
            "error-key": {
                "type": "keyword",
                "index": true
            },
            "error-component-type": {
                "type": "keyword",
                "index": true
            },
            "error-component-name": {
                "type": "keyword",
                "index": true
            },
            "warnings": {
                "type": "nested",
                "properties": {
                    "key": {
                        "type": "keyword",
                        "index": true
                    },
                    "message": {
                        "type": "text"
                    },
                    "component-type": {
                        "type": "keyword",
                        "index": true
                    },
                    "component-name": {
                        "type": "keyword",
                        "index": true
                    }
                }
            }
            <#if extendedRequestMappingTemplate??>,<#include "/${extendedRequestMappingTemplate}"></#if>
        },
        "dynamic_templates": [
            {
                "strings_as_keywords": {
                    "path_match": "custom.*",
                    "match_mapping_type": "string",
                    "mapping": {
                        "type": "keyword"
                    }
                }
            },
            {
                "additional_long_metrics": {
                    "path_match": "additional-metrics.long_*",
                    "match_mapping_type": "long",
                    "mapping": {
                        "type": "long"
                    }
                }
            },
            {
                "additional_keyword_metrics": {
                    "path_match": "additional-metrics.keyword_*",
                    "match_mapping_type": "string",
                    "mapping": {
                        "type": "keyword"
                    }
                }
            },
            {
                "additional_boolean_metrics": {
                    "path_match": "additional-metrics.bool_*",
                    "mapping": {
                        "type": "boolean"
                    }
                }
            },
            {
                "additional_double_metrics": {
                    "path_match": "additional-metrics.double_*",
                    "mapping": {
                        "type": "double"
                    }
                }
            }
        ]
    }
}
