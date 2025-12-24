<#-- @ftlvariable name="metrics" type="io.gravitee.reporter.api.v4.metric.event.ApplicationEventMetrics" -->
<#if metrics.getUpstreamAuthenticatedConnections()??>
    ,"upstream-authenticated-connections": ${metrics.getUpstreamAuthenticatedConnections()}
</#if>
<#if metrics.getDownstreamAuthenticatedConnections()??>
    ,"downstream-authenticated-connections": ${metrics.getDownstreamAuthenticatedConnections()}
</#if>
<#if metrics.getUpstreamAuthenticationFailuresCountIncrement()??>
    ,"upstream-authentication-failures-count-increment": ${metrics.getUpstreamAuthenticationFailuresCountIncrement()}
</#if>
<#if metrics.getDownstreamAuthenticationSuccessesCountIncrement()??>
    ,"downstream-authentication-successes-count-increment": ${metrics.getDownstreamAuthenticationSuccessesCountIncrement()}
</#if>
<#if metrics.getUpstreamAuthenticationSuccessesCountIncrement()??>
    ,"upstream-authentication-successes-count-increment": ${metrics.getUpstreamAuthenticationSuccessesCountIncrement()}
</#if>