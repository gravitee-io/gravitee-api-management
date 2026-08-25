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
    collectAlertFormErrors,
    conditionWithType,
    defaultFilterCondition,
    isAlertConditionComplete,
    isAlertDampeningComplete,
    isAlertFormReady,
} from './alertConditionComplete';

describe('defaultFilterCondition', () => {
    it('uses STRING for node hostname instead of THRESHOLD', () => {
        expect(defaultFilterCondition('node.hostname')).toEqual({
            type: 'STRING',
            property: 'node.hostname',
            operator: 'EQUALS',
        });
    });

    it('uses THRESHOLD for numeric metrics', () => {
        expect(defaultFilterCondition('response.response_time')).toEqual({
            type: 'THRESHOLD',
            property: 'response.response_time',
            operator: 'GT',
        });
    });

    it('uses STRING for Classic health-check and node event filters', () => {
        expect(defaultFilterCondition('status.old')).toEqual({ type: 'STRING', property: 'status.old', operator: 'EQUALS' });
        expect(defaultFilterCondition('node.event')).toEqual({ type: 'STRING', property: 'node.event', operator: 'EQUALS' });
        expect(defaultFilterCondition('node.healthy')).toEqual({ type: 'STRING', property: 'node.healthy', operator: 'EQUALS' });
    });
});

describe('conditionWithType', () => {
    it('resets THRESHOLD operator and values when switching to STRING', () => {
        expect(conditionWithType({ type: 'THRESHOLD', property: 'error.key', operator: 'GT', threshold: 500 }, 'STRING')).toEqual({
            type: 'STRING',
            property: 'error.key',
            operator: 'EQUALS',
            threshold: undefined,
            thresholdLow: undefined,
            thresholdHigh: undefined,
            pattern: undefined,
            property2: undefined,
            multiplier: undefined,
        });
    });

    it('seeds property2 when switching to COMPARE', () => {
        expect(
            conditionWithType({ type: 'THRESHOLD', property: 'response.response_time', operator: 'GT', threshold: 500 }, 'COMPARE', 'api'),
        ).toEqual({
            type: 'COMPARE',
            property: 'response.response_time',
            operator: 'GT',
            threshold: undefined,
            thresholdLow: undefined,
            thresholdHigh: undefined,
            pattern: undefined,
            property2: 'api',
            multiplier: undefined,
        });
    });
});

describe('isAlertConditionComplete', () => {
    it('rejects a THRESHOLD condition with no threshold', () => {
        expect(isAlertConditionComplete({ type: 'THRESHOLD', property: 'response.response_time', operator: 'GT' })).toBe(false);
    });

    it('rejects MISSING_DATA with no duration', () => {
        expect(isAlertConditionComplete({ type: 'MISSING_DATA', timeUnit: 'MINUTES' })).toBe(false);
    });

    it('rejects a range where low is greater than high', () => {
        expect(
            isAlertConditionComplete({
                type: 'THRESHOLD_RANGE',
                property: 'response.response_time',
                thresholdLow: 500,
                thresholdHigh: 300,
            }),
        ).toBe(false);
    });

    it('accepts a filled THRESHOLD condition', () => {
        expect(isAlertConditionComplete({ type: 'THRESHOLD', property: 'response.response_time', operator: 'GT', threshold: 500 })).toBe(
            true,
        );
    });

    it('requires both the When comparison and the rate threshold', () => {
        expect(
            isAlertConditionComplete({
                type: 'RATE',
                comparisonType: 'THRESHOLD',
                property: 'os.cpu.percent',
                operator: 'GT',
                rateOperator: 'GT',
                rateThreshold: 10,
                duration: 1,
            }),
        ).toBe(false);
        expect(
            isAlertConditionComplete({
                type: 'RATE',
                comparisonType: 'THRESHOLD',
                property: 'os.cpu.percent',
                operator: 'GT',
                threshold: 50,
                rateOperator: 'GT',
                rateThreshold: 10,
                duration: 1,
                timeUnit: 'MINUTES',
            }),
        ).toBe(true);
    });

    it('requires duration and threshold for aggregation', () => {
        expect(
            isAlertConditionComplete({
                type: 'AGGREGATION',
                property: 'response.response_time',
                aggregationFunction: 'AVG',
                operator: 'GT',
                timeUnit: 'MINUTES',
            }),
        ).toBe(false);
        expect(
            isAlertConditionComplete({
                type: 'AGGREGATION',
                property: 'response.response_time',
                aggregationFunction: 'AVG',
                operator: 'GT',
                threshold: 500,
                duration: 1,
                timeUnit: 'MINUTES',
            }),
        ).toBe(true);
    });
});

describe('isAlertDampeningComplete', () => {
    it('requires trueEvaluations for STRICT_COUNT', () => {
        expect(isAlertDampeningComplete({ mode: 'STRICT_COUNT' })).toBe(false);
        expect(isAlertDampeningComplete({ mode: 'STRICT_COUNT', trueEvaluations: 1 })).toBe(true);
    });

    it('requires totalEvaluations for RELAXED_COUNT', () => {
        expect(isAlertDampeningComplete({ mode: 'RELAXED_COUNT', trueEvaluations: 1 })).toBe(false);
        expect(isAlertDampeningComplete({ mode: 'RELAXED_COUNT', trueEvaluations: 1, totalEvaluations: 5 })).toBe(true);
    });

    it('rejects RELAXED_COUNT when totalEvaluations is below trueEvaluations (Classic min="{{trueEvaluations}}")', () => {
        expect(isAlertDampeningComplete({ mode: 'RELAXED_COUNT', trueEvaluations: 5, totalEvaluations: 2 })).toBe(false);
        expect(isAlertDampeningComplete({ mode: 'RELAXED_COUNT', trueEvaluations: 5, totalEvaluations: 5 })).toBe(true);
    });

    it('requires duration for time-based modes', () => {
        expect(isAlertDampeningComplete({ mode: 'STRICT_TIME', timeUnit: 'MINUTES' })).toBe(false);
        expect(isAlertDampeningComplete({ mode: 'STRICT_TIME', duration: 1, timeUnit: 'MINUTES' })).toBe(true);
        expect(isAlertDampeningComplete({ mode: 'RELAXED_TIME', trueEvaluations: 1, timeUnit: 'MINUTES' })).toBe(false);
        expect(isAlertDampeningComplete({ mode: 'RELAXED_TIME', trueEvaluations: 1, duration: 2, timeUnit: 'MINUTES' })).toBe(true);
    });
});

const READY_CREATE = {
    name: 'New alert',
    isUpdate: false,
    ruleId: 'REQUEST@METRICS_SIMPLE_CONDITION' as const,
    conditions: [{ type: 'THRESHOLD' as const, property: 'response.response_time', operator: 'GT' as const, threshold: 500 }],
    filters: [] as [],
    notifications: [] as [],
    notificationsComplete: true,
    dampening: { mode: 'STRICT_COUNT' as const, trueEvaluations: 1 },
};

describe('isAlertFormReady', () => {
    it('blocks create until a rule is selected', () => {
        expect(collectAlertFormErrors({ ...READY_CREATE, ruleId: undefined }).rule).toBe('Rule is required.');
        expect(isAlertFormReady({ ...READY_CREATE, ruleId: undefined })).toBe(false);
    });

    it('blocks create when the threshold is empty', () => {
        expect(
            isAlertFormReady({
                ...READY_CREATE,
                conditions: [{ type: 'THRESHOLD', property: 'response.response_time', operator: 'GT' }],
            }),
        ).toBe(false);
    });

    it('blocks create when an added filter has no value', () => {
        expect(
            isAlertFormReady({
                ...READY_CREATE,
                filters: [{ type: 'THRESHOLD', property: 'response.response_time', operator: 'GT' }],
            }),
        ).toBe(false);
    });

    it('blocks create when aggregation duration or threshold is missing', () => {
        expect(
            isAlertFormReady({
                ...READY_CREATE,
                ruleId: 'REQUEST@METRICS_AGGREGATION',
                conditions: [
                    {
                        type: 'AGGREGATION',
                        property: 'response.response_time',
                        aggregationFunction: 'AVG',
                        operator: 'GT',
                        timeUnit: 'MINUTES',
                    },
                ],
            }),
        ).toBe(false);
    });

    it('allows info-only node lifecycle without filling When fields', () => {
        expect(
            isAlertFormReady({
                ...READY_CREATE,
                ruleId: 'NODE_LIFECYCLE@NODE_LIFECYCLE_CHANGED',
                conditions: [{ type: 'STRING', operator: 'MATCHES', property: 'node.event', pattern: 'NODE_START|NODE_STOP' }],
            }),
        ).toBe(true);
    });

    it('blocks info-only create when a filter pattern is empty', () => {
        expect(
            isAlertFormReady({
                ...READY_CREATE,
                ruleId: 'NODE_LIFECYCLE@NODE_LIFECYCLE_CHANGED',
                conditions: [{ type: 'STRING', operator: 'MATCHES', property: 'node.event', pattern: 'NODE_START|NODE_STOP' }],
                filters: [{ type: 'STRING', property: 'node.hostname', operator: 'EQUALS' }],
            }),
        ).toBe(false);
    });

    it('allows create when name, rule, condition, and dampening are filled', () => {
        expect(isAlertFormReady(READY_CREATE)).toBe(true);
    });

    it('blocks create when a notification channel or schema field is missing', () => {
        expect(
            isAlertFormReady({
                ...READY_CREATE,
                notifications: [{ type: '' }],
                notificationsComplete: false,
            }),
        ).toBe(false);
        expect(
            isAlertFormReady({
                ...READY_CREATE,
                notifications: [{ type: 'webhook-notifier' }],
                notificationsComplete: false,
            }),
        ).toBe(false);
    });
});
