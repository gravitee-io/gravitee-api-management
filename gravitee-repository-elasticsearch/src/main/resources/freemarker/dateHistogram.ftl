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
              "from": ${roundedFrom},
              "to": ${roundedTo},
              "include_lower": true,
              "include_upper": true
            }
          }
        }
      ]
    }
  },
  "aggregations": {
    "by_date": {
      "date_histogram": {
        "field": "@timestamp",
        "interval": "${query.timeRange().interval().toMillis()}ms",
        "min_doc_count": 0,
        "extended_bounds": {
          "min": ${roundedFrom},
          "max": ${roundedTo}
        }
      }
<#if query.aggregations()?has_content>
      ,
      "aggregations": {
  <#list query.aggregations() as aggregation>
    <#switch aggregation.type()>
      <#case "AVG">
      "avg_${aggregation.field()}": {
        "avg": {
          "field": "${aggregation.field()}"
        }
      }
        <#break>
      <#case "FIELD">
      "by_${aggregation.field()}": {
        "terms": {
          "field": "${aggregation.field()}"
        }
      }
        <#break>
      <#default>
        <#break>
    </#switch>
    <#sep>,</#sep>
  </#list>
      }
</#if>
    }
  }
}