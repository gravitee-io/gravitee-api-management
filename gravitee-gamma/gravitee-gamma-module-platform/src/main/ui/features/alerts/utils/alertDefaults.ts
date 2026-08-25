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
import type { AlertFormCondition, AlertRuleId } from '../types';

/** Classic create defaults, including hidden STRING payloads on info-only node rules. */
export function getDefaultCondition(ruleId: AlertRuleId): AlertFormCondition[] {
    switch (ruleId) {
        case 'REQUEST@METRICS_SIMPLE_CONDITION':
            return [{ type: 'THRESHOLD', property: 'response.response_time', operator: 'GT' }];
        case 'REQUEST@MISSING_DATA':
            return [{ type: 'MISSING_DATA', timeUnit: 'MINUTES' }];
        case 'REQUEST@METRICS_AGGREGATION':
            return [
                {
                    type: 'AGGREGATION',
                    property: 'response.response_time',
                    aggregationFunction: 'AVG',
                    operator: 'GT',
                    timeUnit: 'MINUTES',
                },
            ];
        case 'REQUEST@METRICS_RATE':
            return [
                {
                    type: 'RATE',
                    comparisonType: 'THRESHOLD',
                    property: 'response.status',
                    operator: 'GTE',
                    rateOperator: 'GT',
                    timeUnit: 'MINUTES',
                },
            ];
        case 'NODE_HEARTBEAT@METRICS_SIMPLE_CONDITION':
            return [{ type: 'STRING', property: 'node.hostname', operator: 'EQUALS' }];
        case 'NODE_HEARTBEAT@METRICS_AGGREGATION':
            return [
                {
                    type: 'AGGREGATION',
                    property: 'node.hostname',
                    aggregationFunction: 'AVG',
                    operator: 'GT',
                    timeUnit: 'MINUTES',
                },
            ];
        case 'NODE_HEARTBEAT@METRICS_RATE':
            return [
                {
                    type: 'RATE',
                    comparisonType: 'STRING',
                    property: 'node.hostname',
                    operator: 'EQUALS',
                    rateOperator: 'GT',
                    timeUnit: 'MINUTES',
                },
            ];
        case 'ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED':
            return [{ type: 'STRING_COMPARE', property: 'status.old', property2: 'status.new', operator: 'NOT_EQUALS' }];
        case 'NODE_LIFECYCLE@NODE_LIFECYCLE_CHANGED':
            return [
                {
                    type: 'STRING',
                    operator: 'MATCHES',
                    property: 'node.event',
                    pattern: 'NODE_START|NODE_STOP',
                },
            ];
        case 'NODE_HEALTHCHECK@NODE_HEALTHCHECK':
            return [
                {
                    type: 'STRING',
                    operator: 'MATCHES',
                    property: 'node.healthy',
                    pattern: '.*',
                },
            ];
        default:
            return [];
    }
}
