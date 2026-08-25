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
import { projectionPropertyKey, propertyProjection } from './alertProjection';
import { getProjectionMetricsForRuleId, ruleSupportsProjections } from '../constants/alertConstants';

describe('alert projections', () => {
    it('exposes Classic group-by properties for request aggregation and rate', () => {
        expect(ruleSupportsProjections('REQUEST@METRICS_AGGREGATION')).toBe(true);
        const keys = getProjectionMetricsForRuleId('REQUEST@METRICS_AGGREGATION').map(m => m.key);
        expect(keys).toEqual(['response.status', 'error.key', 'tenant', 'api', 'application', 'plan']);
        expect(keys).not.toContain('response.response_time');
    });

    it('exposes node hostname and type for node aggregation', () => {
        expect(getProjectionMetricsForRuleId('NODE_HEARTBEAT@METRICS_RATE').map(m => m.key)).toEqual(['node.hostname', 'node.application']);
    });

    it('does not offer projections on simple conditions', () => {
        expect(ruleSupportsProjections('REQUEST@METRICS_SIMPLE_CONDITION')).toBe(false);
        expect(getProjectionMetricsForRuleId('REQUEST@METRICS_SIMPLE_CONDITION')).toEqual([]);
    });

    it('exposes Classic endpoint projection on health-check', () => {
        expect(ruleSupportsProjections('ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED')).toBe(true);
        expect(getProjectionMetricsForRuleId('ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED').map(m => m.key)).toEqual([
            'endpoint.name',
        ]);
    });

    it('reads the Classic PROPERTY projection key', () => {
        expect(projectionPropertyKey([propertyProjection('api')])).toBe('api');
        expect(projectionPropertyKey([])).toBeUndefined();
    });
});
