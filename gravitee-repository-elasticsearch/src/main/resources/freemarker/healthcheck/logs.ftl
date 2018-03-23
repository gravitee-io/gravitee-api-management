<#ftl output_format="JSON">
{
  "from": ${(query.page() - 1) * query.size()},
  "size": ${query.size()},
  "query" : {
    "bool" : {
      "filter" : [
        {
          "term" : {
            "api": "${query.root().id()}"
          }
        }
      ]
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