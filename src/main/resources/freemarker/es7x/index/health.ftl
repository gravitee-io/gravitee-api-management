<#ftl output_format="JSON">
<#-- @ftlvariable name="index" type="java.lang.String" -->
<#-- @ftlvariable name="@timestamp" type="java.lang.String" -->
<#-- @ftlvariable name="gateway" type="java.lang.String" -->
<#-- @ftlvariable name="date" type="java.lang.String" -->
<#-- @ftlvariable name="status" type="io.gravitee.reporter.api.health.EndpointStatus" -->
<#macro stringOrNull data="">
    <#if data != "">
    "${data}"<#else>
    null</#if>
</#macro>
<#if index??>
{ "index" : { "_index" : "${index}", "_id" : "${status.getId()}" } }
</#if>
<#--noinspection FtlReferencesInspection-->
<@compress single_line=true>
{
    "@timestamp":"${@timestamp}",
<#if !index??>
    "type": "health",
    "date" : "${date}",
    "_id" : "${status.getId()}",
</#if>
    "api":"${status.getApi()}",
    "api-name":"${status.getApiName()?j_string}",
    "endpoint":"${status.getEndpoint()?j_string}",
    "available":${status.isAvailable()?c},
    "response-time":${status.getResponseTime()},
    "success":${status.isSuccess()?c},
    "state":${status.getState()},
    "transition":${status.isTransition()?c},
    "steps": [
<#list status.getSteps() as step>
        {"name": "${step.getName()}",
        "success":${step.isSuccess()?c},
        "request": {
            "uri":"${step.getRequest().getUri()?j_string}",
            "method":"${step.getRequest().getMethod()}"
            <#if step.getRequest().getBody()??>
            ,"body":"${step.getRequest().getBody()?j_string}"
            </#if>
            <#if step.getRequest().getHeaders()??>
            ,"headers":{
                <#list step.getRequest().getHeaders() as headerKey, headerValue>
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
        },
        "response": {
            "status":${step.getResponse().getStatus()}
            <#if step.getResponse().getBody()??>
            ,"body":"${step.getResponse().getBody()?j_string}"
            </#if>
            <#if step.getResponse().getHeaders()??>
            ,"headers":{
                <#--noinspection FtlTypesInspection-->
                <#list step.getResponse().getHeaders() as headerKey, headerValue>
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
        },
        "response-time":${step.getResponseTime()},
        "message":<@stringOrNull data=step.getMessage()/>
    }<#sep>,</#sep>
</#list>],
    "gateway":"${gateway}"
}</@compress>
