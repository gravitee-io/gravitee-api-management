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

import type { PortalSettingsLogging } from '../../security-plan-types/services/portalSettings';

/** Matches `Key` parameter keys stored in portal settings metadata.readonly. */
export const API_LOGGING_READONLY_KEYS = {
    maxDuration: 'logging.default.max.duration',
    auditEnabled: 'logging.audit.enabled',
    auditTrailEnabled: 'logging.audit.trail.enabled',
    userDisplayed: 'logging.user.displayed',
    probabilisticDefault: 'logging.messageSampling.probabilistic.default',
    probabilisticLimit: 'logging.messageSampling.probabilistic.limit',
    countDefault: 'logging.messageSampling.count.default',
    countLimit: 'logging.messageSampling.count.limit',
    temporalDefault: 'logging.messageSampling.temporal.default',
    temporalLimit: 'logging.messageSampling.temporal.limit',
    windowedCountDefault: 'logging.messageSampling.windowed_count.default',
    windowedCountLimit: 'logging.messageSampling.windowed_count.limit',
} as const;

export const DEFAULT_LOGGING_MAX_DURATION_MS = 0;
export const DEFAULT_PROBABILISTIC_DEFAULT = 0.01;
export const DEFAULT_PROBABILISTIC_LIMIT = 0.5;
export const DEFAULT_COUNT_DEFAULT = 100;
export const DEFAULT_COUNT_LIMIT = 10;
export const DEFAULT_TEMPORAL_DEFAULT = 'PT1S';
export const DEFAULT_TEMPORAL_LIMIT = 'PT1S';
export const DEFAULT_WINDOWED_COUNT_DEFAULT = '1/PT10S';
export const DEFAULT_WINDOWED_COUNT_LIMIT = '1/PT1S';

const PROBABILISTIC_MIN = 0.01;
const PROBABILISTIC_MAX = 1;
const WINDOWED_FORMAT_ERROR =
    'The sampling value must follow this format: COUNT/DURATION, where COUNT > 0 and DURATION is in ISO-8601 format (e.g., 1/PT1S)';

/** java.time.Duration.parse (ISO-8601 PnDTnHnMn.nS). Weeks are rejected, matching backend validation. */
const JAVA_DURATION = /^([-+]?)P(?:([-+]?\d+)D)?(T(?:([-+]?\d+)H)?(?:([-+]?\d+)M)?(?:([-+]?\d+)(?:[.,](\d{0,9}))?S)?)?$/i;

export interface ApiLoggingFormState {
    maxDurationMillis: string;
    auditEnabled: boolean;
    auditTrailEnabled: boolean;
    userDisplayed: boolean;
    probabilisticDefault: string;
    probabilisticLimit: string;
    countDefault: string;
    countLimit: string;
    temporalDefault: string;
    temporalLimit: string;
    windowedCountDefault: string;
    windowedCountLimit: string;
}

export interface ApiLoggingFieldErrors {
    maxDurationMillis?: string;
    probabilisticDefault?: string;
    probabilisticLimit?: string;
    countDefault?: string;
    countLimit?: string;
    temporalDefault?: string;
    temporalLimit?: string;
    windowedCountDefault?: string;
    windowedCountLimit?: string;
}

export interface ApiLoggingFieldReadonly {
    maxDurationMillis?: boolean;
    auditEnabled?: boolean;
    auditTrailEnabled?: boolean;
    userDisplayed?: boolean;
    probabilisticDefault?: boolean;
    probabilisticLimit?: boolean;
    countDefault?: boolean;
    countLimit?: boolean;
    temporalDefault?: boolean;
    temporalLimit?: boolean;
    windowedCountDefault?: boolean;
    windowedCountLimit?: boolean;
}

export function parseMaxDurationMillis(value: string): number | null {
    if (!/^\d+$/.test(value.trim())) return null;
    const parsed = Number(value);
    return Number.isSafeInteger(parsed) ? parsed : null;
}

/**
 * Seconds represented by a Java Duration.parse-compatible string, including fractions.
 * Returns null when the value is not a valid duration.
 */
export function parseIso8601DurationSeconds(value: string): number | null {
    const trimmed = value.trim();
    const match = trimmed.match(JAVA_DURATION);
    if (!match) return null;

    const day = match[2];
    const time = match[3];
    const hour = match[4];
    const minute = match[5];
    const second = match[6];
    const fractionDigits = match[7];

    if (day === undefined && time === undefined) return null;
    if (time !== undefined && hour === undefined && minute === undefined && second === undefined) return null;

    const fraction = fractionDigits ? Number(`0.${fractionDigits}`) : 0;
    const total = Number(day ?? 0) * 86400 + Number(hour ?? 0) * 3600 + Number(minute ?? 0) * 60 + Number(second ?? 0) + fraction;
    return match[1] === '-' ? -total : total;
}

export function isIso8601DurationAtLeastOneSecond(value: string): boolean {
    const seconds = parseIso8601DurationSeconds(value);
    return seconds !== null && seconds >= 1;
}

export interface WindowedCountValue {
    count: number;
    windowSeconds: number;
}

export function parseWindowedCount(value: string): WindowedCountValue | null {
    const parts = value.trim().split('/');
    if (parts.length !== 2) return null;
    const countPart = parts[0] ?? '';
    const durationPart = parts[1] ?? '';
    if (!/^\d+$/.test(countPart)) return null;
    const count = Number(countPart);
    if (count < 1) return null;
    const windowSeconds = parseIso8601DurationSeconds(durationPart);
    if (windowSeconds === null || windowSeconds <= 0) return null;
    return { count, windowSeconds };
}

function parseProbabilistic(value: string): number | null {
    if (value.trim() === '') return null;
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) return null;
    if (parsed < PROBABILISTIC_MIN || parsed > PROBABILISTIC_MAX) return null;
    return parsed;
}

function parseCount(value: string): number | null {
    if (!/^\d+$/.test(value.trim())) return null;
    const parsed = Number(value);
    return parsed >= 1 ? parsed : null;
}

function probabilisticFieldError(value: string, label: 'Default' | 'Limit'): string | undefined {
    if (value.trim() === '') return `${label} value is required`;
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) return `${label} value is required`;
    if (parsed < PROBABILISTIC_MIN) return `${label} value should be at least ${PROBABILISTIC_MIN}`;
    if (parsed > PROBABILISTIC_MAX) return `${label} value should not be greater than ${PROBABILISTIC_MAX}`;
    return undefined;
}

function countFieldError(value: string, label: 'Default' | 'Limit'): string | undefined {
    if (value.trim() === '') return `${label} value is required`;
    if (!/^\d+$/.test(value.trim())) return `${label} value should be an integer`;
    if (Number(value) < 1) return `${label} value should be at least 1`;
    return undefined;
}

function temporalFieldError(value: string, label: 'Default' | 'Limit'): string | undefined {
    if (value.trim() === '') return `${label} value is required`;
    if (!isIso8601DurationAtLeastOneSecond(value)) return `${label} value should conform to ISO-8601 duration format`;
    return undefined;
}

function windowedCountFieldError(value: string, label: 'Default' | 'Limit'): string | undefined {
    if (value.trim() === '') return `${label} value is required`;
    if (!parseWindowedCount(value)) return WINDOWED_FORMAT_ERROR;
    return undefined;
}

export function getApiLoggingFieldErrors(state: ApiLoggingFormState): ApiLoggingFieldErrors {
    const errors: ApiLoggingFieldErrors = {};

    if (parseMaxDurationMillis(state.maxDurationMillis) === null) {
        errors.maxDurationMillis = 'Enter a duration of 0 or more milliseconds.';
    }

    const probabilisticDefaultError = probabilisticFieldError(state.probabilisticDefault, 'Default');
    const probabilisticLimitError = probabilisticFieldError(state.probabilisticLimit, 'Limit');
    if (probabilisticDefaultError) errors.probabilisticDefault = probabilisticDefaultError;
    if (probabilisticLimitError) errors.probabilisticLimit = probabilisticLimitError;
    const probabilisticDefault = parseProbabilistic(state.probabilisticDefault);
    const probabilisticLimit = parseProbabilistic(state.probabilisticLimit);
    if (probabilisticDefault !== null && probabilisticLimit !== null && probabilisticDefault > probabilisticLimit) {
        const message = 'Default should be lower than Limit';
        errors.probabilisticDefault = message;
        errors.probabilisticLimit = message;
    }

    const countDefaultError = countFieldError(state.countDefault, 'Default');
    const countLimitError = countFieldError(state.countLimit, 'Limit');
    if (countDefaultError) errors.countDefault = countDefaultError;
    if (countLimitError) errors.countLimit = countLimitError;
    const countDefault = parseCount(state.countDefault);
    const countLimit = parseCount(state.countLimit);
    if (countDefault !== null && countLimit !== null && countDefault < countLimit) {
        const message = 'Default should be greater than Limit';
        errors.countDefault = message;
        errors.countLimit = message;
    }

    const temporalDefaultError = temporalFieldError(state.temporalDefault, 'Default');
    const temporalLimitError = temporalFieldError(state.temporalLimit, 'Limit');
    if (temporalDefaultError) errors.temporalDefault = temporalDefaultError;
    if (temporalLimitError) errors.temporalLimit = temporalLimitError;
    const temporalDefault = parseIso8601DurationSeconds(state.temporalDefault);
    const temporalLimit = parseIso8601DurationSeconds(state.temporalLimit);
    if (temporalDefault !== null && temporalLimit !== null && temporalDefault >= 1 && temporalLimit >= 1 && temporalDefault < temporalLimit) {
        const message = 'Default should be greater than Limit';
        errors.temporalDefault = message;
        errors.temporalLimit = message;
    }

    const windowedDefaultError = windowedCountFieldError(state.windowedCountDefault, 'Default');
    const windowedLimitError = windowedCountFieldError(state.windowedCountLimit, 'Limit');
    if (windowedDefaultError) errors.windowedCountDefault = windowedDefaultError;
    if (windowedLimitError) errors.windowedCountLimit = windowedLimitError;
    const windowedDefault = parseWindowedCount(state.windowedCountDefault);
    const windowedLimit = parseWindowedCount(state.windowedCountLimit);
    if (windowedDefault && windowedLimit) {
        const defaultRate = windowedDefault.count / windowedDefault.windowSeconds;
        const limitRate = windowedLimit.count / windowedLimit.windowSeconds;
        if (defaultRate > limitRate) {
            const message = 'Default must be a lower rate than limit';
            errors.windowedCountDefault = message;
            errors.windowedCountLimit = message;
        }
    }

    return errors;
}

export function isApiLoggingFormValid(state: ApiLoggingFormState): boolean {
    return Object.keys(getApiLoggingFieldErrors(state)).length === 0;
}

export function toPortalSettingsLogging(state: ApiLoggingFormState): PortalSettingsLogging | null {
    if (!isApiLoggingFormValid(state)) return null;
    return {
        maxDurationMillis: parseMaxDurationMillis(state.maxDurationMillis) ?? 0,
        audit: {
            enabled: state.auditEnabled,
            trail: { enabled: state.auditTrailEnabled },
        },
        user: { displayed: state.userDisplayed },
        messageSampling: {
            probabilistic: {
                default: Number(state.probabilisticDefault),
                limit: Number(state.probabilisticLimit),
            },
            count: {
                default: Number(state.countDefault),
                limit: Number(state.countLimit),
            },
            temporal: {
                default: state.temporalDefault.trim(),
                limit: state.temporalLimit.trim(),
            },
            windowedCount: {
                default: state.windowedCountDefault.trim(),
                limit: state.windowedCountLimit.trim(),
            },
        },
    };
}
