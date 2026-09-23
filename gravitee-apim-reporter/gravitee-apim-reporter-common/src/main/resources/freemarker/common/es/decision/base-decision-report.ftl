<#ftl output_format="JSON">
<#macro baseDecisionReport report @timestamp index="" date="">
<#if index?has_content>
    { "create": { "_index": "${index}" } }
</#if>
<@compress single_line=true>
{
    "@timestamp": "${@timestamp}"
    <#if !index?has_content>
    ,"type": "decisions"
    ,"date": "${date}"
    </#if>
    ,"gw-id": "${report.getGatewayId()?json_string}"
    ,"org-id": "${report.getOrganizationId()?json_string}"
    ,"env-id": "${report.getEnvironmentId()?json_string}"
    ,"api-id": "${report.getApiId()?json_string}"
    <#if report.getPlanId()??>
    ,"plan-id": "${report.getPlanId()?json_string}"
    </#if>
    <#if report.getApplicationId()??>
    ,"app-id": "${report.getApplicationId()?json_string}"
    </#if>
    <#nested/>
}</@compress>
</#macro>
