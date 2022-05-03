<#ftl output_format="JSON">
<#macro stringOrNull data="">
    <#if data != "">
    "${data}"<#else>
    null</#if>
</#macro>
{ "index" : { "_index" : "${index}", "_id" : "${status.getId()}" } }
<@compress single_line=true>
{
    "gateway":"${gateway}",
    "api":"${status.getApi()}",
    "endpoint":"${status.getEndpoint()}",
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
            "uri":"${step.getRequest().getUri()}",
            "method":"${step.getRequest().getMethod()}"
            <#if step.getRequest().getBody()??>
            ,"body":"${step.getRequest().getBody()?j_string}"
            </#if>
            <#if step.getRequest().getHeaders()??>
            ,"headers":{
            <#list step.getRequest().getHeaders().names() as header>
                "${header}": [
                <#list step.getRequest().getHeaders().getAll(header) as value>
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
        },
        "response": {
            "status":${step.getResponse().getStatus()}
            <#if step.getResponse().getBody()??>
            ,"body":"${step.getResponse().getBody()?j_string}"
            </#if>
            <#if step.getResponse().getHeaders()??>
            ,"headers":{
                <#list step.getResponse().getHeaders().names() as header>
                    "${header}": [
                    <#list step.getResponse().getHeaders().getAll(header) as value>
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
        },
        "response-time":${step.getResponseTime()},
        "message":<@stringOrNull data=step.getMessage()/>
    }<#sep>,</#sep>
</#list>],
"@timestamp":"${@timestamp}"
}</@compress>
