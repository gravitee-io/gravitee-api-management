<#-- @ftlvariable name="@timestamp" type="java.lang.String" -->
<#-- @ftlvariable name="index" type="java.lang.String" -->
<#-- @ftlvariable name="pipeline" type="java.lang.String" -->
<#-- @ftlvariable name="gateway" type="java.lang.String" -->
<#-- @ftlvariable name="metrics" type="io.gravitee.reporter.api.v4.metric.Metrics" -->
<#-- @ftlvariable name="endpointResponseTimeMs" type="java.lang.Long" -->
<#-- @ftlvariable name="gatewayResponseTimeMs" type="java.lang.Long" -->
<#-- @ftlvariable name="gatewayLatencyMs" type="java.lang.Long" -->
<#-- @ftlvariable name="requestContentLength" type="java.lang.Long" -->
<#-- @ftlvariable name="responseContentLength" type="java.lang.Long" -->
<#if index??>
{ "index" : { "_index" : "${index}", "_id" : "${metrics.getRequestId()}"<#if pipeline??>, "pipeline" : "${pipeline}"</#if>} }
</#if>
<@compress single_line=true>
{
  "gateway":"${gateway}"
  <#if !index??>
  ,"_id" : "${metrics.getRequestId()}"
  ,"type": "v4-metrics"
  ,"date" : "${date}"
  </#if>
  ,"@timestamp":"${@timestamp}"
  ,"request-id":"${metrics.getRequestId()}"
  <#if metrics.getClientIdentifier()??>
  ,"client-identifier":"${metrics.getClientIdentifier()}"
  </#if>
  ,"transaction-id":"${metrics.getTransactionId()}"
  <#if metrics.getApiId()??>
  ,"api-id":"${metrics.getApiId()}"
  ,"api-name":"${metrics.getApiName()?j_string}"
  </#if>
  <#if metrics.getPlanId()??>
  ,"plan-id":"${metrics.getPlanId()}"
  </#if>
  <#if metrics.getApplicationId()??>
  ,"application-id":"${metrics.getApplicationId()}"
  </#if>
  <#if metrics.getSubscriptionId()??>
  ,"subscription-id":"${metrics.getSubscriptionId()}"
  </#if>
  <#if metrics.getTenant()??>
  ,"tenant":"${metrics.getTenant()}"
  </#if>
  <#if metrics.getZone()??>
  ,"zone":"${metrics.getZone()}"
  </#if>
  <#if metrics.getHttpMethod()??>
  ,"http-method":${metrics.getHttpMethod().code()?c}
  </#if>
  <#if metrics.getLocalAddress()??>
  ,"local-address":"${metrics.getLocalAddress()}"
  </#if>
  <#if metrics.getRemoteAddress()??>
  ,"remote-address":"${metrics.getRemoteAddress()}"
  </#if>
  <#if metrics.getHost()??>
  ,"host":"${metrics.getHost()}"
  </#if>
  <#if metrics.getUri()??>
  ,"uri":"${metrics.getUri()?j_string}"
  </#if>
  <#if metrics.getPathInfo()??>
  ,"path-info":"${metrics.getPathInfo()}"
  </#if>
  <#if metrics.getMappedPath()??>
  ,"mapped-path":"${metrics.getMappedPath()}"
  </#if>
  <#if metrics.getUserAgent()?? && pipeline?has_content>
  ,"user-agent":"${metrics.getUserAgent()?j_string}"
  <#else>
  ,"user-agent":""
  </#if>
  <#if requestContentLength??>
  ,"request-content-length":${requestContentLength}
  </#if>
  ,"request-ended":"${metrics.isRequestEnded()?c}"
  <#if metrics.getEntrypointId()??>
    ,"entrypoint-id":"${metrics.getEntrypointId()?j_string}"
  </#if>
  <#if metrics.getEndpoint()??>
  ,"endpoint":"${metrics.getEndpoint()?j_string}"
  </#if>
  <#if endpointResponseTimeMs??>
  ,"endpoint-response-time-ms":${endpointResponseTimeMs}
  </#if>
  <#if metrics.getStatus()??>
  ,"status":${metrics.getStatus()}
  </#if>
  <#if responseContentLength??>
  ,"response-content-length":${responseContentLength}
  </#if>
  <#if gatewayResponseTimeMs??>
  ,"gateway-response-time-ms":${gatewayResponseTimeMs}
  </#if>
  <#if gatewayLatencyMs??>
  ,"gateway-latency-ms":${gatewayLatencyMs}
  </#if>
  <#if metrics.getUser()??>
  ,"user":"${metrics.getUser()}"
  </#if>
  <#if metrics.getSecurityType()??>
  ,"security-type":"${metrics.getSecurityType()}"
  </#if>
  <#if metrics.getSecurityToken()??>
  ,"security-token":"${metrics.getSecurityToken()}"
  </#if>
  <#if metrics.getErrorMessage()??>
  ,"error-message":"${metrics.getErrorMessage()?j_string}"
  </#if>
  <#if metrics.getErrorKey()??>
  ,"error-key":"${metrics.getErrorKey()}"
  </#if>
  <#if metrics.getCustomMetrics()??>
  ,"custom": {
  <#list metrics.getCustomMetrics() as propKey, propValue>
    "${propKey}":"${propValue}"<#sep>,
  </#list>
  }
  </#if>
}</@compress>
