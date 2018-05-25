<#ftl output_format="JSON">
{
  "from": ${(query.page() - 1) * query.size()},
  "size": ${query.size()},
  "query" : {
    "bool" : {
<#if query.transition()?has_content && query.transition() = true>
            "must":{
                "term":{
                   "transition": true
                }
            },
</#if>
        "filter" : [{
          "term" : {
            "api": "${query.root().id()}"
          }
        }]
    }
  },
  "sort" : [
    {
      "@timestamp" : {
        "order" : "desc"
      }
    }
  ]
}