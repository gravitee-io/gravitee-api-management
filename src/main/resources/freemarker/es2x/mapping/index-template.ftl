<#ftl output_format="JSON">
{
   "template":"${indexName}-*",
   "settings":{
      "index.number_of_shards":${numberOfShards},
      "index.number_of_replicas":${numberOfReplicas},
      "index.refresh_interval": "${refreshInterval}"
   },
   "mappings": {
      "monitor": {
         "properties": {
            "@timestamp": {
               "type": "date",
               "format": "strict_date_optional_time||epoch_millis"
            },
            "gateway": {
               "type": "string",
               "index": "not_analyzed"
            },
            "hostname": {
               "type": "string",
               "index": "not_analyzed"
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
      },
      "request": {
         "properties": {
            "@timestamp": {
               "type": "date",
               "format": "strict_date_optional_time||epoch_millis"
            },
            "api": {
               "type": "string",
               "index": "not_analyzed"
            },
            "api-key": {
               "type": "string",
               "index": "not_analyzed"
            },
            "api-response-time": {
               "type": "integer"
            },
            "application": {
               "type": "string",
               "index": "not_analyzed"
            },
            "client-request-headers": {
               "type": "object",
               "enabled": false
            },
            "client-response-headers": {
               "type": "object",
               "enabled": false
            },
            "endpoint": {
               "type": "string",
               "index": "not_analyzed"
            },
            "gateway": {
               "type": "string",
               "index": "not_analyzed"
            },
            "hostname": {
               "type": "string",
               "index": "not_analyzed"
            },
            "local-address": {
               "type": "string",
               "index": "not_analyzed"
            },
            "method": {
               "type": "string",
               "index": "not_analyzed"
            },
            "plan": {
               "type": "string",
               "index": "not_analyzed"
            },
            "proxy-latency": {
               "type": "integer"
            },
            "proxy-request-headers": {
               "type": "object",
               "enabled": false
            },
            "proxy-response-headers": {
               "type": "object",
               "enabled": false
            },
            "remote-address": {
               "type": "string",
               "index": "not_analyzed"
            },
            "request-content-length": {
               "type": "integer"
            },
            "response-content-length": {
               "type": "integer"
            },
            "response-time": {
               "type": "integer"
            },
            "status": {
               "type": "short"
            },
            "subscription": {
               "type": "string",
               "index": "not_analyzed"
            },
            "tenant": {
               "type": "string",
               "index": "not_analyzed"
            },
            "transaction": {
               "type": "string",
               "index": "not_analyzed"
            },
            "uri": {
               "type": "string",
               "index": "not_analyzed"
            },
            "user": {
               "type": "string",
               "index": "not_analyzed"
            },
            "path": {
               "type": "string",
               "index": "not_analyzed"
            },
            "mapped-path": {
               "type": "string",
               "index": "not_analyzed"
            },
            "host": {
               "index": "not_analyzed",
               "type": "string"
            },
            "user-agent": {
               "index": "not_analyzed",
               "type": "string"
            }
         }
      },
      "health": {
         "properties": {
            "@timestamp": {
               "type": "date",
               "format": "strict_date_optional_time||epoch_millis"
            },
            "api": {
               "type": "string",
               "index": "not_analyzed"
            },
            "gateway": {
               "type": "string",
               "index": "not_analyzed"
            },
            "hostname": {
               "type": "string",
               "index": "not_analyzed"
            },
            "message": {
               "type": "string",
               "index": "not_analyzed"
            },
            "method": {
               "type": "string",
               "index": "not_analyzed"
            },
            "state": {
               "type": "integer"
            },
            "status": {
               "type": "short"
            },
            "success": {
               "type": "boolean"
            },
            "url": {
               "type": "string",
               "index": "not_analyzed"
            }
         }
      }
   }
}