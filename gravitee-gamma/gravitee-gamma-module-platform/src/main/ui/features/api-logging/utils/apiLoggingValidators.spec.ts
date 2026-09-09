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

import { isApiLoggingFormValid, validateApiLoggingForm, type ApiLoggingFormState } from './apiLoggingValidators';

const VALID_STATE: ApiLoggingFormState = {
    maxDurationMillis: '15000',
    auditEnabled: true,
    auditTrailEnabled: true,
    userDisplayed: true,
    probabilisticDefault: '0.25',
    probabilisticLimit: '0.5',
    countDefault: '200',
    countLimit: '20',
    temporalDefault: 'PT5S',
    temporalLimit: 'PT1S',
    windowedCountDefault: '1/PT10S',
    windowedCountLimit: '1/PT1S',
};

describe('apiLoggingValidators', () => {
    it('accepts a valid form state', () => {
        expect(isApiLoggingFormValid(VALID_STATE)).toBe(true);
        expect(validateApiLoggingForm(VALID_STATE)).toEqual({});
    });

    it('reports invalid probabilistic values as non-numeric', () => {
        const errors = validateApiLoggingForm({
            ...VALID_STATE,
            probabilisticDefault: 'not-a-number',
        });

        expect(errors.probabilisticDefault).toBe('Value should be a number');
    });

    it('requires probabilistic default to stay below the limit', () => {
        const errors = validateApiLoggingForm({
            ...VALID_STATE,
            probabilisticDefault: '0.6',
            probabilisticLimit: '0.5',
        });

        expect(errors.probabilisticDefault).toBe('Default should be lower than Limit');
        expect(errors.probabilisticLimit).toBe('Default should be lower than Limit');
    });

    it('requires count default to stay above the limit', () => {
        const errors = validateApiLoggingForm({
            ...VALID_STATE,
            countDefault: '10',
            countLimit: '20',
        });

        expect(errors.countDefault).toBe('Default should be greater than Limit');
        expect(errors.countLimit).toBe('Default should be greater than Limit');
    });

    it('requires temporal default to stay above the limit', () => {
        const errors = validateApiLoggingForm({
            ...VALID_STATE,
            temporalDefault: 'PT1S',
            temporalLimit: 'PT5S',
        });

        expect(errors.temporalDefault).toBe('Default should be greater than Limit');
        expect(errors.temporalLimit).toBe('Default should be greater than Limit');
    });

    it('requires windowed count default rate to stay below the limit rate', () => {
        const errors = validateApiLoggingForm({
            ...VALID_STATE,
            windowedCountDefault: '2/PT1S',
            windowedCountLimit: '1/PT1S',
        });

        expect(errors.windowedCountDefault).toBe('Default must be a lower rate than limit');
        expect(errors.windowedCountLimit).toBe('Default must be a lower rate than limit');
    });

    it('requires sampling fields to be non-empty', () => {
        const errors = validateApiLoggingForm({
            ...VALID_STATE,
            probabilisticDefault: '',
            countLimit: '   ',
            temporalDefault: '',
        });

        expect(errors.probabilisticDefault).toBe('Value is required');
        expect(errors.countLimit).toBe('Value is required');
        expect(errors.temporalDefault).toBe('Value is required');
    });

    it('enforces probabilistic bounds', () => {
        expect(validateApiLoggingForm({ ...VALID_STATE, probabilisticDefault: '0.001' }).probabilisticDefault).toBe(
            'Value should be at least 0.01',
        );
        expect(validateApiLoggingForm({ ...VALID_STATE, probabilisticLimit: '1.1' }).probabilisticLimit).toBe(
            'Value should not be greater than 1',
        );
    });

    it('rejects non-integer and sub-minimum count values', () => {
        expect(validateApiLoggingForm({ ...VALID_STATE, countDefault: '12.5' }).countDefault).toBe('Value should be an integer');
        expect(validateApiLoggingForm({ ...VALID_STATE, countLimit: '0' }).countLimit).toBe('Value should be at least 1');
    });

    it('rejects invalid temporal ISO-8601 values', () => {
        expect(validateApiLoggingForm({ ...VALID_STATE, temporalDefault: 'P1Y' }).temporalDefault).toBe(
            'Value should conform to ISO-8601 duration format',
        );
        expect(validateApiLoggingForm({ ...VALID_STATE, temporalLimit: 'not-a-duration' }).temporalLimit).toBe(
            'Value should conform to ISO-8601 duration format',
        );
    });

    it('rejects invalid max duration values', () => {
        expect(validateApiLoggingForm({ ...VALID_STATE, maxDurationMillis: '1.5' }).maxDurationMillis).toBe(
            'Max duration must be a number',
        );
        expect(validateApiLoggingForm({ ...VALID_STATE, maxDurationMillis: 'abc' }).maxDurationMillis).toBe(
            'Max duration must be a number',
        );
    });

    it('rejects malformed windowed count values', () => {
        expect(validateApiLoggingForm({ ...VALID_STATE, windowedCountDefault: '2abc/PT1S' }).windowedCountDefault).toBe(
            'The sampling value must follow this format: COUNT/DURATION, where COUNT > 0 and DURATION is in ISO-8601 format (e.g., 1/PT1S)',
        );
    });
});
