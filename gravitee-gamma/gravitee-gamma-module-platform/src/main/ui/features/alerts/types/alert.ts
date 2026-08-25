/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL';

export type AlertRuleId =
    | 'REQUEST@METRICS_SIMPLE_CONDITION'
    | 'REQUEST@MISSING_DATA'
    | 'REQUEST@METRICS_AGGREGATION'
    | 'REQUEST@METRICS_RATE'
    | 'ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED'
    | 'NODE_LIFECYCLE@NODE_LIFECYCLE_CHANGED'
    | 'NODE_HEARTBEAT@METRICS_SIMPLE_CONDITION'
    | 'NODE_HEARTBEAT@METRICS_AGGREGATION'
    | 'NODE_HEARTBEAT@METRICS_RATE'
    | 'NODE_HEALTHCHECK@NODE_HEALTHCHECK';

export type AlertConditionType =
    | 'STRING'
    | 'STRING_COMPARE'
    | 'THRESHOLD'
    | 'THRESHOLD_RANGE'
    | 'COMPARE'
    | 'AGGREGATION'
    | 'RATE'
    | 'MISSING_DATA';

export type AlertComparisonType = 'STRING' | 'THRESHOLD' | 'THRESHOLD_RANGE' | 'COMPARE';

export type AlertOperator = 'LT' | 'LTE' | 'GTE' | 'GT';
export type AlertStringOperator = 'EQUALS' | 'NOT_EQUALS' | 'STARTS_WITH' | 'ENDS_WITH' | 'CONTAINS' | 'MATCHES';
export type AlertAggregationFunction = 'COUNT' | 'AVG' | 'MIN' | 'MAX' | 'P50' | 'P90' | 'P95' | 'P99';
export type AlertTimeUnit = 'SECONDS' | 'MINUTES' | 'HOURS';
export type AlertDampeningMode = 'STRICT_COUNT' | 'RELAXED_COUNT' | 'RELAXED_TIME' | 'STRICT_TIME';

export interface AlertPropertyProjection {
    type: 'PROPERTY';
    property: string;
}

export interface AlertFormCondition {
    type: AlertConditionType;
    property?: string;
    operator?: AlertOperator | AlertStringOperator;
    threshold?: number;
    thresholdLow?: number;
    thresholdHigh?: number;
    pattern?: string;
    property2?: string;
    multiplier?: number;
    duration?: number;
    timeUnit?: AlertTimeUnit;
    aggregationFunction?: AlertAggregationFunction;
    /** Classic rate `conditions[0].comparison.type`. */
    comparisonType?: AlertComparisonType;
    rateOperator?: AlertOperator;
    rateThreshold?: number;
    projections?: unknown[];
}

export interface AlertFormNotification {
    type: string;
    configuration: Record<string, unknown>;
}

export interface AlertFormTimeframe {
    days: number[];
    /** Seconds since midnight — classic API `beginHour`. */
    startHour: number;
    /** Seconds since midnight — classic API `endHour`. */
    endHour: number;
}

export interface AlertDampening {
    mode: AlertDampeningMode;
    trueEvaluations?: number;
    totalEvaluations?: number;
    duration?: number;
    timeUnit?: AlertTimeUnit;
}

export interface AlertApiCondition {
    type: AlertConditionType;
    property?: string;
    /** THRESHOLD / STRING / AGGREGATION / RATE; THRESHOLD_RANGE uses classic `BETWEEN`. */
    operator?: AlertOperator | AlertStringOperator | 'BETWEEN';
    threshold?: number;
    thresholdLow?: number;
    thresholdHigh?: number;
    operatorLow?: 'INCLUSIVE' | 'EXCLUSIVE';
    operatorHigh?: 'INCLUSIVE' | 'EXCLUSIVE';
    pattern?: string;
    property2?: string;
    multiplier?: number;
    duration?: number;
    timeUnit?: AlertTimeUnit;
    function?: AlertAggregationFunction;
    comparison?: AlertApiCondition;
    projections?: unknown[];
}

export interface AlertApiNotification {
    type: string;
    configuration?: Record<string, unknown> | string;
}

export interface AlertApiPeriod {
    days: number[];
    beginHour: number;
    endHour: number;
    zoneId?: string;
}

export interface AlertTrigger {
    id?: string;
    name: string;
    description?: string;
    severity: AlertSeverity;
    enabled: boolean;
    source: string;
    type: string;
    reference_type?: string;
    reference_id?: string;
    conditions?: AlertApiCondition[];
    filters?: AlertApiCondition[];
    notifications?: AlertApiNotification[];
    notificationPeriods?: AlertApiPeriod[];
    dampening?: AlertDampening;
    projections?: unknown[];
    template?: boolean;
    event_rules?: unknown[];
    counters?: Record<string, number>;
    last_alert_at?: string | null;
    last_alert_message?: string | null;
    created_at?: string;
    updated_at?: string;
}

export interface AlertHistoryEvent {
    message: string;
    created_at: number;
}

export interface AlertHistoryPage {
    content: AlertHistoryEvent[];
    totalElements: number;
}

export interface AlertTriggerAnalytics {
    id: string;
    name: string;
    severity: AlertSeverity;
    type: string;
    events_count: number;
}

export interface AlertAnalytics {
    bySeverity: Partial<Record<AlertSeverity, number>>;
    alerts: AlertTriggerAnalytics[];
}
