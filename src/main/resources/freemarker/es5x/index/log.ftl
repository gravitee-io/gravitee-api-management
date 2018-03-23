<#ftl output_format="JSON">
<#macro stringOrNull data="">
  <#if data != "">
  "${data}"
  <#else>
  null
  </#if>
</#macro>
{ "index" : { "_index" : "${index}", "_type" : "${type}", "_id" : "${log.getRequestId()}" } }
<@compress single_line=true>
{
  <#if log.getClientRequest()??>
  "client-request": {
  "method":"${log.getClientRequest().getMethod()}",
  "uri":"${log.getClientRequest().getUri()}"
    <#if log.getClientRequest().getBody()??>
    ,"body":"${log.getClientRequest().getBody()?j_string}"
    </#if>
    <#if log.getClientRequest().getHeaders()??>
    ,"headers":{
      <#list log.getClientRequest().getHeaders() as headerKey, headerValue>
      "${headerKey}": [
        <#list headerValue as value>
        "${value?j_string}"
          <#sep>,</#sep>
        </#list>
      ]
        <#sep>,</#sep>
      </#list>
    }
    </#if>
  }
  ,"client-response": {
  "status":${log.getClientResponse().getStatus()}
    <#if log.getClientResponse().getBody()??>
    ,"body":"${log.getClientResponse().getBody()?j_string}"
    </#if>
    <#if log.getClientResponse().getHeaders()??>
    ,"headers":{
      <#list log.getClientResponse().getHeaders() as headerKey, headerValue>
      "${headerKey}": [
        <#list headerValue as value>
        "${value?j_string}"
          <#sep>,</#sep>
        </#list>
      ]
        <#sep>,</#sep>
      </#list>
    }
    </#if>
  }
  </#if>
  <#if log.getProxyRequest()??>
    <#if log.getClientRequest()??>,</#if>
  "proxy-request": {
  "method":"${log.getProxyRequest().getMethod()}",
  "uri":"${log.getProxyRequest().getUri()}"
    <#if log.getProxyRequest().getBody()??>
    ,"body":"${log.getProxyRequest().getBody()?j_string}"
    </#if>
    <#if log.getProxyRequest().getHeaders()??>
    ,"headers":{
      <#list log.getProxyRequest().getHeaders() as headerKey, headerValue>
      "${headerKey}": [
        <#list headerValue as value>
          <#if value??>
          "${value?j_string}"
            <#sep>,</#sep>
          </#if>
        </#list>
      ]
        <#sep>,</#sep>
      </#list>
    }
    </#if>
  }
  </#if>
  <#if log.getProxyResponse()??>
  ,"proxy-response": {
  "status":${log.getProxyResponse().getStatus()}
    <#if log.getProxyResponse().getBody()??>
    ,"body":"${log.getProxyResponse().getBody()?j_string}"
    </#if>
    <#if log.getProxyResponse().getHeaders()??>
    ,"headers":{
      <#list log.getProxyResponse().getHeaders() as headerKey, headerValue>
      "${headerKey}": [
        <#list headerValue as value>
          <#if value??>
          "${value?j_string}"
            <#sep>,</#sep>
          </#if>
        </#list>
      ]
        <#sep>,</#sep>
      </#list>
    }
    </#if>
  }
  </#if>
}</@compress>
