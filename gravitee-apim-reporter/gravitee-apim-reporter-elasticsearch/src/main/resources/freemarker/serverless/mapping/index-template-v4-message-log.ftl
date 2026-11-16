<#ftl output_format="JSON">
<#import "../../common/mapping/log-body-field.ftl" as bodyField>
{
    "index_patterns": ["${indexName}*"],
    "data_stream": {},
    "template": {
        "settings": {
            "analysis": {
                "analyzer": {
                    "gravitee_body_analyzer": {
                        "type": "custom",
                        "tokenizer": "whitespace",
                        "filter": [
                            "lowercase"
                        ]
                    }
                }
            }
        },
        "mappings": {
            <#include "../../common/mapping/v4-message-log-mapping.ftl">
        }
    }
}
