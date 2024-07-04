<#-- @ftlvariable name="index" type="java.lang.String" -->
<#-- @ftlvariable name="@timestamp" type="java.lang.String" -->
<#-- @ftlvariable name="log" type="io.gravitee.reporter.api.v4.log.MessageLog" -->
<#if index??>
{ "index" : { "_index" : "${index}", "_id" : "${log.getCorrelationId()}-${log.getConnectorType().getLabel()}" } }
</#if>
<@compress single_line=true>
{
  "@timestamp":"${@timestamp}"
  <#if !index??>
  ,"_id": "${log.getCorrelationId()}-${log.getConnectorType().getLabel()}"
  ,"type": "v4-message-log"
  ,"date" : "${date}"
  </#if>
  ,"api-id":"${log.getApiId()}"
  ,"api-name":"${log.getApiName()?j_string}"
  ,"request-id":"${log.getRequestId()}"
  ,"client-identifier":"${log.getClientIdentifier()}"
  ,"correlation-id":"${log.getCorrelationId()}"
  <#if log.getParentCorrelationId()??>
  ,"parent-correlation-id":"${log.getParentCorrelationId()}"
  </#if>
  ,"operation":"${log.getOperation().getLabel()}"
  ,"connector-type":"${log.getConnectorType().getLabel()}"
  ,"connector-id":"${log.getConnectorId()}"
  ,"message": {
    "id":"${(log.getMessage().getId())!}"
    <#if log.getMessage().isError()>
    ,"error":"${log.getMessage().isError()?c}"
    </#if>
    <#if log.getMessage().getPayload()??>
    ,"payload":"${log.getMessage().getPayload()?j_string}"
    </#if>
    <#if log.getMessage().getHeaders()??>
    ,"headers":{
      <#list log.getMessage().getHeaders() as headerKey, headerValue>
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
    <#if log.getMessage().getMetadata()??>
    ,"metadata":{
    <#list log.getMessage().getMetadata() as metadataKey, metadataValue>
      "${metadataKey}": "${metadataValue}"
      <#sep>,</#sep>
    </#list>
    }
    </#if>
  }
}</@compress>
