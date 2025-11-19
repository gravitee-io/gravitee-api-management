<#ftl output_format="JSON">
{
  "from": ${(query.page() - 1) * query.size()},
  "size": ${query.size()},
  "query": {
    "bool": {
      "filter": [
        <#if query.terms()?has_content>
        {
          "bool": {
          "should": [
          <#list query.terms() as terms>
            {
              "terms": {
                "${terms.field()}": ["${terms.values()?join("\",\"")}"]
              }
            }
              <#sep>,</#sep>
          </#list>
          ]
          }
        },
        </#if>
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
              "gte": ${query.timeRange().range().from()},
              "lte": ${query.timeRange().range().to()}
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
