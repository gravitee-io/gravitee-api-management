/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { signal } from '@angular/core';

import {
  emptyFieldKeyErrors,
  normalizedValueWarning,
  normalizeLength,
  parseBoolean,
  safePattern,
  useLengthValidation,
} from './form-helpers';

describe('form-helpers', () => {
  describe('parseBoolean', () => {
    [
      { input: true, expected: true },
      { input: false, expected: false },
      { input: 'true', expected: true },
      { input: 'false', expected: false },
      { input: undefined, expected: false },
      { input: '', expected: false },
      { input: 'TRUE', expected: true },
      { input: 'False', expected: false },
      { input: '  true  ', expected: true },
      { input: ' false ', expected: false },
    ].forEach(({ input, expected }) => {
      it(`should parse ${String(input)} as ${expected}`, () => {
        expect(parseBoolean(input)).toBe(expected);
      });
    });
  });

  describe('normalizeLength', () => {
    it('should return valid value unchanged', () => {
      const result = normalizeLength(50, 0, 100);
      expect(result.value).toBe(50);
      expect(result.warning).toBeUndefined();
    });

    [
      { input: -5, expected: 0, warningValue: -5 },
      { input: 200, expected: 100, warningValue: 200 },
    ].forEach(({ input, expected, warningValue }) => {
      it(`should clamp ${input} to ${expected}`, () => {
        const result = normalizeLength(input, 0, 100);
        expect(result.value).toBe(expected);
        expect(result.warning).toBeDefined();
        expect(result.warning?.originalValue).toBe(warningValue);
      });
    });

    it('should parse string numbers', () => {
      const result = normalizeLength('25', 0, 100);
      expect(result.value).toBe(25);
      expect(result.warning).toBeUndefined();
    });

    it('should return undefined for invalid input', () => {
      const result = normalizeLength('abc', 0, 100);
      expect(result.value).toBeUndefined();
      expect(result.warning).toBeDefined();
    });

    it('should return undefined for undefined input', () => {
      const result = normalizeLength(undefined, 0, 100);
      expect(result.value).toBeUndefined();
      expect(result.warning).toBeUndefined();
    });

    it('should use custom name in warning message', () => {
      const result = normalizeLength(-5, 0, 100, 'minLength');
      expect(result.warning?.message).toContain('minLength');
    });
  });

  describe('safePattern', () => {
    it('should compile valid pattern', () => {
      const result = safePattern('[A-Za-z]+');
      expect(result.regex).toBeInstanceOf(RegExp);
      expect(result.regex?.test('abc')).toBe(true);
      expect(result.regex?.test('123')).toBe(false);
      expect(result.error).toBeUndefined();
    });

    it('should return undefined regex for invalid pattern', () => {
      const result = safePattern('[invalid');
      expect(result.regex).toBeUndefined();
      expect(result.error).toBeDefined();
      expect(result.error?.pattern).toBe('[invalid');
    });

    [{ input: '' }, { input: '   ' }, { input: undefined }].forEach(({ input }) => {
      it(`should return undefined regex for ${String(input)} pattern`, () => {
        const result = safePattern(input);
        expect(result.regex).toBeUndefined();
        expect(result.error).toBeUndefined();
      });
    });

    it('should include component name in error message', () => {
      const result = safePattern('[invalid');
      expect(result.error?.message).toContain('invalid');
    });
  });

  describe('emptyFieldKeyErrors', () => {
    it('should return empty array for valid fieldKey', () => {
      const result = emptyFieldKeyErrors('myField');
      expect(result).toEqual([]);
    });

    it('should return error for undefined fieldKey', () => {
      const result = emptyFieldKeyErrors(undefined);
      expect(result.length).toBe(1);
      expect(result[0].code).toBe('emptyFieldKey');
      expect(result[0].severity).toBe('error');
      expect(result[0].message).toContain('missing');
    });

    it('should return error for empty string fieldKey', () => {
      const result = emptyFieldKeyErrors('');
      expect(result.length).toBe(1);
      expect(result[0].code).toBe('emptyFieldKey');
      expect(result[0].severity).toBe('error');
      expect(result[0].message).toContain('empty');
    });

    it('should return error for whitespace-only fieldKey', () => {
      const result = emptyFieldKeyErrors('   ');
      expect(result.length).toBe(1);
      expect(result[0].code).toBe('emptyFieldKey');
    });
  });

  describe('normalizedValueWarning', () => {
    it('should return undefined when no warning in result', () => {
      const result = normalizedValueWarning('minLength', { value: 10 });
      expect(result).toBeUndefined();
    });

    it('should return config error when warning present', () => {
      const result = normalizedValueWarning('minLength', {
        value: 0,
        warning: { message: 'minLength adjusted from -5 to 0', originalValue: -5 },
      });
      expect(result).toBeDefined();
      expect(result?.code).toBe('normalizedValue');
      expect(result?.severity).toBe('warning');
      expect(result?.field).toBe('minLength');
      expect(result?.value).toBe('-5');
      expect(result?.normalizedTo).toBe('0');
    });

    it('should handle undefined normalized value', () => {
      const result = normalizedValueWarning('pattern', {
        value: undefined,
        warning: { message: 'invalid pattern', originalValue: '[invalid' },
      });
      expect(result).toBeDefined();
      expect(result?.normalizedTo).toBe('undefined');
    });
  });

  describe('useLengthValidation', () => {
    it('should validate minLength', () => {
      const minLength = signal<number | string | undefined>(3);
      const maxLength = signal<number | string | undefined>(undefined);
      const value = signal<string>('ab');

      const { validationErrors: errors, minLength: minL } = useLengthValidation(minLength, maxLength, value);

      expect(minL()).toBe(3);
      expect(errors()).toContain('minLength');

      value.set('abc');
      expect(errors()).toEqual([]);
    });

    it('should validate maxLength', () => {
      const minLength = signal<number | string | undefined>(undefined);
      const maxLength = signal<number | string | undefined>(5);
      const value = signal<string>('123456');

      const { validationErrors: errors, maxLength: maxL } = useLengthValidation(minLength, maxLength, value);

      expect(maxL()).toBe(5);
      expect(errors()).toContain('maxLength');

      value.set('12345');
      expect(errors()).toEqual([]);
    });

    it('should handle cross-validation (maxLength < minLength)', () => {
      const minLength = signal<number | string | undefined>(5);
      const maxLength = signal<number | string | undefined>(3);
      const value = signal<string>('');

      const { configErrors, maxLength: maxL } = useLengthValidation(minLength, maxLength, value);

      // Should automatically adjust maxLength to be >= minLength
      expect(maxL()).toBe(5);

      const errs = configErrors();
      expect(errs.length).toBeGreaterThan(0);
      const warning = errs.find(e => e.code === 'normalizedValue' && e.field === 'maxLength');
      expect(warning).toBeDefined();
      expect(warning?.normalizedTo).toBe('5');
    });

    it('should report config errors for invalid minLength', () => {
      const minLength = signal<number | string | undefined>(-1);
      const maxLength = signal<number | string | undefined>(undefined);
      const value = signal<string>('');

      const { configErrors, minLength: minL } = useLengthValidation(minLength, maxLength, value);

      expect(minL()).toBe(0); // Normalized
      // Should have warning
      expect(configErrors().some(e => e.code === 'normalizedValue')).toBe(true);
    });
  });
});
