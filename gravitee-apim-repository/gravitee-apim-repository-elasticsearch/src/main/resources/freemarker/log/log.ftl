<#ftl output_format="JSON">
{
  "from": ${(query.page() - 1) * query.size()},
  "size": ${query.size()},
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
        },
        </#if>
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
      ]
    }
  }
  <#if query.sort()?has_content>
  ,"sort": [
    {
      "${query.sort().getField()}": {
        "order": "${query.sort().getOrder()?lower_case}"
      }
    }
  ]
  </#if>
}