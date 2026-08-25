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
import {
    API_METRICS,
    getAlertRuleCategoriesForEnvironment,
    getAlertRulesForEnvironment,
    getCompareTargetMetrics,
    getConditionTypesForMetric,
    getFilterMetricsForRuleId,
    getMetricsForRuleId,
    isStringMetric,
    NODE_LIFECYCLE_METRICS,
    NODE_METRICS,
    sourceTypeToRuleId,
} from './alertConstants';
import { getMetricValueChoices, sanitizePatternForOperator, shouldShowStringValueSelect } from '../utils/alertMetricValues';

describe('sourceTypeToRuleId', () => {
    it('maps a known source and type to its rule id', () => {
        expect(sourceTypeToRuleId('REQUEST', 'METRICS_SIMPLE_CONDITION')).toBe('REQUEST@METRICS_SIMPLE_CONDITION');
    });

    it('does not fall back to a default rule for an unrecognized type', () => {
        expect(sourceTypeToRuleId('CUSTOM', 'UNKNOWN_TYPE')).toBeUndefined();
    });
});

describe('getConditionTypesForMetric', () => {
    it('returns no types for an unrecognized metric instead of inventing THRESHOLD', () => {
        expect(getConditionTypesForMetric('not.a.metric', API_METRICS)).toEqual([]);
    });

    it('lists other COMPARE metrics for Classic property2, excluding the left-hand property', () => {
        expect(
            getCompareTargetMetrics(getMetricsForRuleId('NODE_HEARTBEAT@METRICS_SIMPLE_CONDITION'), 'jvm.mem.heap.percent').map(m => m.key),
        ).toEqual(['os.cpu.percent', 'process.cpu.percent', 'process.cpu.total', 'jvm.mem.heap.used', 'jvm.mem.heap.max']);
    });

    it('offers Classic COMPARE on node CPU and heap', () => {
        expect(getConditionTypesForMetric('os.cpu.percent', getMetricsForRuleId('NODE_HEARTBEAT@METRICS_SIMPLE_CONDITION'))).toEqual([
            'THRESHOLD',
            'THRESHOLD_RANGE',
            'COMPARE',
        ]);
    });
});

describe('getFilterMetricsForRuleId', () => {
    function keysFor(ruleId: Parameters<typeof getFilterMetricsForRuleId>[0]) {
        return getFilterMetricsForRuleId(ruleId).map(m => m.key);
    }

    it('keeps full API metrics on request aggregation and rate, not the numeric-only condition list', () => {
        expect(keysFor('REQUEST@METRICS_AGGREGATION')).toEqual(API_METRICS.map(m => m.key));
        expect(keysFor('REQUEST@METRICS_RATE')).toEqual(API_METRICS.map(m => m.key));
        expect(keysFor('REQUEST@METRICS_AGGREGATION')).toContain('api');
        expect(getMetricsForRuleId('REQUEST@METRICS_AGGREGATION').map(m => m.key)).not.toContain('api');
    });

    it('keeps full node heartbeat metrics on node aggregation', () => {
        expect(keysFor('NODE_HEARTBEAT@METRICS_AGGREGATION')).toEqual([
            'node.hostname',
            'node.application',
            'os.cpu.percent',
            'process.cpu.percent',
            'process.cpu.total',
            'jvm.mem.heap.used',
            'jvm.mem.heap.max',
            'jvm.mem.heap.percent',
        ]);
        expect(getMetricsForRuleId('NODE_HEARTBEAT@METRICS_AGGREGATION').map(m => m.key)).toEqual([
            'node.hostname',
            'node.application',
            'os.cpu.percent',
            'process.cpu.percent',
            'process.cpu.total',
            'jvm.mem.heap.used',
            'jvm.mem.heap.max',
            'jvm.mem.heap.percent',
        ]);
    });

    it('uses Classic health-check filter metrics instead of API request metrics', () => {
        expect(keysFor('ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED')).toEqual([
            'status.old',
            'status.new',
            'endpoint.name',
            'response_time',
            'tenant',
        ]);
    });

    it('uses Classic node lifecycle and node health-check filter metrics', () => {
        expect(keysFor('NODE_LIFECYCLE@NODE_LIFECYCLE_CHANGED')).toEqual(['node.hostname', 'node.application', 'node.event']);
        expect(keysFor('NODE_HEALTHCHECK@NODE_HEALTHCHECK')).toEqual(['node.hostname', 'node.application', 'node.healthy']);
    });
});

describe('isStringMetric', () => {
    it('treats Classic health-check and node event filters as string metrics', () => {
        expect(isStringMetric('status.old')).toBe(true);
        expect(isStringMetric('endpoint.name')).toBe(true);
        expect(isStringMetric('node.event')).toBe(true);
        expect(isStringMetric('node.healthy')).toBe(true);
        expect(isStringMetric('response_time')).toBe(false);
    });
});

describe('getAlertRulesForEnvironment', () => {
    it('drops Node rules and the Node category when cloud-hosted', () => {
        expect(getAlertRulesForEnvironment(true).every(rule => rule.category !== 'Node')).toBe(true);
        expect(getAlertRuleCategoriesForEnvironment(true)).toEqual(['API metrics', 'Health-check']);
    });

    it('keeps the edited Node rule when cloud-hosted so existing alerts stay visible', () => {
        expect(getAlertRulesForEnvironment(true, 'NODE_LIFECYCLE@NODE_LIFECYCLE_CHANGED').map(rule => rule.id)).toContain(
            'NODE_LIFECYCLE@NODE_LIFECYCLE_CHANGED',
        );
        expect(getAlertRuleCategoriesForEnvironment(true, 'Node')).toEqual(['Node', 'API metrics', 'Health-check']);
    });
});

describe('getMetricValueChoices', () => {
    it('lists gateway error keys for Error Key', () => {
        expect(getMetricValueChoices(API_METRICS.find(m => m.key === 'error.key')!, {})).toContainEqual({
            value: 'API_KEY_MISSING',
            label: 'API_KEY_MISSING',
        });
    });

    it('returns undefined for Hostname so Classic Pattern is used', () => {
        expect(getMetricValueChoices(NODE_METRICS.find(m => m.key === 'node.hostname')!, {})).toBeUndefined();
    });

    it('returns an empty list for Application on environment so Value still appears', () => {
        expect(getMetricValueChoices(API_METRICS.find(m => m.key === 'application')!, {})).toEqual([]);
    });

    it('uses tenant lookups for Tenant', () => {
        expect(getMetricValueChoices(API_METRICS.find(m => m.key === 'tenant')!, { tenants: [{ value: 't1', label: 'Europe' }] })).toEqual([
            { value: 't1', label: 'Europe' },
        ]);
    });

    it('lists Start/Stop for node Event', () => {
        expect(getMetricValueChoices(NODE_LIFECYCLE_METRICS.find(m => m.key === 'node.event')!, {})).toEqual([
            { value: 'NODE_START', label: 'Start' },
            { value: 'NODE_STOP', label: 'Stop' },
        ]);
    });
});

describe('shouldShowStringValueSelect', () => {
    it('shows Value for EQUALS when a loader exists, even if the list is empty', () => {
        expect(shouldShowStringValueSelect([], 'EQUALS')).toBe(true);
    });

    it('shows Pattern for MATCHES even when values exist', () => {
        expect(shouldShowStringValueSelect([{ value: 'a', label: 'A' }], 'MATCHES')).toBe(false);
    });

    it('shows Pattern when there is no loader', () => {
        expect(shouldShowStringValueSelect(undefined, 'EQUALS')).toBe(false);
    });
});

describe('sanitizePatternForOperator', () => {
    const options = [{ value: 'API_KEY_MISSING', label: 'API_KEY_MISSING' }];

    it('clears a regex pattern that is not a valid Value option when switching to EQUALS', () => {
        expect(sanitizePatternForOperator('API.*', options, 'EQUALS')).toBeUndefined();
    });

    it('keeps the pattern when it is already a valid Value option', () => {
        expect(sanitizePatternForOperator('API_KEY_MISSING', options, 'EQUALS')).toBe('API_KEY_MISSING');
    });

    it('keeps the pattern when switching to MATCHES since Pattern is free text', () => {
        expect(sanitizePatternForOperator('API.*', options, 'MATCHES')).toBe('API.*');
    });

    it('keeps the pattern when there is no loader for the metric', () => {
        expect(sanitizePatternForOperator('some-host', undefined, 'EQUALS')).toBe('some-host');
    });
});
