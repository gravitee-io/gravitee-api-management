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
    | 'ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED';

export type AlertConditionType = 'STRING' | 'THRESHOLD' | 'THRESHOLD_RANGE' | 'COMPARE' | 'AGGREGATION' | 'RATE' | 'MISSING_DATA';

export type AlertOperator = 'LT' | 'LTE' | 'GTE' | 'GT';
export type AlertStringOperator = 'EQUALS' | 'NOT_EQUALS' | 'STARTS_WITH' | 'ENDS_WITH' | 'CONTAINS' | 'MATCHES';
export type AlertAggregationFunction = 'COUNT' | 'AVG' | 'MIN' | 'MAX' | 'P50' | 'P90' | 'P95' | 'P99';
export type AlertTimeUnit = 'SECONDS' | 'MINUTES' | 'HOURS';
export type AlertDampeningMode = 'STRICT_COUNT' | 'RELAXED_COUNT' | 'RELAXED_TIME' | 'STRICT_TIME';
export type AlertNotificationChannel = 'email-notifier' | 'slack-notifier' | 'default-email' | 'webhook-notifier';

/** Denormalized condition used as form state — flat for easy React updates. */
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
    /** RATE only — operator for the comparison (inner) condition */
    rateOperator?: AlertOperator;
    /** RATE only — rate percentage threshold (0-100) */
    rateThreshold?: number;
}

/** Simplified notification shape used in form state. */
export interface AlertFormNotification {
    channel: AlertNotificationChannel;
    target: string;
}

/** Timeframe used in form state. */
export interface AlertFormTimeframe {
    days: number[];
    startHour: number;
    endHour: number;
}

export interface AlertDampening {
    mode: AlertDampeningMode;
    trueEvaluations?: number;
    totalEvaluations?: number;
    duration?: number;
    timeUnit?: AlertTimeUnit;
}

/** The raw API shape for a condition (sent to / received from the server). */
export interface AlertApiCondition {
    type: AlertConditionType;
    property?: string;
    operator?: AlertOperator | AlertStringOperator;
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
}

/** API notification payload shape. */
export interface AlertApiNotification {
    type: string;
    configuration?: Record<string, unknown>;
}

/** API notification period shape. */
export interface AlertApiPeriod {
    days: number[];
    beginHour: number;
    endHour: number;
    zoneId?: string;
}

/** Full alert trigger entity returned by the API. */
export interface AlertTrigger {
    id?: string;
    name: string;
    description?: string;
    severity: AlertSeverity;
    enabled: boolean;
    source: string;
    type: string;
    conditions?: AlertApiCondition[];
    filters?: AlertApiCondition[];
    notifications?: AlertApiNotification[];
    notificationPeriods?: AlertApiPeriod[];
    dampening?: AlertDampening;
    counters?: Record<string, number>;
    last_alert_at?: string | null;
    last_alert_message?: string | null;
    created_at?: string;
    updated_at?: string;
}

export interface AlertHistoryEvent {
    id: string;
    message: string;
    createdAt: string;
}

export interface AlertHistoryPage {
    content: AlertHistoryEvent[];
    totalElements: number;
}
