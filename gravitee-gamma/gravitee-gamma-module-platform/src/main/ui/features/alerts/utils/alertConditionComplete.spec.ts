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
import { conditionWithType, defaultFilterCondition, isAlertConditionComplete, isAlertDampeningComplete } from './alertConditionComplete';

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

    it('requires duration for time-based modes', () => {
        expect(isAlertDampeningComplete({ mode: 'STRICT_TIME', timeUnit: 'MINUTES' })).toBe(false);
        expect(isAlertDampeningComplete({ mode: 'STRICT_TIME', duration: 1, timeUnit: 'MINUTES' })).toBe(true);
        expect(isAlertDampeningComplete({ mode: 'RELAXED_TIME', trueEvaluations: 1, timeUnit: 'MINUTES' })).toBe(false);
        expect(isAlertDampeningComplete({ mode: 'RELAXED_TIME', trueEvaluations: 1, duration: 2, timeUnit: 'MINUTES' })).toBe(true);
    });
});
