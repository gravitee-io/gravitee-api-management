<#ftl output_format="JSON">
<#macro stringOrNull data="">
    <#if data != "">
    "${data}"<#else>
    null</#if>
</#macro>
{ "index" : { "_index" : "${index}", "_type" : "${type}", "_id" : "${status.getId()}" } }
<@compress single_line=true>
{
    "gateway":"${gateway}",
    "api":"${status.getApi()}",
    "endpoint":"${status.getEndpoint()}",
    "available":${status.isAvailable()?c},
    "response-time":${status.getResponseTime()},
    "success":${status.isSuccess()?c},
    "state":${status.getState()},
    "steps": [
<#list status.getSteps() as step>
        {"name": "${step.getName()}",
        "success":${step.isSuccess()?c},
        "request": {
            "uri":"${step.getRequest().getUri()}",
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
"@timestamp":"${@timestamp}"
}</@compress>
