<#-- @ftlvariable name="metrics" type="io.gravitee.reporter.api.v4.metric.event.DecisionEventMetrics" -->
<#if metrics.getEventId()??>
    ,"event-id": "${metrics.getEventId()?json_string}"
</#if>
<#if metrics.getCaseId()??>
    ,"case-id": "${metrics.getCaseId()?json_string}"
</#if>
<#if metrics.getBatchId()??>
    ,"batch-id": "${metrics.getBatchId()?json_string}"
</#if>
<#if metrics.getPhase()??>
    ,"phase": "${metrics.getPhase().getLabel()?json_string}"
</#if>
<#if metrics.getDecisionPointType()??>
    ,"decision-point-type": "${metrics.getDecisionPointType()?json_string}"
</#if>
<#if metrics.getDecisionPointId()??>
    ,"decision-point-id": "${metrics.getDecisionPointId()?json_string}"
</#if>
<#if metrics.getDecisionPointVersion()??>
    ,"decision-point-version": "${metrics.getDecisionPointVersion()?json_string}"
</#if>
<#if metrics.getCheckpoint()??>
    ,"checkpoint": "${metrics.getCheckpoint()?json_string}"
</#if>
<#if metrics.getCaller()??>
    ,"caller": "${metrics.getCaller()?json_string}"
</#if>
<#if metrics.getSubjectType()??>
    ,"subject-type": "${metrics.getSubjectType()?json_string}"
</#if>
<#if metrics.getSubjectId()??>
    ,"subject-id": "${metrics.getSubjectId()?json_string}"
</#if>
<#if metrics.getActorType()??>
    ,"actor-type": "${metrics.getActorType()?json_string}"
</#if>
<#if metrics.getActorId()??>
    ,"actor-id": "${metrics.getActorId()?json_string}"
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
<#if metrics.getArgsHash()??>
    ,"args-hash": "${metrics.getArgsHash()?json_string}"
</#if>
<#if metrics.getOutcome()??>
    ,"outcome": "${metrics.getOutcome().getLabel()?json_string}"
</#if>
<#if metrics.getEnforced()??>
    ,"enforced": "${metrics.getEnforced().getLabel()?json_string}"
</#if>
<#if metrics.getVerdict()??>
    ,"verdict": "${metrics.getVerdict()?json_string}"
</#if>
<#if metrics.getIndeterminateCause()??>
    ,"indeterminate-cause": "${metrics.getIndeterminateCause().getLabel()?json_string}"
</#if>
<#if metrics.getConfidence()??>
    ,"confidence": ${metrics.getConfidence()?c}
</#if>
<#if metrics.getReasons()??>
    ,"reasons": [<#list metrics.getReasons() as v>"${(v!"")?json_string}"<#sep>,</#sep></#list>]
</#if>
<#if metrics.getMatchedRules()??>
    ,"matched-rules": [<#list metrics.getMatchedRules() as r>{
        "id": "${r.id()!?json_string}"
        <#if r.name()??>,"name": "${r.name()?json_string}"</#if>
        <#if r.effect()??>,"effect": "${r.effect()?json_string}"</#if>
    }<#sep>,</#sep></#list>]
</#if>
<#if metrics.getTransformed()??>
    ,"transformed": ${metrics.getTransformed()?c}
</#if>
<#if metrics.getTransformationType()??>
    ,"transformation-type": "${metrics.getTransformationType().getLabel()?json_string}"
</#if>
<#if metrics.getRequiredApprover()??>
    ,"required-approver": "${metrics.getRequiredApprover()?json_string}"
</#if>
<#if metrics.getDeciderType()??>
    ,"decider-type": "${metrics.getDeciderType()?json_string}"
</#if>
<#if metrics.getDeciderId()??>
    ,"decider-id": "${metrics.getDeciderId()?json_string}"
</#if>
<#if metrics.getChannel()??>
    ,"channel": "${metrics.getChannel()?json_string}"
</#if>
<#if metrics.getRequestId()??>
    ,"request-id": "${metrics.getRequestId()?json_string}"
</#if>
<#if metrics.getTraceId()??>
    ,"trace-id": "${metrics.getTraceId()?json_string}"
</#if>
<#if metrics.getConversationId()??>
    ,"conversation-id": "${metrics.getConversationId()?json_string}"
</#if>
<#if metrics.getMissionId()??>
    ,"mission-id": "${metrics.getMissionId()?json_string}"
</#if>
<#if metrics.getStatus()??>
    ,"status": "${metrics.getStatus().getLabel()?json_string}"
</#if>
<#if metrics.getErrorType()??>
    ,"error-type": "${metrics.getErrorType()?json_string}"
</#if>
<#if metrics.getDurationNanos()??>
    ,"duration-nanos": ${metrics.getDurationNanos()?c}
</#if>
<#if metrics.getWaitedNanos()??>
    ,"waited-nanos": ${metrics.getWaitedNanos()?c}
</#if>
<#if (metrics.longAdditionalMetrics())?? || (metrics.doubleAdditionalMetrics())?? || (metrics.keywordAdditionalMetrics())?? || (metrics.boolAdditionalMetrics())?? || (metrics.intAdditionalMetrics())?? || (metrics.stringAdditionalMetrics())?? || (metrics.jsonAdditionalMetrics())??>
    ,"additional-metrics": {
    <#assign additionalMetrics = []>
    <#if (metrics.longAdditionalMetrics())??>
      <#list metrics.longAdditionalMetrics() as propKey, propValue>
        <#assign additionalMetrics = additionalMetrics + ['"' + propKey + '":' + propValue]>
      </#list>
    </#if>
    <#if (metrics.doubleAdditionalMetrics())??>
      <#list metrics.doubleAdditionalMetrics() as propKey, propValue>
        <#assign additionalMetrics = additionalMetrics + ['"' + propKey + '":' + propValue]>
      </#list>
    </#if>
    <#if (metrics.keywordAdditionalMetrics())??>
      <#list metrics.keywordAdditionalMetrics() as propKey, propValue>
        <#assign additionalMetrics = additionalMetrics + ['"' + propKey + '":"' + propValue?j_string + '"']>
      </#list>
    </#if>
    <#if (metrics.boolAdditionalMetrics())??>
      <#list metrics.boolAdditionalMetrics() as propKey, propValue>
        <#assign additionalMetrics = additionalMetrics + ['"' + propKey + '":' + propValue?string('true', 'false')]>
      </#list>
    </#if>
    <#if (metrics.intAdditionalMetrics())??>
      <#list metrics.intAdditionalMetrics() as propKey, propValue>
        <#assign additionalMetrics = additionalMetrics + ['"' + propKey + '":' + propValue]>
      </#list>
    </#if>
    <#if (metrics.stringAdditionalMetrics())??>
      <#list metrics.stringAdditionalMetrics() as propKey, propValue>
        <#assign additionalMetrics = additionalMetrics + ['"' + propKey + '":"' + propValue?j_string + '"']>
      </#list>
    </#if>
    <#if (metrics.jsonAdditionalMetrics())??>
      <#list metrics.jsonAdditionalMetrics() as propKey, propValue>
        <#assign additionalMetrics = additionalMetrics + ['"' + propKey + '":"' + propValue?j_string + '"']>
      </#list>
    </#if>
      ${additionalMetrics?join(',')}
  }
</#if>
