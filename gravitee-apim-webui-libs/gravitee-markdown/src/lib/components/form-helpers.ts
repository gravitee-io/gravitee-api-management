/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { computed, Signal } from '@angular/core';

import type { GmdConfigError, GmdFieldErrorCode } from '../models/formField';

const EL_SEPARATOR = '}:';
const EL_OPTIONS_CANDIDATE_PATTERN = /^\s*\{#.+}(?::.*)?$/;
const INVALID_EL_OPTIONS_PREFIX_PATTERN = /^\s*(?:#\{|\{(?!#))/;
const INVALID_EL_SYNTAX_MESSAGE = 'Invalid EL syntax in options. Use {#...}:option1,option2.';
const MISSING_EL_FALLBACK_MESSAGE =
  "EL expression in options requires fallback options as a comma-separated list (CSV) after ':', e.g. {#api.metadata['key']}:option1,option2";

/**
 * Returns true if the options string is an EL expression (starts with `{#`).
 *
 * @example
 * isElExpression("{#api.metadata['key']}:A,B") → true
 * isElExpression("A,B,C") → false
 */
export function isElExpression(options: string): boolean {
  return EL_OPTIONS_CANDIDATE_PATTERN.test(options);
}

/**
 * Parses the fallback option list from an EL options string.
 * Returns the comma-split values after `}:`, or an empty array if no separator is found.
 *
 * @example
 * parseElFallback("{#api.metadata['key']}:A,B,C") → ["A", "B", "C"]
 * parseElFallback("{#api.metadata['key']}") → []
 */
export function parseElFallback(options: string): string[] {
  const sepIdx = options.indexOf(EL_SEPARATOR);
  if (sepIdx === -1) return [];
  return options
    .slice(sepIdx + EL_SEPARATOR.length)
    .split(',')
    .map(s => s.trim())
    .filter(s => s.length > 0);
}

const DEFAULT_MIN_LENGTH = 0;

export const GMD_INPUT_HARD_MAX_LENGTH = 256;
export const GMD_TEXTAREA_HARD_MAX_LENGTH = 1024;
export const GMD_SUBSCRIPTION_FORM_MAX_FIELDS = 25;

const DEFAULT_ROWS = 4;
const MIN_ROWS = 1;
const MAX_ROWS = 100;

/**
 * Result of normalization with optional warning
 */
export interface NormalizeResult<T> {
  value: T;
  warning?: {
    message: string;
    originalValue: unknown;
  };
}

/**
 * Parses boolean-like values from HTML attributes.
 * Handles both actual booleans and string representations.
 *
 * @param v - Value to parse (boolean, string, or undefined)
 * @returns boolean - true if value is truthy boolean representation
 *
 * @example
 * parseBoolean(true) → true
 * parseBoolean("true") → true
 * parseBoolean("false") → false
 * parseBoolean(undefined) → false
 */
export function parseBoolean(v: boolean | string | undefined): boolean {
  if (v === undefined || v === null) return false;
  if (typeof v === 'boolean') return v;

  const str = String(v).trim().toLowerCase();
  return str === 'true';
}

/**
 * Normalizes numeric length constraints with bounds checking.
 * Returns value and optional warning if adjustment was made.
 *
 * @param v - Value to normalize (number, string, or undefined)
 * @param max - Upper bound (required; e.g. {@link GMD_INPUT_HARD_MAX_LENGTH})
 * @param min - Lower bound (default: 0)
 * @param propertyName - Property name for warning messages (default: 'value')
 * @returns NormalizeResult with normalized value and optional warning
 *
 * @example
 * normalizeLength(-5, 100, 0, 'minLength')
 * // → { value: 0, warning: { message: 'minLength adjusted from -5 to 0', originalValue: -5 } }
 */
export function normalizeLength(
  v: number | string | undefined,
  max: number,
  min: number = DEFAULT_MIN_LENGTH,
  propertyName: string = 'value',
): NormalizeResult<number | undefined> {
  if (v === undefined || v === null) {
    return { value: undefined };
  }

  const num = typeof v === 'number' ? v : Number(String(v).trim());

  if (!Number.isFinite(num)) {
    const message = `${propertyName} "${v}" is not a valid number`;
    return {
      value: undefined,
      warning: { message, originalValue: v },
    };
  }

  const clamped = Math.max(min, Math.min(num, max));

  if (clamped !== num) {
    const message = `${propertyName} adjusted from ${num} to ${clamped} (valid range: ${min}-${max})`;
    return {
      value: clamped,
      warning: { message, originalValue: num },
    };
  }

  return { value: clamped };
}

/**
 * Creates normalized signals for numeric length inputs.
 *
 * @param max - Upper bound for the parsed attribute (field-type hard cap)
 */
export function normalizedLengthInput(
  value: () => number | string | undefined,
  max: number,
  min: number = DEFAULT_MIN_LENGTH,
  name: string = 'value',
): { result: Signal<NormalizeResult<number | undefined>>; value: Signal<number | undefined> } {
  const result = computed(() => normalizeLength(value(), max, min, name));
  const normalizedValue = computed(() => result().value);
  return { result, value: normalizedValue };
}

/**
 * Builds a config error when an EL expression in options is missing its required fallback list.
 */
export function elFallbackErrors(options: string | undefined): GmdConfigError[] {
  if (!options) {
    return [];
  }

  if (INVALID_EL_OPTIONS_PREFIX_PATTERN.test(options)) {
    return [
      {
        code: 'invalidElSyntax',
        message: INVALID_EL_SYNTAX_MESSAGE,
        severity: 'error',
        field: 'options',
        value: options,
      },
    ];
  }

  if (isElExpression(options) && parseElFallback(options).length === 0) {
    return [
      {
        code: 'missingElFallback',
        message: MISSING_EL_FALLBACK_MESSAGE,
        severity: 'error',
        field: 'options',
        value: options,
      },
    ];
  }
  return [];
}

/**
 * Builds config errors for missing or empty fieldKey.
 */
export function emptyFieldKeyErrors(fieldKey: string | undefined): GmdConfigError[] {
  if (fieldKey && fieldKey.trim().length > 0) return [];
  return [
    {
      code: 'emptyFieldKey',
      message: fieldKey === undefined ? 'fieldKey is missing - field will not emit data' : 'fieldKey is empty - field will not emit data',
      severity: 'error',
      field: 'fieldKey',
      value: fieldKey ?? '(undefined)',
    },
  ];
}

/**
 * Converts a NormalizeResult warning to a config error.
 */
export function normalizedValueWarning<T>(field: string, result: NormalizeResult<T>): GmdConfigError | undefined {
  if (!result.warning) return;
  return {
    code: 'normalizedValue',
    message: result.warning.message,
    severity: 'warning',
    field,
    value: String(result.warning.originalValue),
    normalizedTo: String(result.value),
  };
}

/**
 * Safely compiles a RegExp pattern with error handling.
 * Returns undefined for invalid patterns.
 *
 * @param pattern - Pattern string to compile
 * @returns Object with compiled regex or error details
 */
export function safePattern(pattern: string | undefined): { regex?: RegExp; error?: { message: string; pattern: string } } {
  if (!pattern || pattern.trim().length === 0) {
    return { regex: undefined };
  }

  try {
    return { regex: new RegExp(pattern) };
  } catch (e) {
    const message = `Invalid regex pattern "${pattern}": ${e instanceof Error ? e.message : 'Unknown error'}`;
    return {
      regex: undefined,
      error: { message, pattern },
    };
  }
}

/**
 * Creates normalized signals for textarea rows input.
 */
export function normalizedRowsInput(value: () => number | string | undefined): {
  result: Signal<NormalizeResult<number>>;
  value: Signal<number>;
} {
  const result = computed(() => {
    const v = value();
    if (v === undefined || v === null) {
      return { value: DEFAULT_ROWS };
    }

    const num = typeof v === 'number' ? v : parseInt(String(v), 10);

    if (!Number.isFinite(num)) {
      const message = `rows "${v}" is not a valid number, using default (${DEFAULT_ROWS})`;
      return {
        value: DEFAULT_ROWS,
        warning: { message, originalValue: v },
      };
    }

    const clamped = Math.max(MIN_ROWS, Math.min(num, MAX_ROWS));

    if (clamped !== num) {
      const message = `rows adjusted from ${num} to ${clamped} (valid range: ${MIN_ROWS}-${MAX_ROWS})`;
      return {
        value: clamped,
        warning: { message, originalValue: num },
      };
    }

    return { value: clamped };
  });
  const normalizedValue = computed(() => result().value);
  return { result, value: normalizedValue };
}

/**
 * Composition function for shared length validation logic.
 * Handles normalization, cross-field validation, and error generation.
 *
 * @param minLength - Signal for minimum length input
 * @param maxLength - Signal for maximum length input
 * @param value - Signal for current field value
 * @param options.hardMaxLength - Backend cap for this field type (input vs textarea); also used when maxLength attr is omitted
 */
export function useLengthValidation(
  minLength: Signal<number | string | undefined>,
  maxLength: Signal<number | string | undefined>,
  value: Signal<string>,
  options: { hardMaxLength: number },
) {
  const { hardMaxLength } = options;

  const minLengthInput = normalizedLengthInput(minLength, hardMaxLength, DEFAULT_MIN_LENGTH, 'minLength');
  const minLengthVM = minLengthInput.value;

  const maxLengthInput = normalizedLengthInput(maxLength, hardMaxLength, DEFAULT_MIN_LENGTH, 'maxLength');
  const maxLengthVM = computed(() => {
    const result = maxLengthInput.result();
    let v = result.value;

    // Cross-field logic: maxLength must be >= minLength
    const min = minLengthVM();
    if (v !== undefined && min !== undefined && v < min) {
      v = min;
    }

    // Backend always enforces a max (forInput / forTextarea); when the author omits maxLength, use the hard cap
    if (v === undefined) {
      return hardMaxLength;
    }

    return v;
  });

  const configErrors = computed<GmdConfigError[]>(() => {
    const errors: GmdConfigError[] = [];

    // Check minLength normalization
    const minResult = minLengthInput.result();
    const minWarning = normalizedValueWarning('minLength', minResult);
    if (minWarning) errors.push(minWarning);

    // Check maxLength normalization
    const maxResult = maxLengthInput.result();
    const maxValue = maxLengthVM();
    const minValue = minLengthVM();

    if (maxResult.value !== undefined && minValue !== undefined && maxResult.value < minValue) {
      errors.push({
        code: 'normalizedValue',
        message: `maxLength adjusted from ${maxResult.value} to ${minValue} (must be >= minLength)`,
        severity: 'warning',
        field: 'maxLength',
        value: String(maxResult.value),
        normalizedTo: String(maxValue),
      });
    } else {
      const maxWarning = normalizedValueWarning('maxLength', maxResult);
      if (maxWarning) errors.push(maxWarning);
    }

    return errors;
  });

  const errors = computed<GmdFieldErrorCode[]>(() => {
    const v = value();
    const errs: GmdFieldErrorCode[] = [];

    const minL = minLengthVM();
    if (minL !== undefined && v.length < minL) errs.push('minLength');

    const maxL = maxLengthVM();
    if (maxL !== undefined && v.length > maxL) errs.push('maxLength');

    return errs;
  });

  return {
    minLength: minLengthVM,
    maxLength: maxLengthVM,
    configErrors,
    validationErrors: errors,
  };
}
