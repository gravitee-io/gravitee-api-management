<#-- @ftlvariable name="metrics" type="io.gravitee.reporter.api.v4.metric.event.ApiEventMetrics" -->
<#if metrics.getDownstreamActiveConnections()??>
    ,"downstream-active-connections": ${metrics.getDownstreamActiveConnections()}
</#if>
<#if metrics.getUpstreamActiveConnections()??>
    ,"upstream-active-connections": ${metrics.getUpstreamActiveConnections()}
</#if>
<#if metrics.getDownstreamAuthenticationFailuresCountIncrement()??>
    ,"downstream-authentication-failures-count-increment": ${metrics.getDownstreamAuthenticationFailuresCountIncrement()}
</#if>