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
    isAlertFormReady,
} from './alertConditionComplete';

const defaultDampening = { mode: 'STRICT_COUNT' as const, trueEvaluations: 1 };

describe('defaultFilterCondition', () => {
    it('uses STRING for string-only metrics', () => {
        expect(defaultFilterCondition('error.key')).toEqual({
            type: 'STRING',
            property: 'error.key',
            operator: 'EQUALS',
        });
    });

    it('uses THRESHOLD with GT operator for numeric metrics', () => {
        expect(defaultFilterCondition('response.response_time')).toEqual({
            type: 'THRESHOLD',
            property: 'response.response_time',
            operator: 'GT',
        });
    });
});

describe('conditionWithType', () => {
    it('resets range fields and sets GT when switching to THRESHOLD', () => {
        expect(
            conditionWithType(
                { type: 'THRESHOLD_RANGE', property: 'response.response_time', thresholdLow: 200, thresholdHigh: 500 },
                'THRESHOLD',
            ),
        ).toEqual({
            type: 'THRESHOLD',
            property: 'response.response_time',
            operator: 'GT',
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
            conditionWithType({ type: 'THRESHOLD', property: 'response.response_time', operator: 'GT', threshold: 500 }, 'COMPARE', 'plan'),
        ).toEqual({
            type: 'COMPARE',
            property: 'response.response_time',
            operator: 'GT',
            threshold: undefined,
            thresholdLow: undefined,
            thresholdHigh: undefined,
            pattern: undefined,
            property2: 'plan',
            multiplier: undefined,
        });
    });
});

describe('isAlertConditionComplete', () => {
    it('requires operator for THRESHOLD filters', () => {
        expect(isAlertConditionComplete({ type: 'THRESHOLD', property: 'response.response_time', threshold: 10 })).toBe(false);
        expect(isAlertConditionComplete({ type: 'THRESHOLD', property: 'response.response_time', operator: 'GT', threshold: 10 })).toBe(
            true,
        );
    });

    it('does not require operator for THRESHOLD_RANGE filters', () => {
        expect(
            isAlertConditionComplete({
                type: 'THRESHOLD_RANGE',
                property: 'response.response_time',
                thresholdLow: 200,
                thresholdHigh: 500,
            }),
        ).toBe(true);
    });
});

describe('collectAlertFormErrors', () => {
    it('requires a rule on create', () => {
        expect(
            collectAlertFormErrors({
                name: 'New alert',
                isUpdate: false,
                ruleId: undefined,
                conditions: [],
                filters: [],
                notifications: [],
                notificationsComplete: true,
                dampening: defaultDampening,
            }),
        ).toEqual({
            rule: 'Rule is required.',
        });
    });

    it('rejects names shorter than 3 characters', () => {
        expect(
            collectAlertFormErrors({
                name: 'AB',
                isUpdate: false,
                ruleId: undefined,
                conditions: [],
                filters: [],
                notifications: [],
                notificationsComplete: true,
                dampening: defaultDampening,
            }),
        ).toMatchObject({
            name: 'Name has to be at least 3 characters long.',
        });
    });

    it('allows endpoint health alerts without conditions', () => {
        expect(
            isAlertFormReady({
                name: 'New alert',
                isUpdate: false,
                ruleId: 'ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED',
                conditions: [],
                filters: [],
                notifications: [],
                notificationsComplete: true,
                dampening: defaultDampening,
            }),
        ).toBe(true);
    });
});
