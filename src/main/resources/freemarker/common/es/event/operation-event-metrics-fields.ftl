<#-- @ftlvariable name="metrics" type="io.gravitee.reporter.api.v4.metric.event.OperationEventMetrics" -->
<#if metrics.getOperation()??>
,"operation": "${metrics.getOperation()}"
</#if>
<#if metrics.getRequestDurationsMillis()??>
,"request-durations-millis": ${metrics.getRequestDurationsMillis()}
</#if>
<#if metrics.getEndpointDurationsMillis()??>
,"endpoint-durations-millis": ${metrics.getEndpointDurationsMillis()}
</#if>
<#if metrics.getResponseDurationsMillis()??>
,"response-durations-millis": ${metrics.getResponseDurationsMillis()}
</#if>
<#if metrics.getRequestsTotal()??>
,"requests-total": ${metrics.getRequestsTotal()}
</#if>
<#if metrics.getEndpointRequestsTotal()??>
,"endpoint-requests-total": ${metrics.getEndpointRequestsTotal()}
</#if>
<#if metrics.getEndpointResponsesTotal()??>
,"endpoint-responses-total": ${metrics.getEndpointResponsesTotal()}
</#if>
<#if metrics.getResponsesTotal()??>
,"responses-total": ${metrics.getResponsesTotal()}
</#if>