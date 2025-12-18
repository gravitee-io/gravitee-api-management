<#ftl output_format="JSON">
{
  "from": ${(query.page() - 1) * query.size()},
  "size": ${query.size()},
  "query": {
    "bool": {
      "filter": [
        <#if query.terms()?has_content>
        {
          "bool": {
          "should": [
          <#list query.terms() as terms>
            {
              "terms": {
                "${terms.field()}": ["${terms.values()?join("\",\"")}"]
              }
            }
              <#sep>,</#sep>
          </#list>
          ]
          }
        },
        </#if>
        <#if responseTimeRanges?? && responseTimeRanges?has_content>
        {
          "bool": {
            "should": [
            <#list responseTimeRanges as range>
              {
                "range": {
                  "response-time": {
                    "gte": ${range.gte},
                    "lte": ${range.lte}
                  }
                }
              }
              <#sep>,</#sep>
            </#list>
            ],
            "minimum_should_match": 1
          }
        },
        </#if>
        <#if query.query()?has_content>
        <#-- Use cleaned query filter if ranges were extracted, otherwise use original -->
        <#if responseTimeRanges?? && responseTimeRanges?has_content>
          <#-- Ranges were extracted, use cleaned filter -->
          <#assign queryFilter = (cleanedQueryFilter?? && cleanedQueryFilter?has_content)?then(cleanedQueryFilter, "")>
        <#else>
          <#-- No ranges extracted, use original filter -->
          <#assign queryFilter = query.query().filter()>
        </#if>
        <#-- Only process if there's still content after removing ranges -->
        <#if queryFilter?has_content && queryFilter?trim?length gt 0>
        <#-- Remove leading spaces in quoted values: _id:" value" -> _id:"value" -->
        <#assign processedQuery = queryFilter?replace('_id:" ', '_id:"', 'r')>
        <#assign processedQuery = processedQuery?replace('transaction:" ', 'transaction:"', 'r')>
        {
          "query_string": {
            "query": "${processedQuery}",
            "default_operator": "AND",
            "lenient": true,
            "analyze_wildcard": true,
            "allow_leading_wildcard": true,
            "auto_generate_synonyms_phrase_query": false
          }
        },
        </#if>
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
              "from": ${query.timeRange().range().from()},
              "to": ${query.timeRange().range().to()},
              "include_lower": true,
              "include_upper": true
            }
          }
        }
      ]
    }
  }
  <#if query.sort()?has_content>
  ,"sort": [
    {
      "${query.sort().getField()}": {
        "order": "${query.sort().getOrder()?lower_case}"
      }
    }
  ]
  </#if>
}
