<#-- @ftlvariable name="metrics" type="io.gravitee.reporter.api.v4.metric.EventMetrics" -->
<#if metrics.getTopic()??>
,"topic": "${metrics.getTopic()}"
</#if>
<#if metrics.getDownstreamPublishMessagesCountIncrement()??>
,"downstream-publish-messages-count-increment": ${metrics.getDownstreamPublishMessagesCountIncrement()}
</#if>
<#if metrics.getDownstreamPublishMessageBytesIncrement()??>
,"downstream-publish-message-bytes-increment": ${metrics.getDownstreamPublishMessageBytesIncrement()}
</#if>
<#if metrics.getUpstreamPublishMessagesCountIncrement()??>
,"upstream-publish-messages-count-increment": ${metrics.getUpstreamPublishMessagesCountIncrement()}
</#if>
<#if metrics.getUpstreamPublishMessageBytesIncrement()??>
,"upstream-publish-message-bytes-increment": ${metrics.getUpstreamPublishMessageBytesIncrement()}
</#if>
<#if metrics.getDownstreamSubscribeMessagesCountIncrement()??>
,"downstream-subscribe-messages-count-increment": ${metrics.getDownstreamSubscribeMessagesCountIncrement()}
</#if>
<#if metrics.getDownstreamSubscribeMessageBytesIncrement()??>
,"downstream-subscribe-message-bytes-increment": ${metrics.getDownstreamSubscribeMessageBytesIncrement()}
</#if>
<#if metrics.getUpstreamSubscribeMessagesCountIncrement()??>
,"upstream-subscribe-messages-count-increment": ${metrics.getUpstreamSubscribeMessagesCountIncrement()}
</#if>
<#if metrics.getUpstreamSubscribeMessageBytesIncrement()??>
,"upstream-subscribe-message-bytes-increment": ${metrics.getUpstreamSubscribeMessageBytesIncrement()}
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
<#if metrics.getDownstreamAuthenticationFailuresCountIncrement()??>
,"downstream-authentication-failures-count-increment": ${metrics.getDownstreamAuthenticationFailuresCountIncrement()}
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