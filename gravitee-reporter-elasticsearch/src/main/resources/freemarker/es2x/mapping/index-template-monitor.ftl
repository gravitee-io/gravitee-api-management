<#ftl output_format="JSON">
{
"settings": {
"index.number_of_shards":${numberOfShards},
"index.number_of_replicas":${numberOfReplicas},
"index.refresh_interval": "${refreshInterval}"
},
"template": "${indexName}-*",
"mappings": {
"monitor": {
"properties": {
"@timestamp": {
"format": "strict_date_optional_time||epoch_millis",
"type": "date"
},
"gateway": {
"index": "not_analyzed",
"type": "string"
},
"hostname": {
"index": "not_analyzed",
"type": "string"
},
"jvm": {
"properties": {
"gc": {
"properties": {
"collectors": {
"properties": {
"old": {
"properties": {
"collection_count": {
"type": "long"
},
"collection_time_in_millis": {
"type": "long"
}
}
},
"young": {
"properties": {
"collection_count": {
"type": "long"
},
"collection_time_in_millis": {
"type": "long"
}
}
}
}
}
}
},
"mem": {
"properties": {
"heap_committed_in_bytes": {
"type": "long"
},
"heap_max_in_bytes": {
"type": "long"
},
"heap_used_in_bytes": {
"type": "long"
},
"heap_used_percent": {
"type": "long"
},
"non_heap_committed_in_bytes": {
"type": "long"
},
"non_heap_used_in_bytes": {
"type": "long"
},
"pools": {
"properties": {
"old": {
"properties": {
"max_in_bytes": {
"type": "long"
},
"peak_max_in_bytes": {
"type": "long"
},
"peak_used_in_bytes": {
"type": "long"
},
"used_in_bytes": {
"type": "long"
}
}
},
"survivor": {
"properties": {
"max_in_bytes": {
"type": "long"
},
"peak_max_in_bytes": {
"type": "long"
},
"peak_used_in_bytes": {
"type": "long"
},
"used_in_bytes": {
"type": "long"
}
}
},
"young": {
"properties": {
"max_in_bytes": {
"type": "long"
},
"peak_max_in_bytes": {
"type": "long"
},
"peak_used_in_bytes": {
"type": "long"
},
"used_in_bytes": {
"type": "long"
}
}
}
}
}
}
},
"threads": {
"properties": {
"count": {
"type": "long"
},
"peak_count": {
"type": "long"
}
}
},
"timestamp": {
"type": "long"
},
"uptime_in_millis": {
"type": "long"
}
}
},
"os": {
"properties": {
"cpu": {
"properties": {
"load_average": {
"properties": {
"15m": {
"type": "double"
},
"1m": {
"type": "double"
},
"5m": {
"type": "double"
}
}
},
"percent": {
"type": "long"
}
}
},
"mem": {
"properties": {
"free_in_bytes": {
"type": "long"
},
"free_percent": {
"type": "long"
},
"total_in_bytes": {
"type": "long"
},
"used_in_bytes": {
"type": "long"
},
"used_percent": {
"type": "long"
}
}
}
}
},
"process": {
"properties": {
"max_file_descriptors": {
"type": "long"
},
"open_file_descriptors": {
"type": "long"
},
"timestamp": {
"type": "long"
}
}
}
}
}
}
}