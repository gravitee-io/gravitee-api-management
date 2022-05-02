<#ftl output_format="JSON">
{
  "size" : 0,
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
  "aggregations" : {
    "terms" : {
      "terms" : {
        "field" : "${query.field()?lower_case}",
        "size" : 100,
        "order" : [
          {
            "_count" : "desc"
          },
          {
            "_term" : "asc"
          }
        ]
      },
      "aggregations" : {
        "ranges" : {
          "date_range" : {
            "field" : "@timestamp",
            "ranges" : [
              {
                "key" : "1m",
                "from" : "now-1m"
              },
              {
                "key" : "1h",
                "from" : "now-1h"
              },
              {
                "key" : "1d",
                "from" : "now-1d"
              },
              {
                "key" : "1w",
                "from" : "now-1w"
              },
              {
                "key" : "1M",
                "from" : "now-1M"
              }
            ],
            "keyed" : false
          },
          "aggregations" : {
            "results" : {
              "terms" : {
                "field" : "available",
                "size" : 2,
                "order" : [
                  {
                    "_count" : "desc"
                  },
                  {
                    "_term" : "asc"
                  }
                ]
              }
            }
          }
        }
      }
    }
  }
}