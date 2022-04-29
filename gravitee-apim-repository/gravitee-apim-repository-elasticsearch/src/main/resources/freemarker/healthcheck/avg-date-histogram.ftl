<#ftl output_format="JSON">
{
  "size" : 0,
  "query" : {
    "bool" : {
      "filter" : [
        {
          "term" : {
            "api" : {
              "value" : "${query.root().id()}"
            }
          }
        },
        {
          "range": {
            "@timestamp": {
              "from": ${roundedFrom},
              "to": ${roundedTo}
            }
          }
        }
      ],
      "adjust_pure_negative" : true
    }
  },
  "aggregations" : {
    "by_date" : {
      "date_histogram" : {
        "field" : "@timestamp",
        "interval": "${query.timeRange().interval().toMillis()}ms",
        "order" : {
          "_key" : "asc"
        },
        "keyed" : false,
        "min_doc_count" : 0,
        "extended_bounds" : {
          "min" : ${roundedFrom},
          "max" : ${roundedTo}
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
          "field": "${aggregation.field()}",
          "size": 100
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
