{ "index" : { "_index" : "${index}", "_type" : "${documentType}" } }
<@compress single_line=true>
{
  "os":{
    "cpu":{
      "percent":${percent},
      "load_average":{
        <#if load_average_1m??>"1m":${load_average_1m}</#if>
        <#if load_average_5m??>,"5m":${load_average_5m}</#if>
        <#if load_average_15m??>,"15m":${load_average_15m}</#if>
      }
    },
    "mem":{
      "total_in_bytes":${mem_total_in_bytes},
      "free_in_bytes":${mem_free_in_bytes},
      "used_in_bytes":${mem_used_in_bytes},
      "free_percent":${mem_free_percent},
      "used_percent":${mem_used_percent}
    }
  },
  "process":{
    "timestamp":${process_timestamp},
    "open_file_descriptors":${open_file_descriptors},
    "max_file_descriptors":${max_file_descriptors}
  },
  "jvm":{
    "timestamp":${jvm_timestamp},
    "uptime_in_millis":${uptime_in_millis},
    "mem":{
      "heap_used_in_bytes":${heap_used_in_bytes},
      "heap_used_percent":${heap_used_percent},
      "heap_committed_in_bytes":${heap_committed_in_bytes},
      "heap_max_in_bytes":${heap_max_in_bytes},
      "non_heap_used_in_bytes":${non_heap_used_in_bytes},
      "non_heap_committed_in_bytes":${non_heap_committed_in_bytes},
      "pools":{
<#list pools as pool>
      "${pool.getName()}":{
          "used_in_bytes":${pool.getUsed()},
          "max_in_bytes":${pool.getMax()},
          "peak_used_in_bytes":${pool.getPeakUsed()},
          "peak_max_in_bytes":${pool.getPeakMax()}
        }
  <#sep>,</#sep>
</#list>
      }
    },
    "threads":{
      "count":${count},
      "peak_count":${peak_count}
    },
    "gc":{
      "collectors":{

<#list collectors as collector>
        "${collector.getName()}":{
          "collection_count":${collector.getCollectionCount()},
          "collection_time_in_millis":${collector.getCollectionTime()}
        }
  <#sep>,</#sep>
</#list>

      }
    }
  },
  "gateway":"${gateway}",
  "hostname":"${hostname}",
  "@timestamp":"${@timestamp}"
}</@compress>
