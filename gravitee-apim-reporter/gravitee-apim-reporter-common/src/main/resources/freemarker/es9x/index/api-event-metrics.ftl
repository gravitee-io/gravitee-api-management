<#import "../../common/es/event/base-event-metrics.ftl" as base />
<@base.baseEventMetrics metrics @timestamp index date>
    <#include "../../common/es/event/api-event-metrics-fields.ftl"/>
</@base.baseEventMetrics>