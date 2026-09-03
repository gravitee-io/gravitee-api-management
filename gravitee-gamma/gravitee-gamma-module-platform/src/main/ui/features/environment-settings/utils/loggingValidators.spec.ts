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
    API_LOGGING_READONLY_KEYS,
    DEFAULT_COUNT_DEFAULT,
    DEFAULT_COUNT_LIMIT,
    DEFAULT_LOGGING_MAX_DURATION_MS,
    DEFAULT_PROBABILISTIC_DEFAULT,
    DEFAULT_PROBABILISTIC_LIMIT,
    DEFAULT_TEMPORAL_DEFAULT,
    DEFAULT_TEMPORAL_LIMIT,
    DEFAULT_WINDOWED_COUNT_DEFAULT,
    DEFAULT_WINDOWED_COUNT_LIMIT,
    getApiLoggingFieldErrors,
    isApiLoggingFormValid,
    isIso8601DurationAtLeastOneSecond,
    parseIso8601DurationSeconds,
    parseMaxDurationMillis,
    parseWindowedCount,
    toPortalSettingsLogging,
    type ApiLoggingFormState,
} from './loggingValidators';

const VALID: ApiLoggingFormState = {
    maxDurationMillis: '15000',
    auditEnabled: true,
    auditTrailEnabled: true,
    userDisplayed: true,
    probabilisticDefault: '0.01',
    probabilisticLimit: '0.5',
    countDefault: '100',
    countLimit: '10',
    temporalDefault: 'PT1S',
    temporalLimit: 'PT1S',
    windowedCountDefault: '1/PT10S',
    windowedCountLimit: '1/PT1S',
};

describe('loggingValidators', () => {
    it('exposes Key.key() values used in portal metadata.readonly', () => {
        expect(API_LOGGING_READONLY_KEYS.maxDuration).toBe('logging.default.max.duration');
        expect(API_LOGGING_READONLY_KEYS.windowedCountDefault).toBe('logging.messageSampling.windowed_count.default');
        expect(API_LOGGING_READONLY_KEYS.windowedCountLimit).toBe('logging.messageSampling.windowed_count.limit');
    });

    it('exposes Classic parameter defaults', () => {
        expect(DEFAULT_LOGGING_MAX_DURATION_MS).toBe(0);
        expect(DEFAULT_PROBABILISTIC_DEFAULT).toBe(0.01);
        expect(DEFAULT_PROBABILISTIC_LIMIT).toBe(0.5);
        expect(DEFAULT_COUNT_DEFAULT).toBe(100);
        expect(DEFAULT_COUNT_LIMIT).toBe(10);
        expect(DEFAULT_TEMPORAL_DEFAULT).toBe('PT1S');
        expect(DEFAULT_TEMPORAL_LIMIT).toBe('PT1S');
        expect(DEFAULT_WINDOWED_COUNT_DEFAULT).toBe('1/PT10S');
        expect(DEFAULT_WINDOWED_COUNT_LIMIT).toBe('1/PT1S');
    });

    describe('parseMaxDurationMillis', () => {
        it('accepts 0 and positive integers', () => {
            expect(parseMaxDurationMillis('0')).toBe(0);
            expect(parseMaxDurationMillis('15000')).toBe(15000);
        });

        it('rejects empty, signed, and fractional values', () => {
            expect(parseMaxDurationMillis('')).toBeNull();
            expect(parseMaxDurationMillis('-1')).toBeNull();
            expect(parseMaxDurationMillis('1.5')).toBeNull();
        });
    });

    describe('parseIso8601DurationSeconds', () => {
        it('parses Java Duration strings', () => {
            expect(parseIso8601DurationSeconds('PT1S')).toBe(1);
            expect(parseIso8601DurationSeconds('PT10S')).toBe(10);
            expect(parseIso8601DurationSeconds('PT1M')).toBe(60);
            expect(parseIso8601DurationSeconds('PT1H')).toBe(3600);
            expect(parseIso8601DurationSeconds('P1D')).toBe(86400);
            expect(parseIso8601DurationSeconds('PT1.5S')).toBe(1.5);
            expect(parseIso8601DurationSeconds('P1DT2H')).toBe(93600);
        });

        it('rejects empty, weeks, and incomplete time designs', () => {
            expect(parseIso8601DurationSeconds('')).toBeNull();
            expect(parseIso8601DurationSeconds('P')).toBeNull();
            expect(parseIso8601DurationSeconds('PT')).toBeNull();
            expect(parseIso8601DurationSeconds('P1W')).toBeNull();
            expect(parseIso8601DurationSeconds('1S')).toBeNull();
        });

        it('applies a leading minus to the whole duration', () => {
            expect(parseIso8601DurationSeconds('-PT1S')).toBe(-1);
        });
    });

    describe('isIso8601DurationAtLeastOneSecond', () => {
        it('requires a duration of at least one second', () => {
            expect(isIso8601DurationAtLeastOneSecond('PT1S')).toBe(true);
            expect(isIso8601DurationAtLeastOneSecond('PT0S')).toBe(false);
            expect(isIso8601DurationAtLeastOneSecond('PT0.5S')).toBe(false);
        });
    });

    describe('parseWindowedCount', () => {
        it('parses COUNT/DURATION', () => {
            expect(parseWindowedCount('1/PT10S')).toEqual({ count: 1, windowSeconds: 10 });
            expect(parseWindowedCount('2/PT15S')).toEqual({ count: 2, windowSeconds: 15 });
        });

        it('rejects zero count and invalid durations', () => {
            expect(parseWindowedCount('0/PT1S')).toBeNull();
            expect(parseWindowedCount('1/P1W')).toBeNull();
            expect(parseWindowedCount('1PT1S')).toBeNull();
            expect(parseWindowedCount('1/PT1S/extra')).toBeNull();
        });
    });

    describe('getApiLoggingFieldErrors', () => {
        it('accepts Classic defaults', () => {
            expect(getApiLoggingFieldErrors(VALID)).toEqual({});
            expect(isApiLoggingFormValid(VALID)).toBe(true);
        });

        it('requires probabilistic values in 0.01..1 and default <= limit', () => {
            expect(getApiLoggingFieldErrors({ ...VALID, probabilisticDefault: '' }).probabilisticDefault).toBe('Default value is required');
            expect(getApiLoggingFieldErrors({ ...VALID, probabilisticDefault: '0' }).probabilisticDefault).toContain('at least');
            expect(getApiLoggingFieldErrors({ ...VALID, probabilisticLimit: '1.1' }).probabilisticLimit).toContain('greater than');
            expect(getApiLoggingFieldErrors({ ...VALID, probabilisticDefault: '0.6', probabilisticLimit: '0.5' })).toEqual(
                expect.objectContaining({
                    probabilisticDefault: 'Default should be lower than Limit',
                    probabilisticLimit: 'Default should be lower than Limit',
                }),
            );
        });

        it('requires count integers with default >= limit', () => {
            expect(getApiLoggingFieldErrors({ ...VALID, countDefault: '1.5' }).countDefault).toBe('Default value should be an integer');
            expect(getApiLoggingFieldErrors({ ...VALID, countLimit: '0' }).countLimit).toContain('at least 1');
            expect(getApiLoggingFieldErrors({ ...VALID, countDefault: '5', countLimit: '10' })).toEqual(
                expect.objectContaining({
                    countDefault: 'Default should be greater than Limit',
                    countLimit: 'Default should be greater than Limit',
                }),
            );
        });

        it('requires temporal ISO-8601 durations with default >= limit', () => {
            expect(getApiLoggingFieldErrors({ ...VALID, temporalDefault: 'nope' }).temporalDefault).toContain('ISO-8601');
            expect(getApiLoggingFieldErrors({ ...VALID, temporalDefault: 'PT1S', temporalLimit: 'PT10S' })).toEqual(
                expect.objectContaining({
                    temporalDefault: 'Default should be greater than Limit',
                    temporalLimit: 'Default should be greater than Limit',
                }),
            );
        });

        it('requires windowed count format and a default rate no higher than limit', () => {
            expect(getApiLoggingFieldErrors({ ...VALID, windowedCountDefault: 'bad' }).windowedCountDefault).toContain('COUNT/DURATION');
            expect(getApiLoggingFieldErrors({ ...VALID, windowedCountDefault: '2/PT1S', windowedCountLimit: '1/PT1S' })).toEqual(
                expect.objectContaining({
                    windowedCountDefault: 'Default must be a lower rate than limit',
                    windowedCountLimit: 'Default must be a lower rate than limit',
                }),
            );
        });

        it('rejects a negative max duration', () => {
            expect(getApiLoggingFieldErrors({ ...VALID, maxDurationMillis: '-1' }).maxDurationMillis).toBeDefined();
        });
    });

    describe('toPortalSettingsLogging', () => {
        it('maps a valid form to the portal logging JSON shape', () => {
            expect(toPortalSettingsLogging(VALID)).toEqual({
                maxDurationMillis: 15000,
                audit: { enabled: true, trail: { enabled: true } },
                user: { displayed: true },
                messageSampling: {
                    probabilistic: { default: 0.01, limit: 0.5 },
                    count: { default: 100, limit: 10 },
                    temporal: { default: 'PT1S', limit: 'PT1S' },
                    windowedCount: { default: '1/PT10S', limit: '1/PT1S' },
                },
            });
        });

        it('returns null when the form is invalid', () => {
            expect(toPortalSettingsLogging({ ...VALID, probabilisticDefault: '9' })).toBeNull();
        });
    });
});
