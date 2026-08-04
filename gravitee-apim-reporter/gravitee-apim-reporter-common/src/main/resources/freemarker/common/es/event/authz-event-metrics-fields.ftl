<#-- @ftlvariable name="metrics" type="io.gravitee.reporter.api.v4.metric.event.AuthzEventMetrics" -->
<#if metrics.getOperation()??>
    ,"operation": "${metrics.getOperation()?json_string}"
</#if>
<#if metrics.getEventId()??>
    ,"event-id": "${metrics.getEventId()?json_string}"
</#if>
<#if metrics.getStatus()??>
    ,"status": "${metrics.getStatus()?json_string}"
</#if>
<#if metrics.getRequestId()??>
    ,"request-id": "${metrics.getRequestId()?json_string}"
</#if>
<#if metrics.getCaller()??>
    ,"caller": "${metrics.getCaller()?json_string}"
</#if>
<#if metrics.getTargetPdpId()??>
    ,"target-pdp-id": "${metrics.getTargetPdpId()?json_string}"
</#if>
<#if metrics.getPolicyGeneration()??>
    ,"policy-generation": ${metrics.getPolicyGeneration()?c}
</#if>
<#if metrics.getBatchId()??>
    ,"batch-id": "${metrics.getBatchId()?json_string}"
</#if>
<#if metrics.getBatchIndex()??>
    ,"batch-index": ${metrics.getBatchIndex()?c}
</#if>
<#if metrics.getBatchSize()??>
    ,"batch-size": ${metrics.getBatchSize()?c}
</#if>
<#if metrics.getSubjectType()??>
    ,"subject-type": "${metrics.getSubjectType()?json_string}"
</#if>
<#if metrics.getSubjectId()??>
    ,"subject-id": "${metrics.getSubjectId()?json_string}"
</#if>
<#if metrics.getAction()??>
    ,"action": "${metrics.getAction()?json_string}"
</#if>
<#if metrics.getResourceType()??>
    ,"resource-type": "${metrics.getResourceType()?json_string}"
</#if>
<#if metrics.getResourceId()??>
    ,"resource-id": "${metrics.getResourceId()?json_string}"
</#if>
<#if metrics.getDecision()??>
    ,"decision": "${metrics.getDecision()?json_string}"
</#if>
<#if metrics.getMatchedPolicies()??>
    ,"matched-policies": [<#list metrics.getMatchedPolicies() as p>{
        "id": "${p.id()!?json_string}"
        <#if p.name()??>,"name": "${p.name()?json_string}"</#if>
        <#if p.effect()??>,"effect": "${p.effect()?json_string}"</#if>
    }<#sep>,</#sep></#list>]
</#if>
<#if metrics.getReasons()??>
    ,"reasons": [<#list metrics.getReasons() as v>"${(v!"")?json_string}"<#sep>,</#sep></#list>]
</#if>
<#if metrics.getSearchType()??>
    ,"search-type": "${metrics.getSearchType()?json_string}"
</#if>
<#if metrics.getResultCount()??>
    ,"result-count": ${metrics.getResultCount()?c}
</#if>
<#if metrics.getPageSize()??>
    ,"page-size": ${metrics.getPageSize()?c}
</#if>
<#if metrics.getHasMore()??>
    ,"has-more": ${metrics.getHasMore()?c}
</#if>
<#if metrics.getErrorType()??>
    ,"error-type": "${metrics.getErrorType()?json_string}"
</#if>
<#if metrics.getDurationNanos()??>
    ,"duration-nanos": ${metrics.getDurationNanos()?c}
</#if>
