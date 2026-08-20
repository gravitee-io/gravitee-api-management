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

export type AlertRuleCategory = 'API metrics' | 'Health-check' | 'Node';

export interface AlertRuleDefinition {
    id: AlertRuleId;
    source: string;
    type: string;
    description: string;
    category: AlertRuleCategory;
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
    {
        id: 'NODE_LIFECYCLE@NODE_LIFECYCLE_CHANGED',
        source: 'NODE_LIFECYCLE',
        type: 'NODE_LIFECYCLE_CHANGED',
        description: 'Alert when the lifecycle status of a node has changed',
        category: 'Node',
    },
    {
        id: 'NODE_HEARTBEAT@METRICS_SIMPLE_CONDITION',
        source: 'NODE_HEARTBEAT',
        type: 'METRICS_SIMPLE_CONDITION',
        description: 'Alert when a metric of the node validates a condition',
        category: 'Node',
    },
    {
        id: 'NODE_HEARTBEAT@METRICS_AGGREGATION',
        source: 'NODE_HEARTBEAT',
        type: 'METRICS_AGGREGATION',
        description: 'Alert when the aggregated value of a node metric rises a threshold',
        category: 'Node',
    },
    {
        id: 'NODE_HEARTBEAT@METRICS_RATE',
        source: 'NODE_HEARTBEAT',
        type: 'METRICS_RATE',
        description: 'Alert when the rate of a given condition rises a threshold',
        category: 'Node',
    },
    {
        id: 'NODE_HEALTHCHECK@NODE_HEALTHCHECK',
        source: 'NODE_HEALTHCHECK',
        type: 'NODE_HEALTHCHECK',
        description: 'Alert on the health status of the node',
        category: 'Node',
    },
];

export interface AlertMetricDefinition {
    key: string;
    label: string;
    conditionTypes: AlertConditionType[];
}

export const API_METRICS: AlertMetricDefinition[] = [
    { key: 'response.response_time', label: 'Response Time (ms)', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE', 'COMPARE'] },
    {
        key: 'response.upstream_response_time',
        label: 'Upstream Response Time (ms)',
        conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE', 'COMPARE'],
    },
    { key: 'response.status', label: 'Status Code', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE'] },
    { key: 'request.content_length', label: 'Request Content-Length', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE', 'COMPARE'] },
    { key: 'response.content_length', label: 'Response Content-Length', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE', 'COMPARE'] },
    { key: 'error.key', label: 'Error Key', conditionTypes: ['STRING'] },
    { key: 'tenant', label: 'Tenant', conditionTypes: ['STRING'] },
    { key: 'api', label: 'API', conditionTypes: ['STRING'] },
    { key: 'application', label: 'Application', conditionTypes: ['STRING'] },
    { key: 'plan', label: 'Plan', conditionTypes: ['STRING'] },
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

/** Node heartbeat metrics from classic `node.metrics.ts`. */
export const NODE_METRICS: AlertMetricDefinition[] = [
    { key: 'node.hostname', label: 'Hostname', conditionTypes: ['STRING'] },
    { key: 'node.application', label: 'Type', conditionTypes: ['STRING'] },
    { key: 'os.cpu.percent', label: 'OS CPU (%)', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE'] },
    { key: 'process.cpu.percent', label: 'Process CPU (%)', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE'] },
    { key: 'process.cpu.total', label: 'Process CPU (total)', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE'] },
    { key: 'jvm.mem.heap.used', label: 'JVM Heap used', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE'] },
    { key: 'jvm.mem.heap.max', label: 'JVM Heap max', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE'] },
    { key: 'jvm.mem.heap.percent', label: 'JVM Heap (%)', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE'] },
];

export const NODE_AGGREGATION_METRICS: AlertMetricDefinition[] = [
    { key: 'os.cpu.percent', label: 'OS CPU (%)', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE'] },
    { key: 'process.cpu.percent', label: 'Process CPU (%)', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE'] },
    { key: 'process.cpu.total', label: 'Process CPU (total)', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE'] },
    { key: 'jvm.mem.heap.used', label: 'JVM Heap used', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE'] },
    { key: 'jvm.mem.heap.max', label: 'JVM Heap max', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE'] },
    { key: 'jvm.mem.heap.percent', label: 'JVM Heap (%)', conditionTypes: ['THRESHOLD', 'THRESHOLD_RANGE'] },
];

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

export function isStringMetric(metricKey: string): boolean {
    const all = [...API_METRICS, ...NODE_METRICS];
    const m = all.find(met => met.key === metricKey);
    return !!m && m.conditionTypes.includes('STRING') && !m.conditionTypes.includes('THRESHOLD');
}

export function getMetricsForRuleId(ruleId: AlertRuleId): AlertMetricDefinition[] {
    switch (ruleId) {
        case 'REQUEST@METRICS_AGGREGATION':
            return AGGREGATION_METRICS;
        case 'NODE_HEARTBEAT@METRICS_AGGREGATION':
            return NODE_AGGREGATION_METRICS;
        case 'NODE_HEARTBEAT@METRICS_SIMPLE_CONDITION':
        case 'NODE_HEARTBEAT@METRICS_RATE':
            return NODE_METRICS;
        default:
            return API_METRICS;
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

/** Rules with no configurable condition fields (status-change / lifecycle). */
export function isInfoOnlyRule(ruleId: AlertRuleId): boolean {
    return (
        ruleId === 'ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED' ||
        ruleId === 'NODE_LIFECYCLE@NODE_LIFECYCLE_CHANGED' ||
        ruleId === 'NODE_HEALTHCHECK@NODE_HEALTHCHECK'
    );
}
