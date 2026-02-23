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
import { CommonModule } from '@angular/common';
import { Component, computed, effect, input, signal } from '@angular/core';

import { GmdConfigError, GmdFieldErrorCode, GmdFieldState } from '../../models/formField';
import { GmdFormFieldBase } from '../form-field-base/gmd-form-field-base.component';
import { emptyFieldKeyErrors, parseBoolean } from '../form-helpers';

@Component({
  selector: 'gmd-select',
  imports: [CommonModule],
  templateUrl: './gmd-select.component.html',
  styleUrl: './gmd-select.component.scss',
  standalone: true,
})
export class GmdSelectComponent extends GmdFormFieldBase {
  // Inputs
  fieldKey = input<string | undefined>();
  name = input<string>('');
  label = input<string | undefined>();
  value = input<string | undefined>();
  required = input(false, { transform: parseBoolean });
  options = input<string>('');
  disabled = input(false, { transform: parseBoolean });

  // State
  protected readonly internalValue = signal<string>('');
  protected readonly touched = signal<boolean>(false);

  // Computed
  configErrors = computed<GmdConfigError[]>(() => {
    const errors: GmdConfigError[] = [];

    errors.push(...emptyFieldKeyErrors(this.fieldKey()));

    return errors;
  });

  validationErrors = computed<GmdFieldErrorCode[]>(() => {
    const v = this.internalValue();
    const errs: GmdFieldErrorCode[] = [];

    if (this.required() && v.trim().length === 0) errs.push('required');

    return errs;
  });

  valid = computed(() => this.validationErrors().length === 0);

  errorMessages = computed<string[]>(() => {
    const errs = this.validationErrors();
    const msgs: string[] = [];

    for (const e of errs) {
      switch (e) {
        case 'required':
          msgs.push('This field is required.');
          break;
      }
    }
    return msgs;
  });

  optionsVM = computed(() => {
    const opts = this.options();
    if (!opts) return [];

    // Decode HTML entities (Angular should do this automatically, but be safe)
    // The renderer service encodes quotes in options attribute to prevent DOMParser truncation
    const decodedOpts = opts
      .replace(/&quot;/g, '"')
      .replace(/&#39;/g, "'")
      .replace(/&amp;/g, '&')
      .replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>');

    // Try to parse as JSON array first
    try {
      const parsed = JSON.parse(decodedOpts);
      if (Array.isArray(parsed)) {
        return parsed.map(opt => (typeof opt === 'string' ? opt : String(opt)));
      }
    } catch {
      // Not JSON, try comma-separated
    }

    // Parse as comma-separated values
    return decodedOpts
      .split(',')
      .map(opt => opt.trim())
      .filter(opt => opt.length > 0);
  });

  protected errorId = computed(() => `${this.name()}-error`);
  protected hasErrors = computed(() => this.touched() && this.errorMessages().length > 0);

  // Init/sync from provided value
  private readonly valueSync = effect(() => {
    const v = this.value();
    if (v !== undefined) {
      this.internalValue.set(String(v));
    }
  });

  onChange(event: Event) {
    const value = (event.target as HTMLSelectElement | null)?.value ?? '';
    this.internalValue.set(value);
  }

  onBlur() {
    this.touched.set(true);
  }

  protected trackProperties(): void {
    this.fieldKey();
    this.internalValue();
    this.required();
    this.disabled();
  }

  protected buildFieldState(): GmdFieldState {
    return {
      id: this.id,
      fieldKey: (this.fieldKey() ?? '').trim(),
      value: this.internalValue(),
      valid: this.valid(),
      required: this.required(),
      touched: this.touched(),
      validationErrors: this.validationErrors(),
      configErrors: this.configErrors(),
    };
  }

  protected isDisabled(): boolean {
    return this.disabled();
  }
}
