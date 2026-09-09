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

import { isValidIso8601Duration, parseIso8601DurationSeconds } from './iso8601Duration';
import { WindowedCount, WindowedCountFormatError } from './windowedCount';

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

export type ApiLoggingFieldKey = keyof ApiLoggingFormState;

export type ApiLoggingFieldErrors = Partial<Record<ApiLoggingFieldKey, string>>;

const PROBABILISTIC_MIN = 0.01;
const PROBABILISTIC_MAX = 1;
const COUNT_MIN = 1;
const WINDOWED_COUNT_FORMAT_ERROR =
    'The sampling value must follow this format: COUNT/DURATION, where COUNT > 0 and DURATION is in ISO-8601 format (e.g., 1/PT1S)';

function requiredError(value: string): string | undefined {
    return value.trim() === '' ? 'Value is required' : undefined;
}

function parseProbabilistic(value: string): number | null {
    if (value.trim() === '') {
        return null;
    }
    const parsed = Number(value);
    return Number.isNaN(parsed) ? null : parsed;
}

function parseCount(value: string): { parsed: number | null; integerError?: string } {
    if (value.trim() === '') {
        return { parsed: null };
    }
    if (!/^\d+$/.test(value.trim())) {
        return { parsed: null, integerError: 'Value should be an integer' };
    }
    return { parsed: Number(value) };
}

function validateProbabilisticField(value: string): string | undefined {
    const required = requiredError(value);
    if (required) {
        return required;
    }

    const parsed = parseProbabilistic(value);
    if (parsed === null) {
        return 'Value should be a number';
    }
    if (parsed < PROBABILISTIC_MIN) {
        return `Value should be at least ${PROBABILISTIC_MIN}`;
    }
    if (parsed > PROBABILISTIC_MAX) {
        return `Value should not be greater than ${PROBABILISTIC_MAX}`;
    }

    return undefined;
}

function validateCountField(value: string): string | undefined {
    const required = requiredError(value);
    if (required) {
        return required;
    }

    const { parsed, integerError } = parseCount(value);
    if (integerError) {
        return integerError;
    }
    if (parsed !== null && parsed < COUNT_MIN) {
        return `Value should be at least ${COUNT_MIN}`;
    }

    return undefined;
}

function validateTemporalField(value: string): string | undefined {
    const required = requiredError(value);
    if (required) {
        return required;
    }
    if (!isValidIso8601Duration(value)) {
        return 'Value should conform to ISO-8601 duration format';
    }
    return undefined;
}

function validateWindowedCountField(value: string): string | undefined {
    const required = requiredError(value);
    if (required) {
        return required;
    }

    try {
        const parsed = WindowedCount.parse(value);
        if (!parsed.isValid()) {
            return WINDOWED_COUNT_FORMAT_ERROR;
        }
    } catch (error) {
        if (error instanceof WindowedCountFormatError) {
            return WINDOWED_COUNT_FORMAT_ERROR;
        }
        return WINDOWED_COUNT_FORMAT_ERROR;
    }

    return undefined;
}

function compareProbabilistic(defaultValue: string, limitValue: string): string | undefined {
    const defaultParsed = parseProbabilistic(defaultValue);
    const limitParsed = parseProbabilistic(limitValue);
    if (defaultParsed !== null && limitParsed !== null && defaultParsed > limitParsed) {
        return 'Default should be lower than Limit';
    }
    return undefined;
}

function compareCount(defaultValue: string, limitValue: string): string | undefined {
    const defaultParsed = parseCount(defaultValue).parsed;
    const limitParsed = parseCount(limitValue).parsed;
    if (defaultParsed !== null && limitParsed !== null && defaultParsed < limitParsed) {
        return 'Default should be greater than Limit';
    }
    return undefined;
}

function compareTemporal(defaultValue: string, limitValue: string): string | undefined {
    const defaultSeconds = parseIso8601DurationSeconds(defaultValue);
    const limitSeconds = parseIso8601DurationSeconds(limitValue);
    if (defaultSeconds !== null && limitSeconds !== null && defaultSeconds < limitSeconds) {
        return 'Default should be greater than Limit';
    }
    return undefined;
}

function compareWindowedCount(defaultValue: string, limitValue: string): string | undefined {
    try {
        const defaultWindowed = WindowedCount.parse(defaultValue);
        const limitWindowed = WindowedCount.parse(limitValue);
        if (defaultWindowed.rate() > limitWindowed.rate()) {
            return 'Default must be a lower rate than limit';
        }
    } catch (error) {
        if (!(error instanceof WindowedCountFormatError)) {
            return 'Default must be a lower rate than limit';
        }
    }
    return undefined;
}

export function validateApiLoggingForm(state: ApiLoggingFormState): ApiLoggingFieldErrors {
    const errors: ApiLoggingFieldErrors = {};

    if (state.maxDurationMillis.trim() !== '' && !/^-?\d+$/.test(state.maxDurationMillis.trim())) {
        errors.maxDurationMillis = 'Max duration must be a number';
    }

    const probabilisticDefaultError = validateProbabilisticField(state.probabilisticDefault);
    const probabilisticLimitError = validateProbabilisticField(state.probabilisticLimit);
    const probabilisticCompareError = compareProbabilistic(state.probabilisticDefault, state.probabilisticLimit);

    if (probabilisticDefaultError) {
        errors.probabilisticDefault = probabilisticDefaultError;
    } else if (probabilisticCompareError) {
        errors.probabilisticDefault = probabilisticCompareError;
    }

    if (probabilisticLimitError) {
        errors.probabilisticLimit = probabilisticLimitError;
    } else if (probabilisticCompareError) {
        errors.probabilisticLimit = probabilisticCompareError;
    }

    const countDefaultError = validateCountField(state.countDefault);
    const countLimitError = validateCountField(state.countLimit);
    const countCompareError = compareCount(state.countDefault, state.countLimit);

    if (countDefaultError) {
        errors.countDefault = countDefaultError;
    } else if (countCompareError) {
        errors.countDefault = countCompareError;
    }

    if (countLimitError) {
        errors.countLimit = countLimitError;
    } else if (countCompareError) {
        errors.countLimit = countCompareError;
    }

    const temporalDefaultError = validateTemporalField(state.temporalDefault);
    const temporalLimitError = validateTemporalField(state.temporalLimit);
    const temporalCompareError = compareTemporal(state.temporalDefault, state.temporalLimit);

    if (temporalDefaultError) {
        errors.temporalDefault = temporalDefaultError;
    } else if (temporalCompareError) {
        errors.temporalDefault = temporalCompareError;
    }

    if (temporalLimitError) {
        errors.temporalLimit = temporalLimitError;
    } else if (temporalCompareError) {
        errors.temporalLimit = temporalCompareError;
    }

    const windowedDefaultError = validateWindowedCountField(state.windowedCountDefault);
    const windowedLimitError = validateWindowedCountField(state.windowedCountLimit);
    const windowedCompareError = compareWindowedCount(state.windowedCountDefault, state.windowedCountLimit);

    if (windowedDefaultError) {
        errors.windowedCountDefault = windowedDefaultError;
    } else if (windowedCompareError) {
        errors.windowedCountDefault = windowedCompareError;
    }

    if (windowedLimitError) {
        errors.windowedCountLimit = windowedLimitError;
    } else if (windowedCompareError) {
        errors.windowedCountLimit = windowedCompareError;
    }

    return errors;
}

export function isApiLoggingFormValid(state: ApiLoggingFormState): boolean {
    return Object.keys(validateApiLoggingForm(state)).length === 0;
}

export function toApiLoggingSettingsPayload(state: ApiLoggingFormState) {
    return {
        maxDurationMillis: state.maxDurationMillis.trim() === '' ? 0 : Number(state.maxDurationMillis),
        audit: {
            enabled: state.auditEnabled,
            trail: {
                enabled: state.auditTrailEnabled,
            },
        },
        user: {
            displayed: state.userDisplayed,
        },
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
