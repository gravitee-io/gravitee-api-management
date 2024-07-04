<#ftl output_format="JSON">
{
    "index_patterns": ["${indexName}*"],
    "template": {
        "settings": {
            <#if indexLifecyclePolicyRequest??>"${indexLifecyclePolicyPropertyName}": "${indexLifecyclePolicyRequest}",</#if>
            <#if indexLifecyclePolicyRequest??>"index.lifecycle.rollover_alias": "${indexName}",</#if>
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
                "subscription": {
                    "type": "keyword"
                },
                "zone": {
                    "type": "keyword"
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
                }
            ]
        }
    }
}
