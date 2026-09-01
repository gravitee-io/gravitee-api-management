<#import "../../common/es/event/base-event-metrics.ftl" as base />
<@base.baseEventMetrics metrics @timestamp index date>
    <#include "../../common/es/event/decision-event-metrics-fields.ftl"/>
</@base.baseEventMetrics>
