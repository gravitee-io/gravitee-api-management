<#ftl output_format="JSON">
<#-- @ftlvariable name="metrics" type="io.gravitee.reporter.api.v4.metric.event.BaseEventMetrics" -->
<#macro baseEventMetrics metrics @timestamp index="" date="">
<#if index?has_content>
    { "create": { "_index": "${index}" } }
</#if>
<#--noinspection FtlReferencesInspection-->
<@compress single_line=true>
{
    "@timestamp": "${@timestamp}"
    <#if !index?has_content>
    ,"type": "event-metrics"
    ,"date": "${date}"
    </#if>
    ,"gw-id": "${metrics.getGatewayId()}"
    ,"org-id": "${metrics.getOrganizationId()}"
    ,"env-id": "${metrics.getEnvironmentId()}"
    ,"api-id": "${metrics.getApiId()}"
    <#if metrics.getPlanId()??>
    ,"plan-id": "${metrics.getPlanId()}"
    </#if>
    <#if metrics.getApplicationId()??>
    ,"app-id": "${metrics.getApplicationId()}"
    </#if>
    ,"doc-type": "${metrics.getDocumentType()}"
    <#nested/>
}</@compress>
</#macro>
