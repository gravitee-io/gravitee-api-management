<@compress single_line=true>
{
  "type": "request",
  "date" : "${date}"
  ,"_id" : "${metrics.getRequestId()}"
  ,"gateway":"${gateway}"
  ,"@timestamp":"${@timestamp}"
  ,"transaction":"${metrics.getTransactionId()}"
  ,"method":${metrics.getHttpMethod().code()?c}
  ,"uri":"${metrics.getUri()}"
  ,"status":${metrics.getStatus()}
  ,"response-time":${metrics.getProxyResponseTimeMs()}
  <#if apiResponseTime??>
    ,"api-response-time":${apiResponseTime}
  </#if>
  <#if proxyLatency??>
  ,"proxy-latency":${proxyLatency}
  </#if>
  <#if requestContentLength??>
  ,"request-content-length":${requestContentLength}
  </#if>
  <#if responseContentLength??>
  ,"response-content-length":${responseContentLength}
  </#if>
  <#if metrics.getPlan()??>
  ,"plan":"${metrics.getPlan()}"
  </#if>
  <#if metrics.getApi()??>
  ,"api":"${metrics.getApi()}"
  </#if>
  <#if metrics.getApplication()??>
  ,"application":"${metrics.getApplication()}"
  </#if>
  ,"local-address":"${metrics.getLocalAddress()}"
  ,"remote-address":"${metrics.getRemoteAddress()}"
  <#if metrics.getEndpoint()??>
  ,"endpoint":"${metrics.getEndpoint()}"
  </#if>
  <#if metrics.getTenant()??>
  ,"tenant":"${metrics.getTenant()}"
  </#if>
  <#if metrics.getMessage()??>
  ,"message":"${metrics.getMessage()?j_string}"
  </#if>
  <#if metrics.getPath()??>
  ,"path":"${metrics.getPath()}"
  </#if>
  <#if metrics.getMappedPath()??>
  ,"mapped-path":"${metrics.getMappedPath()}"
  </#if>
  <#if metrics.getHost()??>
  ,"host":"${metrics.getHost()}"
  </#if>
  <#if metrics.getUserAgent()??>
  ,"user-agent":"${metrics.getUserAgent()?j_string}"
  <#else>
  ,"user-agent":""
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
  <#if metrics.getErrorKey()??>
  ,"error-key":"${metrics.getErrorKey()}"
  </#if>
  <#if metrics.getSubscription()??>
  ,"subscription":"${metrics.getSubscription()}"
  </#if>
  <#if metrics.getCustomMetrics()??>
    ,"custom": {
    <#list metrics.getCustomMetrics() as propKey, propValue>
      "${propKey}":"${propValue}"<#sep>,
    </#list>
    }
  </#if>
}</@compress>