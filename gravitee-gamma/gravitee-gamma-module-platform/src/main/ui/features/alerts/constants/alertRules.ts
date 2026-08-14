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

/**
 * Environment-scoped alert rules from classic Console (`entities/alerts/rule.metrics.ts`).
 * Used for list rule labels and the educational empty state.
 */
export type AlertRuleCategory = 'API metrics' | 'Health-check' | 'Node';

export interface AlertRuleDefinition {
    id: string;
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

export function getAlertRuleLabel(source: string, type: string): string {
    const ruleId = `${source}@${type}`;
    return ALERT_RULES.find(rule => rule.id === ruleId)?.description ?? `${source} / ${type}`;
}
