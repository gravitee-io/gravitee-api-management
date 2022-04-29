<#ftl output_format="JSON">
{
    "size": 0,
    "query": {
        "bool": {
            "filter": [
                {
                    "term": {
                        "api": "${query.root().id()}"
                    }
                }
            ]
        }
    },
    "aggregations": {
        "terms": {
            "aggregations": {
                "ranges": {
                    "aggregations": {
                        "results": {
                            "avg": {
                                "field": "response-time"
                            }
                        }
                    },
                    "date_range": {
                        "field": "@timestamp",
                        "keyed": false,
                        "ranges": [
                            {
                                "from": "now-1m",
                                "key": "1m"
                            },
                            {
                                "from": "now-1h",
                                "key": "1h"
                            },
                            {
                                "from": "now-1d",
                                "key": "1d"
                            },
                            {
                                "from": "now-1w",
                                "key": "1w"
                            },
                            {
                                "from": "now-1M",
                                "key": "1M"
                            }
                        ]
                    }
                }
            },
            "terms": {
                "field" : "${query.field()?lower_case}",
                "order": [
                    {
                        "_count": "desc"
                    },
                    {
                        "_term": "asc"
                    }
                ],
                "size": 100
            }
        }
    }
}