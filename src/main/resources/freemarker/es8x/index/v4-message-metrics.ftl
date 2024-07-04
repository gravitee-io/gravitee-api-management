<#-- @ftlvariable name="@timestamp" type="java.lang.String" -->
<#-- @ftlvariable name="index" type="java.lang.String" -->
<#-- @ftlvariable name="pipeline" type="java.lang.String" -->
<#-- @ftlvariable name="gateway" type="java.lang.String" -->
<#-- @ftlvariable name="metrics" type="io.gravitee.reporter.api.v4.metric.MessageMetrics" -->
<#-- @ftlvariable name="contentLength" type="java.lang.Long" -->
<#-- @ftlvariable name="count" type="java.lang.Long" -->
<#-- @ftlvariable name="errorCount" type="java.lang.Long" -->
<#-- @ftlvariable name="gatewayLatencyMs" type="java.lang.Long" -->
<#if index??>
{ "index" : { "_index" : "${index}", "_id" : "${metrics.getCorrelationId()}-${metrics.getConnectorType().getLabel()}"} }
</#if>
<@compress single_line=true>
{
  "gateway":"${gateway}"
  <#if !index??>
  ,"_id" : "${metrics.getCorrelationId()}-${metrics.getConnectorType().getLabel()}"
  ,"type": "v4-message-metrics"
  ,"date" : "${date}"
  </#if>
  ,"@timestamp":"${@timestamp}"
  ,"request-id":"${metrics.getRequestId()}"
  ,"api-id":"${metrics.getApiId()}"
  ,"api-name":"${metrics.getApiName()?j_string}"
  ,"client-identifier":"${metrics.getClientIdentifier()}"
  ,"correlation-id":"${metrics.getCorrelationId()}"
  <#if metrics.getParentCorrelationId()??>
  ,"parent-correlation-id":"${metrics.getParentCorrelationId()}"
  </#if>
  ,"operation":"${metrics.getOperation().getLabel()}"
  ,"connector-type":"${metrics.getConnectorType().getLabel()}"
  ,"connector-id":"${metrics.getConnectorId()}"
  <#if contentLength??>
    ,"content-length":${contentLength}
  </#if>
  <#if count??>
  ,"count":${count}
  </#if>
  <#if errorCount??>
  ,"error-count":"${errorCount}"
  </#if>
  <#if countIncrement??>
    ,"count-increment":${countIncrement}
  </#if>
  <#if errorCountIncrement??>
    ,"error-count-increment":"${errorCountIncrement}"
  </#if>
  <#if metrics.isError()>
  ,"error":"${metrics.isError()?c}"
  </#if>
  <#if gatewayLatencyMs??>
  ,"gateway-latency-ms":${gatewayLatencyMs}
  </#if>
  <#if metrics.getCustomMetrics()??>
  ,"custom": {
  <#list metrics.getCustomMetrics() as propKey, propValue>
    "${propKey}":"${propValue}"<#sep>,
  </#list>
  }
  </#if>
}</@compress>
