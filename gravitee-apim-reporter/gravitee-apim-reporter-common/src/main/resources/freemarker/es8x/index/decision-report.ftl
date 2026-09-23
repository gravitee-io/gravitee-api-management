<#import "../../common/es/decision/base-decision-report.ftl" as base />
<@base.baseDecisionReport report @timestamp index date>
    <#include "../../common/es/decision/decision-report-fields.ftl"/>
</@base.baseDecisionReport>
