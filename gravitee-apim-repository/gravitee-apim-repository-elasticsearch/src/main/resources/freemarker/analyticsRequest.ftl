<#ftl output_format="JSON">
{
   "size":0,
   "query":{
      "bool":{
         "filter":[
            {
               "term":{
                  "api":"${api}"
               }
            },
            {
               "range":{
                  "@timestamp":{
                     "gte":${from},
                     "lte":${to}
                  }
               }
            }
         ]
      }
   },
   "aggregations":{
      "by_date":{
         "date_histogram":{
            "field":"@timestamp",
<#if useFixedInterval??>
            "fixed_interval": "${interval}",
<#else>
            "interval": "${interval}",
</#if>
            "min_doc_count":0,
            "extended_bounds":{
               "min":${from},
               "max":${to}
            }
         },
         "aggregations":{
            "by_application":{
               "terms":{
                  "field":"application"
               }
            }
         }
      }
   }
}