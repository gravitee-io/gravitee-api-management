<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}*"],
    "template": {
        "settings": {
            <#if indexLifecyclePolicyRequest??>"index.plugins.index_state_management.policy_id": "${indexLifecyclePolicyRequest}",</#if>
            <#if indexLifecyclePolicyRequest??>"index.plugins.index_state_management.rollover_alias": "${indexName}",</#if>
            "index.number_of_shards":${numberOfShards},
            "index.number_of_replicas":${numberOfReplicas},
            "index.refresh_interval": "${refreshInterval}"
            <#if extendedSettingsTemplate??>,<#include "/${extendedSettingsTemplate}"></#if>
        },
        "mappings": {
            "properties": {
                "@timestamp": {
                    "type": "date"
                },
                "org-id": {
                    "type": "keyword"
                },
                "env-id": {
                    "type": "keyword"
                },
                "api": {
                    "type": "keyword"
                },
                "api-name": {
                    "type": "keyword"
                },
                "api-response-time": {
                    "type": "long"
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
                    "type": "text"
                },
                "method": {
                    "type": "short"
                },
                "plan": {
                    "type": "keyword"
                },
                "proxy-latency": {
                    "type": "integer"
                },
                "remote-address": {
                    "type": "ip"
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
                    "type": "long",
                    "index": false
                },
                "response-content-length": {
                    "type": "long",
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
                "subscription": {
                    "type": "keyword"
                },
                "zone": {
                    "type": "keyword"
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
                },
                {
                    "additional_int_metrics": {
                        "path_match": "additional-metrics.int_*",
                        "mapping": {
                            "type": "integer"
                        }
                    }
                },
                {
                    "additional_string_metrics": {
                        "path_match": "additional-metrics.string_*",
                        "mapping": {
                            "type": "text"
                        }
                    }
                },
                {
                    "additional_json_metrics": {
                        "path_match": "additional-metrics.json_*",
                        "mapping": {
                            "type": "text"
                        }
                    }
                }
            ]
        }
    }
}
