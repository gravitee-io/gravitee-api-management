<#-- @ftlvariable name="metrics" type="io.gravitee.reporter.api.v4.metric.event.OperationEventMetrics" -->
<#if metrics.getOperation()??>
,"operation": "${metrics.getOperation()}"
</#if>
<#if metrics.getUpstreamDurationsMillis()??>
,"request-durations-millis": ${metrics.getUpstreamDurationsMillis()}
</#if>
<#if metrics.getEndpointDurationsMillis()??>
,"endpoint-durations-millis": ${metrics.getEndpointDurationsMillis()}
</#if>
<#if metrics.getDownstreamDurationsMillis()??>
,"response-durations-millis": ${metrics.getDownstreamDurationsMillis()}
</#if>
<#if metrics.getUpstreamCountIncrement()??>
,"requests-count-increment": ${metrics.getUpstreamCountIncrement()}
</#if>
<#if metrics.getEndpointUpstreamCountIncrement()??>
,"endpoint-requests-count-increment": ${metrics.getEndpointUpstreamCountIncrement()}
</#if>
<#if metrics.getEndpointDownstreamCountIncrement()??>
,"endpoint-responses-count-increment": ${metrics.getEndpointDownstreamCountIncrement()}
</#if>
<#if metrics.getDownstreamCountIncrement()??>
,"responses-count-increment": ${metrics.getDownstreamCountIncrement()}
</#if>