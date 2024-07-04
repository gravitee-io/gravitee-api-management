<#-- @ftlvariable name="index" type="java.lang.String" -->
<#-- @ftlvariable name="@timestamp" type="java.lang.String" -->
<#-- @ftlvariable name="log" type="io.gravitee.reporter.api.v4.log.Log" -->

<#if index??>
{ "index" : { "_index" : "${index}", "_id" : "${log.getRequestId()}" } }
</#if>
<@compress single_line=true>
{
  "@timestamp":"${@timestamp}"
  <#if !index??>
  ,"type": "v4-log"
  ,"date" : "${date}"
  ,"_id" : "${log.getRequestId()}"
  </#if>
  ,"api-id":"${log.getApiId()}"
  ,"api-name":"${log.getApiName()?j_string}"
  ,"request-id":"${log.getRequestId()}"
  ,"client-identifier":"${log.getClientIdentifier()}"
  ,"request-ended":"${log.isRequestEnded()?c}"
  <#if log.getEntrypointRequest()??>
  ,"entrypoint-request": {
  "method":"${log.getEntrypointRequest().getMethod()}",
  "uri":"${log.getEntrypointRequest().getUri()?j_string}"
    <#if log.getEntrypointRequest().getBody()??>
    ,"body":"${log.getEntrypointRequest().getBody()?j_string}"
    </#if>
    <#if log.getEntrypointRequest().getHeaders()??>
    ,"headers":{
      <#list log.getEntrypointRequest().getHeaders() as headerKey, headerValue>
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
  <#if log.getEntrypointResponse()??>
  ,"entrypoint-response": {
  "status":${log.getEntrypointResponse().getStatus()}
    <#if log.getEntrypointResponse().getBody()??>
    ,"body":"${log.getEntrypointResponse().getBody()?j_string}"
    </#if>
    <#if log.getEntrypointResponse().getHeaders()??>
    ,"headers":{
      <#list log.getEntrypointResponse().getHeaders() as headerKey, headerValue>
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
  <#if log.getEndpointRequest()??>
  ,"endpoint-request": {
  "method":"${log.getEndpointRequest().getMethod()}",
  "uri":"${log.getEndpointRequest().getUri()?j_string}"
    <#if log.getEndpointRequest().getBody()??>
    ,"body":"${log.getEndpointRequest().getBody()?j_string}"
    </#if>
    <#if log.getEndpointRequest().getHeaders()??>
    ,"headers":{
      <#list log.getEndpointRequest().getHeaders() as headerKey, headerValue>
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
  <#if log.getEndpointResponse()??>
  ,"endpoint-response": {
  "status":${log.getEndpointResponse().getStatus()}
    <#if log.getEndpointResponse().getBody()??>
    ,"body":"${log.getEndpointResponse().getBody()?j_string}"
    </#if>
    <#if log.getEndpointResponse().getHeaders()??>
    ,"headers":{
      <#list log.getEndpointResponse().getHeaders() as headerKey, headerValue>
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
