<#-- @ftlvariable name="metrics" type="io.gravitee.reporter.api.v4.metric.EventMetrics" -->
<#if metrics.getTopic()??>
,"topic": "${metrics.getTopic()}"
</#if>
<#if metrics.getDownstreamPublishMessagesTotal()??>
,"downstream-publish-messages-total": ${metrics.getDownstreamPublishMessagesTotal()}
</#if>
<#if metrics.getDownstreamPublishMessageBytes()??>
,"downstream-publish-message-bytes": ${metrics.getDownstreamPublishMessageBytes()}
</#if>
<#if metrics.getUpstreamPublishMessagesTotal()??>
,"upstream-publish-messages-total": ${metrics.getUpstreamPublishMessagesTotal()}
</#if>
<#if metrics.getUpstreamPublishMessageBytes()??>
,"upstream-publish-message-bytes": ${metrics.getUpstreamPublishMessageBytes()}
</#if>
<#if metrics.getDownstreamSubscribeMessagesTotal()??>
,"downstream-subscribe-messages-total": ${metrics.getDownstreamSubscribeMessagesTotal()}
</#if>
<#if metrics.getDownstreamSubscribeMessageBytes()??>
,"downstream-subscribe-message-bytes": ${metrics.getDownstreamSubscribeMessageBytes()}
</#if>
<#if metrics.getUpstreamSubscribeMessagesTotal()??>
,"upstream-subscribe-messages-total": ${metrics.getUpstreamSubscribeMessagesTotal()}
</#if>
<#if metrics.getUpstreamSubscribeMessageBytes()??>
,"upstream-subscribe-message-bytes": ${metrics.getUpstreamSubscribeMessageBytes()}
</#if>
<#if metrics.getDownstreamActiveConnections()??>
,"downstream-active-connections": ${metrics.getDownstreamActiveConnections()}
</#if>
<#if metrics.getUpstreamActiveConnections()??>
,"upstream-active-connections": ${metrics.getUpstreamActiveConnections()}
</#if>
<#if metrics.getUpstreamAuthenticatedConnections()??>
,"upstream-authenticated-connections": ${metrics.getUpstreamAuthenticatedConnections()}
</#if>
<#if metrics.getDownstreamAuthenticatedConnections()??>
,"downstream-authenticated-connections": ${metrics.getDownstreamAuthenticatedConnections()}
</#if>
<#if metrics.getDownstreamAuthenticationFailuresTotal()??>
,"downstream-authentication-failures-total": ${metrics.getDownstreamAuthenticationFailuresTotal()}
</#if>
<#if metrics.getUpstreamAuthenticationFailuresTotal()??>
,"upstream-authentication-failures-total": ${metrics.getUpstreamAuthenticationFailuresTotal()}
</#if>
<#if metrics.getDownstreamAuthorizationSuccessesTotal()??>
,"downstream-authentication-successes-total": ${metrics.getDownstreamAuthorizationSuccessesTotal()}
</#if>
<#if metrics.getUpstreamAuthorizationSuccessesTotal()??>
,"upstream-authentication-successes-total": ${metrics.getUpstreamAuthorizationSuccessesTotal()}
</#if>