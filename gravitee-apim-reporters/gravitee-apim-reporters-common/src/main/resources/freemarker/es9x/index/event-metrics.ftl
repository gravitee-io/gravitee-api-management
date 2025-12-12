<#import "../../common/es/event/base-event-metrics.ftl" as base />
<#-- @ftlvariable name="@timestamp" type="java.lang.String" -->
<#-- @ftlvariable name="index" type="java.lang.String" -->
<#-- @ftlvariable name="metrics" type="io.gravitee.reporter.api.v4.metric.EventMetrics" -->
<@base.baseEventMetrics metrics @timestamp index date>
    <#include "../../common/es/event/event-metrics-fields.ftl"/>
</@base.baseEventMetrics>