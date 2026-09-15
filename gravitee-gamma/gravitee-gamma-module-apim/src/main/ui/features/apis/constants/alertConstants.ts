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
import type {
    AlertAggregationFunction,
    AlertConditionType,
    AlertDampeningMode,
    AlertOperator,
    AlertRuleId,
    AlertStringOperator,
    AlertTimeUnit,
} from '../types';
import { GATEWAY_ERROR_KEY_OPTIONS } from './gatewayErrorKeys';

export type AlertRuleCategory = 'API metrics' | 'Health-check';

export interface AlertRuleDefinition {
    id: AlertRuleId;
    source: string;
    type: string;
    description: string;
    category: AlertRuleCategory;
}

export const API_ALERT_RULE_CATEGORY_ORDER: AlertRuleCategory[] = ['API metrics', 'Health-check'];

export function getAlertRuleCategoriesForApi(): AlertRuleCategory[] {
    return API_ALERT_RULE_CATEGORY_ORDER;
}

export const ALERT_RULES: AlertRuleDefinition[] = [
    {
        id: 'REQUEST@METRICS_SIMPLE_CONDITION',
        source: 'REQUEST',
        type: 'METRICS_SIMPLE_CONDITION',
        description: 'Alert when a metric of the request validates a condition',
        category: 'API metrics',
    },
    {
        id: 'REQUEST@MISSING_DATA',
        source: 'REQUEST',
        type: 'MISSING_DATA',
        description: 'Alert when there is no request matching filters received for a period of time',
        category: 'API metrics',
    },
    {
        id: 'REQUEST@METRICS_AGGREGATION',
        source: 'REQUEST',
        type: 'METRICS_AGGREGATION',
        description: 'Alert when the aggregated value of a request metric rises a threshold',
        category: 'API metrics',
    },
    {
        id: 'REQUEST@METRICS_RATE',
        source: 'REQUEST',
        type: 'METRICS_RATE',
        description: 'Alert when the rate of a given condition rises a threshold',
        category: 'API metrics',
    },
    {
        id: 'ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED',
        source: 'ENDPOINT_HEALTH_CHECK',
        type: 'API_HC_ENDPOINT_STATUS_CHANGED',
        description: 'Alert when the health status of an endpoint has changed',
        category: 'Health-check',
    },
];

export interface AlertMetricValueOption {
    value: string;
    label: string;
}

export type AlertMetricValueSource = 'tenants' | 'applications' | 'plans' | 'empty';

export interface AlertMetricDefinition {
    key: string;
    label: string;
    conditionTypes: AlertConditionType[];
    /** Classic `Metrics.supportPropertyProjection` — group-by Aggregation properties. */
    supportPropertyProjection?: boolean;
    /** Classic static `Metrics.loader` tuples. */
    valueOptions?: AlertMetricValueOption[];
    /** Classic async / empty loaders. `empty` still shows Value for EQUALS. */
    valueSource?: AlertMetricValueSource;
}

export const HEALTHCHECK_STATUS_VALUES: AlertMetricValueOption[] = [
    { value: 'DOWN', label: 'Down' },
    { value: 'TRANSITIONALLY_DOWN', label: 'Transitionally down' },
    { value: 'TRANSITIONALLY_UP', label: 'Transitionally up' },
    { value: 'UP', label: 'Up' },
];

export const API_METRICS: AlertMetricDefinition[] = [
    { key: 'response.response_time', label: 'Response Time (ms)', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE', 'COMPARE'] },
    {
        key: 'response.upstream_response_time',
        label: 'Upstream Response Time (ms)',
        conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE', 'COMPARE'],
    },
    { key: 'response.status', label: 'Status Code', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE'], supportPropertyProjection: true },
    { key: 'request.content_length', label: 'Request Content-Length', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE', 'COMPARE'] },
    { key: 'response.content_length', label: 'Response Content-Length', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE', 'COMPARE'] },
    {
        key: 'error.key',
        label: 'Error Key',
        conditionTypes: ['STRING'],
        supportPropertyProjection: true,
        valueOptions: GATEWAY_ERROR_KEY_OPTIONS,
    },
    { key: 'tenant', label: 'Tenant', conditionTypes: ['STRING'], supportPropertyProjection: true, valueSource: 'tenants' },
    {
        key: 'application',
        label: 'Application',
        conditionTypes: ['STRING'],
        supportPropertyProjection: true,
        valueSource: 'applications',
    },
    { key: 'plan', label: 'Plan', conditionTypes: ['STRING'], supportPropertyProjection: true, valueSource: 'plans' },
];

export const AGGREGATION_METRICS: AlertMetricDefinition[] = [
    { key: 'response.response_time', label: 'Response Time (ms)', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE', 'COMPARE'] },
    {
        key: 'response.upstream_response_time',
        label: 'Upstream Response Time (ms)',
        conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE', 'COMPARE'],
    },
    { key: 'request.content_length', label: 'Request Content-Length', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE', 'COMPARE'] },
    { key: 'response.content_length', label: 'Response Content-Length', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE', 'COMPARE'] },
];

/** Classic `HealthcheckMetrics` — API health-check filters. */
export const HEALTHCHECK_METRICS: AlertMetricDefinition[] = [
    { key: 'status.old', label: 'Old Status', conditionTypes: ['STRING'], valueOptions: HEALTHCHECK_STATUS_VALUES },
    { key: 'status.new', label: 'New Status', conditionTypes: ['STRING'], valueOptions: HEALTHCHECK_STATUS_VALUES },
    { key: 'endpoint.name', label: 'Endpoint name', conditionTypes: ['STRING'], supportPropertyProjection: true },
    { key: 'response_time', label: 'Response Time (ms)', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE', 'COMPARE'] },
    { key: 'tenant', label: 'Tenant', conditionTypes: ['STRING'], valueSource: 'tenants' },
];

const ALL_KNOWN_METRICS: AlertMetricDefinition[] = [...API_METRICS, ...HEALTHCHECK_METRICS];

export const ALERT_OPERATORS: { value: AlertOperator; label: string }[] = [
    { value: 'LT', label: 'less than' },
    { value: 'LTE', label: 'less than or equals to' },
    { value: 'GTE', label: 'greater than or equals to' },
    { value: 'GT', label: 'greater than' },
];

export const ALERT_STRING_OPERATORS: { value: AlertStringOperator; label: string }[] = [
    { value: 'EQUALS', label: 'equals to' },
    { value: 'NOT_EQUALS', label: 'not equals to' },
    { value: 'STARTS_WITH', label: 'starts with' },
    { value: 'ENDS_WITH', label: 'ends with' },
    { value: 'CONTAINS', label: 'contains' },
    { value: 'MATCHES', label: 'matches' },
];

export const AGGREGATION_FUNCTIONS: { value: AlertAggregationFunction; label: string }[] = [
    { value: 'COUNT', label: 'count' },
    { value: 'AVG', label: 'average' },
    { value: 'MIN', label: 'min' },
    { value: 'MAX', label: 'max' },
    { value: 'P50', label: '50th percentile' },
    { value: 'P90', label: '90th percentile' },
    { value: 'P95', label: '95th percentile' },
    { value: 'P99', label: '99th percentile' },
];

export const TIME_UNITS: { value: AlertTimeUnit; label: string }[] = [
    { value: 'SECONDS', label: 'Seconds' },
    { value: 'MINUTES', label: 'Minutes' },
    { value: 'HOURS', label: 'Hours' },
];

export const DAMPENING_MODES: { value: AlertDampeningMode; label: string }[] = [
    { value: 'STRICT_COUNT', label: 'N consecutive true evaluations' },
    { value: 'RELAXED_COUNT', label: 'N true evaluations out of M total evaluations' },
    { value: 'RELAXED_TIME', label: 'N true evaluations in T time' },
    { value: 'STRICT_TIME', label: 'Only true evaluations for at least T time' },
];

export function getConditionTypesForMetric(metricKey: string, metrics: AlertMetricDefinition[]): AlertConditionType[] {
    return metrics.find(m => m.key === metricKey)?.conditionTypes ?? [];
}

/** Classic compare `property2` list: other COMPARE metrics, not the left-hand property. */
export function getCompareTargetMetrics(metrics: AlertMetricDefinition[], property: string): AlertMetricDefinition[] {
    return metrics.filter(m => m.conditionTypes.includes('COMPARE') && m.key !== property);
}

export function isStringMetric(metricKey: string): boolean {
    const m = ALL_KNOWN_METRICS.find(met => met.key === metricKey);
    return !!m && m.conditionTypes.includes('STRING') && !m.conditionTypes.includes('THRESHOLD');
}

export function ruleSupportsProjections(ruleId: AlertRuleId): boolean {
    return (
        ruleId === 'REQUEST@METRICS_AGGREGATION' ||
        ruleId === 'REQUEST@METRICS_RATE' ||
        ruleId === 'ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED'
    );
}

/** Classic projection metrics (`supportPropertyProjection`), not the aggregation function metric. */
export function getProjectionMetricsForRuleId(ruleId: AlertRuleId): AlertMetricDefinition[] {
    if (!ruleSupportsProjections(ruleId)) {
        return [];
    }
    if (ruleId === 'ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED') {
        return HEALTHCHECK_METRICS.filter(m => m.supportPropertyProjection);
    }
    return API_METRICS.filter(m => m.supportPropertyProjection);
}

export function getMetricsForRuleId(ruleId: AlertRuleId | undefined): AlertMetricDefinition[] {
    if (!ruleId) {
        return [];
    }
    if (ruleId === 'REQUEST@METRICS_AGGREGATION') {
        return AGGREGATION_METRICS;
    }
    if (ruleId === 'ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED') {
        return HEALTHCHECK_METRICS;
    }
    return API_METRICS;
}

/** Classic filter metrics follow the trigger source, not the condition metric subset. */
export function getFilterMetricsForRuleId(ruleId: AlertRuleId): AlertMetricDefinition[] {
    switch (ruleIdToSourceType(ruleId).source) {
        case 'REQUEST':
            return API_METRICS;
        case 'ENDPOINT_HEALTH_CHECK':
            return HEALTHCHECK_METRICS;
        default:
            return [];
    }
}

export function ruleIdToSourceType(ruleId: AlertRuleId): { source: string; type: string } {
    const atIdx = ruleId.indexOf('@');
    return { source: ruleId.slice(0, atIdx), type: ruleId.slice(atIdx + 1) };
}

export function sourceTypeToRuleId(source: string, type: string): AlertRuleId | undefined {
    return ALERT_RULES.find(r => r.id === `${source}@${type}`)?.id;
}

export function getAlertRuleLabel(source: string, type: string): string {
    const ruleId = `${source}@${type}`;
    return ALERT_RULES.find(rule => rule.id === ruleId)?.description ?? `${source} / ${type}`;
}

/** Rules with no configurable condition fields (status-change). */
export function isInfoOnlyRule(ruleId: AlertRuleId): boolean {
    return ruleId === 'ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED';
}
