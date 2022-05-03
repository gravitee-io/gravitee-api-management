{
  "query": {
    "bool": {
      "filter": [
<#if query.query()?has_content>
        {
          "query_string": {
            "query": "${query.query().filter()}"
          }
        },
</#if>
<#if query.root()?has_content>
        {
          "term": {
            "${query.root().field()}": "${query.root().id()}"
          }
        }<#if query.timeRange()?has_content>,</#if>
</#if>
<#if query.timeRange()?has_content>
        {
          "range": {
            "@timestamp": {
              "from": ${query.timeRange().range().from()},
              "to": ${query.timeRange().range().to()},
              "include_lower": true,
              "include_upper": true
            }
          }
        }
</#if>
      ]
    }
  }
}