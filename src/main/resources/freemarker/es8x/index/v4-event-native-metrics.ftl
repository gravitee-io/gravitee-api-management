<#ftl output_format="JSON">
<#-- @ftlvariable name="@timestamp" type="java.lang.String" -->
<#-- @ftlvariable name="index" type="java.lang.String" -->
<#-- @ftlvariable name="metrics" type="io.gravitee.reporter.api.v4.metric.eventnative.EventNativeMetrics" -->
<#if index??>
    { "index": { "_index": "${index}" } }
</#if>
<#--noinspection FtlReferencesInspection-->
<@compress single_line=true>
    {
    <#if !index??>
        "type": "v4-event-native-metrics"
        ,"date": "${date}"
    </#if>
    ,"@timestamp": "${@timestamp}"
    ,"plan-id": "${metrics.getPlanId()}"
    ,"api-id": "${metrics.getApiId()}"
    ,"app-id": "${metrics.getApplicationId()}"
    ,"gw-id": "${metrics.getGatewayId()}"
    ,"env-id": "${metrics.getEnvironmentId()}"
    ,"org-id": "${metrics.getOrganizationId()}"
    <#if metrics.getTopic()??>
        ,"topic": "${metrics.getTopic()}"
    </#if>
    <#if metrics.getDownstreamPublishMessagesTotal()??>
        ,"downstream-publish-messages-total": ${metrics.getDownstreamPublishMessagesTotal()}
    </#if>
    <#if metrics.getDownstreamPublishMessageBytes()??>
        ,"downstream-publish-message-bytes": ${metrics.getDownstreamPublishMessageBytes()}
    </#if>
    <#if metrics.getUpstreamPublishMessagesTotal()??>
        ,"upstream-publish-messages-total": ${metrics.getUpstreamPublishMessagesTotal()}
    </#if>
    <#if metrics.getUpstreamPublishMessageBytes()??>
        ,"upstream-publish-message-bytes": ${metrics.getUpstreamPublishMessageBytes()}
    </#if>
    <#if metrics.getDownstreamSubscribeMessagesTotal()??>
        ,"downstream-subscribe-messages-total": ${metrics.getDownstreamSubscribeMessagesTotal()}
    </#if>
    <#if metrics.getDownstreamSubscribeMessageBytes()??>
        ,"downstream-subscribe-message-bytes": ${metrics.getDownstreamSubscribeMessageBytes()}
    </#if>
    <#if metrics.getUpstreamSubscribeMessagesTotal()??>
        ,"upstream-subscribe-messages-total": ${metrics.getUpstreamSubscribeMessagesTotal()}
    </#if>
    <#if metrics.getUpstreamSubscribeMessageBytes()??>
        ,"upstream-subscribe-message-bytes": ${metrics.getUpstreamSubscribeMessageBytes()}
    </#if>
    <#if metrics.getDownstreamActiveConnections()??>
        ,"downstream-active-connections": ${metrics.getDownstreamActiveConnections()}
    </#if>
    <#if metrics.getUpstreamActiveConnections()??>
        ,"upstream-active-connections": ${metrics.getUpstreamActiveConnections()}
    </#if>
    }
</@compress>