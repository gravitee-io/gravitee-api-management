{
  "size": 0,
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
  },
  "aggregations": {

<#if query.groups()?has_content>
      "by_${query.field()}_range": {
        "range":{
          "field":"${query.field()}",
          "ranges":[
  <#list query.groups() as range>
            {
              "from":${range.from()},
    <#if query.field() == 'status'>
              "to":${range.to() + 1}
    <#else>
              "to":${range.to()}
    </#if>
            }
    <#sep>,</#sep>
  </#list>
          ]}
<#else>
      "by_${query.field()}": {
        "terms":{
          "field":"${query.field()}",
          "size": 1000
  <#if query.sort()?has_content>
          ,"order": {
            "${query.sort().getField()}":"${query.sort().getOrder()?lower_case}"
          }
        },
        "aggregations":{
      <#switch query.sort().getType().name()>
          <#case "AVG">
          "${query.sort().getField()}":{
            "avg":{
              "field":"${query.sort().getField()}"
            }
          }
          <#break>
          <#default>
              <#break>
      </#switch>
  </#if>
      }
</#if>
    }
  }
}
