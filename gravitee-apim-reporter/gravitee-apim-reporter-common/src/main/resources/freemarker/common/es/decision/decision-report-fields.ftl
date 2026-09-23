<#if report.getEventId()??>
    ,"event-id": "${report.getEventId()?json_string}"
</#if>
<#if report.getCaseId()??>
    ,"case-id": "${report.getCaseId()?json_string}"
</#if>
<#if report.getBatchId()??>
    ,"batch-id": "${report.getBatchId()?json_string}"
</#if>
<#if report.getPhase()??>
    ,"phase": "${report.getPhase().getLabel()?json_string}"
</#if>
<#if report.getDecisionPointType()??>
    ,"decision-point-type": "${report.getDecisionPointType()?json_string}"
</#if>
<#if report.getDecisionPointId()??>
    ,"decision-point-id": "${report.getDecisionPointId()?json_string}"
</#if>
<#if report.getDecisionPointVersion()??>
    ,"decision-point-version": "${report.getDecisionPointVersion()?json_string}"
</#if>
<#if report.getCheckpoint()??>
    ,"checkpoint": "${report.getCheckpoint()?json_string}"
</#if>
<#if report.getCaller()??>
    ,"caller": "${report.getCaller()?json_string}"
</#if>
<#if report.getSubjectType()??>
    ,"subject-type": "${report.getSubjectType()?json_string}"
</#if>
<#if report.getSubjectId()??>
    ,"subject-id": "${report.getSubjectId()?json_string}"
</#if>
<#if report.getActorType()??>
    ,"actor-type": "${report.getActorType()?json_string}"
</#if>
<#if report.getActorId()??>
    ,"actor-id": "${report.getActorId()?json_string}"
</#if>
<#if report.getAction()??>
    ,"action": "${report.getAction()?json_string}"
</#if>
<#if report.getResourceType()??>
    ,"resource-type": "${report.getResourceType()?json_string}"
</#if>
<#if report.getResourceId()??>
    ,"resource-id": "${report.getResourceId()?json_string}"
</#if>
<#if report.getArgsHash()??>
    ,"args-hash": "${report.getArgsHash()?json_string}"
</#if>
<#if report.getOutcome()??>
    ,"outcome": "${report.getOutcome().getLabel()?json_string}"
</#if>
<#if report.getEnforced()??>
    ,"enforced": "${report.getEnforced().getLabel()?json_string}"
</#if>
<#if report.getVerdict()??>
    ,"verdict": "${report.getVerdict()?json_string}"
</#if>
<#if report.getIndeterminateCause()??>
    ,"indeterminate-cause": "${report.getIndeterminateCause().getLabel()?json_string}"
</#if>
<#if report.getConfidence()??>
    ,"confidence": ${report.getConfidence()?c}
</#if>
<#if report.getReasons()??>
    ,"reasons": [<#list report.getReasons() as v>"${(v!"")?json_string}"<#sep>,</#sep></#list>]
</#if>
<#if report.getMatchedRules()??>
    ,"matched-rules": [<#list report.getMatchedRules() as r>{
        "id": "${r.id()!?json_string}"
        <#if r.name()??>,"name": "${r.name()?json_string}"</#if>
        <#if r.version()??>,"version": "${r.version()?json_string}"</#if>
        <#if r.effect()??>,"effect": "${r.effect()?json_string}"</#if>
    }<#sep>,</#sep></#list>]
</#if>
<#if report.getTransformed()??>
    ,"transformed": ${report.getTransformed()?c}
</#if>
<#if report.getTransformationType()??>
    ,"transformation-type": "${report.getTransformationType().getLabel()?json_string}"
</#if>
<#if report.getRequiredApprover()??>
    ,"required-approver": "${report.getRequiredApprover()?json_string}"
</#if>
<#if report.getDeciderType()??>
    ,"decider-type": "${report.getDeciderType()?json_string}"
</#if>
<#if report.getDeciderId()??>
    ,"decider-id": "${report.getDeciderId()?json_string}"
</#if>
<#if report.getChannel()??>
    ,"channel": "${report.getChannel()?json_string}"
</#if>
<#if report.getRequestId()??>
    ,"request-id": "${report.getRequestId()?json_string}"
</#if>
<#if report.getTraceId()??>
    ,"trace-id": "${report.getTraceId()?json_string}"
</#if>
<#if report.getConversationId()??>
    ,"conversation-id": "${report.getConversationId()?json_string}"
</#if>
<#if report.getMissionId()??>
    ,"mission-id": "${report.getMissionId()?json_string}"
</#if>
<#if report.getStatus()??>
    ,"status": "${report.getStatus().getLabel()?json_string}"
</#if>
<#if report.getErrorType()??>
    ,"error-type": "${report.getErrorType()?json_string}"
</#if>
<#if report.getDurationNanos()??>
    ,"duration-nanos": ${report.getDurationNanos()?c}
</#if>
<#if report.getWaitedNanos()??>
    ,"waited-nanos": ${report.getWaitedNanos()?c}
</#if>
<#if (report.longAdditionalMetrics())?? || (report.doubleAdditionalMetrics())?? || (report.keywordAdditionalMetrics())?? || (report.boolAdditionalMetrics())?? || (report.intAdditionalMetrics())?? || (report.stringAdditionalMetrics())?? || (report.jsonAdditionalMetrics())?? || (report.keywordListAdditionalMetrics())??>
    ,"additional-metrics": {
    <#assign additionalMetrics = []>
    <#if (report.longAdditionalMetrics())??>
      <#list report.longAdditionalMetrics() as propKey, propValue>
        <#assign additionalMetrics = additionalMetrics + ['"' + propKey + '":' + propValue]>
      </#list>
    </#if>
    <#if (report.doubleAdditionalMetrics())??>
      <#list report.doubleAdditionalMetrics() as propKey, propValue>
        <#assign additionalMetrics = additionalMetrics + ['"' + propKey + '":' + propValue]>
      </#list>
    </#if>
    <#if (report.keywordAdditionalMetrics())??>
      <#list report.keywordAdditionalMetrics() as propKey, propValue>
        <#assign additionalMetrics = additionalMetrics + ['"' + propKey + '":"' + propValue?j_string + '"']>
      </#list>
    </#if>
    <#if (report.keywordListAdditionalMetrics())??>
      <#list report.keywordListAdditionalMetrics() as propKey, propValues>
        <#assign additionalMetrics = additionalMetrics + ['"' + propKey + '":[' + propValues?map(v -> '"' + v?j_string + '"')?join(',') + ']']>
      </#list>
    </#if>
    <#if (report.boolAdditionalMetrics())??>
      <#list report.boolAdditionalMetrics() as propKey, propValue>
        <#assign additionalMetrics = additionalMetrics + ['"' + propKey + '":' + propValue?string('true', 'false')]>
      </#list>
    </#if>
    <#if (report.intAdditionalMetrics())??>
      <#list report.intAdditionalMetrics() as propKey, propValue>
        <#assign additionalMetrics = additionalMetrics + ['"' + propKey + '":' + propValue]>
      </#list>
    </#if>
    <#if (report.stringAdditionalMetrics())??>
      <#list report.stringAdditionalMetrics() as propKey, propValue>
        <#assign additionalMetrics = additionalMetrics + ['"' + propKey + '":"' + propValue?j_string + '"']>
      </#list>
    </#if>
    <#if (report.jsonAdditionalMetrics())??>
      <#list report.jsonAdditionalMetrics() as propKey, propValue>
        <#assign additionalMetrics = additionalMetrics + ['"' + propKey + '":"' + propValue?j_string + '"']>
      </#list>
    </#if>
      ${additionalMetrics?join(',')}
  }
</#if>
