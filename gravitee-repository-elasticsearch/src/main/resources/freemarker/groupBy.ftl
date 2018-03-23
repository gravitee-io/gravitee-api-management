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
              "to":${range.to()}
            }
    <#sep>,</#sep>
  </#list>
          ]}
      }
<#else>
      "by_${query.field()}": {
        "terms":{
          "field":"${query.field()}",
          "size":20
  <#if query.sort()?has_content>
          ,"order":{
            "${query.sort().getType().name()?lower_case}_${query.sort().getField()}":"${query.sort().getOrder()?lower_case}"
          }
        },
        "aggregations":{
      <#switch query.sort().getType().name()>
          <#case "AVG">
          "avg_${query.sort().getField()}":{
            "avg":{
              "field":"${query.sort().getField()}"
            }
          }
          <#break>
          <#default>
              <#break>
      </#switch>
        }
  </#if>
      }
</#if>
    }
  }
}