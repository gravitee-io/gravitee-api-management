<#-- @ftlvariable name="metrics" type="io.gravitee.reporter.api.v4.metric.event.OperationEventMetrics" -->
<#if metrics.getOperation()??>
,"operation": "${metrics.getOperation()}"
</#if>
<#if metrics.getUpstreamDurationsNanos()??>
,"upstream-durations-nanos": ${metrics.getUpstreamDurationsNanos()}
</#if>
<#if metrics.getEndpointDurationsNanos()??>
,"endpoint-durations-nanos": ${metrics.getEndpointDurationsNanos()}
</#if>
<#if metrics.getDownstreamDurationsNanos()??>
,"downstream-durations-nanos": ${metrics.getDownstreamDurationsNanos()}
</#if>
<#if metrics.getUpstreamCountIncrement()??>
,"upstream-count-increment": ${metrics.getUpstreamCountIncrement()}
</#if>
<#if metrics.getEndpointUpstreamCountIncrement()??>
,"endpoint-upstream-count-increment": ${metrics.getEndpointUpstreamCountIncrement()}
</#if>
<#if metrics.getEndpointDownstreamCountIncrement()??>
,"endpoint-downstream-count-increment": ${metrics.getEndpointDownstreamCountIncrement()}
</#if>
<#if metrics.getDownstreamCountIncrement()??>
,"downstream-count-increment": ${metrics.getDownstreamCountIncrement()}
</#if>