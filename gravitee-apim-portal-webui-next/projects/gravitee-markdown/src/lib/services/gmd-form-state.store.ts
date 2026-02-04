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
import { computed, Injectable, InjectionToken, signal } from '@angular/core';
import { isEqual } from 'lodash';

import { GmdConfigError, GmdFieldState } from '../models/formField';

/**
 * Store for managing form field states across dynamically rendered GMD components.
 * Uses Angular signals for reactive state management.
 * Provides API for form validation and configuration errors.
 */
@Injectable()
export class GmdFormStateStore {
  private readonly _fieldsMap = signal<Map<string, GmdFieldState>>(new Map());

  /**
   * Readonly signal exposing the current form state.
   * Consumers can read but not modify directly.
   */
  readonly fieldsMap = this._fieldsMap.asReadonly();

  /**
   * Computed: Whether all required fields are valid.
   */
  readonly formValid = computed(() => {
    const fields = this.fieldsMap();
    if (fields.size === 0) return true; // No fields means valid

    for (const state of fields.values()) {
      if (state.required && !state.valid) {
        return false;
      }
    }
    return true;
  });

  /**
   * Computed: Count of required fields.
   */
  readonly requiredFieldsCount = computed(() => {
    const fields = this.fieldsMap();
    let count = 0;
    for (const state of fields.values()) {
      if (state.required) count++;
    }
    return count;
  });

  /**
   * Computed: Count of valid required fields.
   */
  readonly validRequiredFieldsCount = computed(() => {
    const fields = this.fieldsMap();
    let count = 0;
    for (const state of fields.values()) {
      if (state.required && state.valid) count++;
    }
    return count;
  });

  /**
   * Computed: List of invalid fields with their errors.
   */
  readonly invalidFields = computed(() => {
    const fields = this.fieldsMap();
    const invalid: Array<{ id: string; fieldKey: string; errors: string[] }> = [];
    for (const state of fields.values()) {
      if (!state.valid && state.validationErrors.length > 0) {
        invalid.push({ id: state.id, fieldKey: state.fieldKey, errors: state.validationErrors });
      }
    }
    return invalid;
  });

  /**
   * Computed: List of field values (key-value pairs).
   */
  readonly fieldValues = computed(() => {
    const fields = this.fieldsMap();
    const values: Array<{ id: string; fieldKey: string; value: string }> = [];
    for (const state of fields.values()) {
      values.push({ id: state.id, fieldKey: state.fieldKey, value: state.value });
    }
    return values;
  });

  /**
   * Computed: Detect duplicate fieldKeys.
   */
  readonly duplicateFieldKeys = computed(() => {
    const fields = this.fieldsMap();
    const fieldKeyCount = new Map<string, number>();

    // Count occurrences of each actual fieldKey (ignore empty fieldKeys)
    for (const state of fields.values()) {
      if (state.fieldKey && state.fieldKey.trim().length > 0) {
        fieldKeyCount.set(state.fieldKey, (fieldKeyCount.get(state.fieldKey) || 0) + 1);
      }
    }

    return Array.from(fieldKeyCount.entries())
      .filter(([_, count]) => count > 1)
      .map(([key, count]) => ({ key, count }));
  });

  /**
   * Computed: All configuration errors from fields.
   * Includes duplicate keys, empty keys, and field-specific config errors.
   */
  readonly allConfigErrors = computed(() => {
    const fields = this.fieldsMap();
    const errors: Array<GmdConfigError & { fieldKey?: string }> = [];

    // Add duplicate key errors
    const duplicates = this.duplicateFieldKeys();
    for (const { key, count } of duplicates) {
      errors.push({
        code: 'duplicateKey',
        message: `fieldKey "${key}" is used ${count} times (must be unique)`,
        severity: 'error',
        field: 'fieldKey',
        fieldKey: key,
      });
    }

    // Add field-specific config errors
    for (const state of fields.values()) {
      if (state.configErrors && state.configErrors.length > 0) {
        for (const err of state.configErrors) {
          errors.push({ ...err, fieldKey: state.fieldKey });
        }
      }
    }

    return errors;
  });

  /**
   * Computed: Critical config errors (severity: 'error').
   */
  readonly criticalConfigErrors = computed(() => {
    return this.allConfigErrors().filter(err => err.severity === 'error');
  });

  /**
   * Computed: Config warnings (severity: 'warning').
   */
  readonly configWarnings = computed(() => {
    return this.allConfigErrors().filter(err => err.severity === 'warning');
  });

  /**
   * Update or add field state to the store.
   * Only updates if the state has actually changed (optimization).
   *
   * @param state - The new field state
   */
  updateField(state: GmdFieldState): void {
    const current = this._fieldsMap();
    const existing = current.get(state.id);

    // Only update if changed (prevents unnecessary signal updates)
    // Note: We use id as the key in the map to handle duplicate fieldKeys or missing keys
    if (!isEqual(existing, state)) {
      const newMap = new Map(current);
      newMap.set(state.id, state);
      this._fieldsMap.set(newMap);
    }
  }

  /**
   * Remove field from state.
   * Called when a form component is destroyed.
   *
   * @param id - The component unique ID to remove
   */
  removeField(id: string): void {
    const current = this._fieldsMap();
    if (current.has(id)) {
      const newMap = new Map(current);
      newMap.delete(id);
      this._fieldsMap.set(newMap);
    }
  }

  /**
   * Clear all field states.
   * Called when Markdown content changes or form is reset.
   */
  reset(): void {
    this._fieldsMap.set(new Map());
  }
}

/**
 * Injection token for GMD form state store.
 */
export const GMD_FORM_STATE_STORE = new InjectionToken<GmdFormStateStore>('GMD_FORM_STATE_STORE');
