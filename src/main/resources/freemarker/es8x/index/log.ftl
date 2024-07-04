<#-- @ftlvariable name="index" type="java.lang.String" -->
<#-- @ftlvariable name="@timestamp" type="java.lang.String" -->
<#-- @ftlvariable name="date" type="java.lang.String" -->
<#-- @ftlvariable name="log" type="io.gravitee.reporter.api.log.Log" -->
<#if index??>
{ "index" : { "_index" : "${index}", "_id" : "${log.getRequestId()}" } }
</#if>
<@compress single_line=true>
{
  "@timestamp":"${@timestamp}",
  <#if !index??>
    "type": "log",
    "date" : "${date}",
    "_id" : "${log.getRequestId()}",
  </#if>
  "api":"${log.getApi()}",
  "api-name":"${log.getApiName()?j_string}"
  <#if log.getClientRequest()??>
  ,"client-request": {
  "method":"${log.getClientRequest().getMethod()}",
  "uri":"${log.getClientRequest().getUri()?j_string}"
    <#if log.getClientRequest().getBody()??>
    ,"body":"${log.getClientRequest().getBody()?j_string}"
    </#if>
    <#if log.getClientRequest().getHeaders()??>
    ,"headers":{
      <#--noinspection FtlTypesInspection-->
      <#list log.getClientRequest().getHeaders() as headerKey, headerValue>
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
  <#if log.getClientResponse()??>
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
  <#if log.getProxyRequest()??>
  ,"proxy-request": {
  "method":"${log.getProxyRequest().getMethod()}",
  "uri":"${log.getProxyRequest().getUri()?j_string}"
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
