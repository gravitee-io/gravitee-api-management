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
    <#-- org/env/api are @NonNull by contract but connection-level Kafka accumulators report before the API (and org/env) is resolved, so guard them to avoid FreeMarker InvalidReferenceException -->
    <#if metrics.getOrganizationId()??>
    ,"org-id": "${metrics.getOrganizationId()}"
    </#if>
    <#if metrics.getEnvironmentId()??>
    ,"env-id": "${metrics.getEnvironmentId()}"
    </#if>
    <#if metrics.getApiId()??>
    ,"api-id": "${metrics.getApiId()}"
    </#if>
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
