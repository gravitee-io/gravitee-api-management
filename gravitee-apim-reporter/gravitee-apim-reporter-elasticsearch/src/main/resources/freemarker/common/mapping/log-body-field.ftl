<#ftl output_format="JSON">
<#--
    Mapping of one captured request, response or message body.

    Bodies are the heaviest thing the reporter asks Elasticsearch to index: with a 256KB logging limit a
    single v4 log document carries up to four of them. The only query that ever reads these fields is the
    "search in payloads" prefix query_string, which needs nothing beyond the term postings, so positions
    and norms are left out.

    When index_body is off the inverted index is dropped entirely: the body stays in _source and the Logs UI
    still displays it, but payload search no longer matches it. Elasticsearch rejects analyzer, index_options
    and norms on an unindexed field, hence the two exclusive branches.

    The analyzer is passed in rather than fixed here because the template trees do not agree on one: es7x
    maps v4 bodies with the Elasticsearch default while every other body uses gravitee_body_analyzer.
-->
<#macro mapping analyzer="">
{
    "type": "text"<#if indexBody><#if analyzer?has_content>,
    "analyzer": "${analyzer?json_string}"</#if>,
    "index_options": "docs",
    "norms": false<#else>,
    "index": false</#if>
}
</#macro>
