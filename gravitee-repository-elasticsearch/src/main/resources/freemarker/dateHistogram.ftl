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
      <#case "FIELD">
      "by_${aggregation.field()}": {
        "terms": {
          "field": "${aggregation.field()}"
           <#if aggregation.size()?has_content>
           ,
            "size": "${aggregation.size()}"
           </#if>
        }
      }
        <#break>
      <#default>
      "${aggregation.type()?lower_case}_${aggregation.field()}": {
        "${aggregation.type()?lower_case}": {
        "field": "${aggregation.field()}"
        }
      }
        <#break>
    </#switch>
    <#sep>,</#sep>
  </#list>
      }
</#if>
    }
  }
}
