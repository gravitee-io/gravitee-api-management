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
import { CommonModule } from '@angular/common';
import { Component, computed, effect, input, signal } from '@angular/core';

import { GmdConfigError, GmdFieldErrorCode, GmdFieldState } from '../../models/formField';
import { GmdFormFieldBase } from '../form-field-base/gmd-form-field-base.component';
import { emptyFieldKeyErrors, parseBoolean, safePattern, useLengthValidation } from '../form-helpers';

@Component({
  selector: 'gmd-input',
  imports: [CommonModule],
  templateUrl: './gmd-input.component.html',
  styleUrl: './gmd-input.component.scss',
  standalone: true,
})
export class GmdInputComponent extends GmdFormFieldBase {
  // Inputs
  fieldKey = input<string | undefined>();
  name = input<string>('');
  label = input<string | undefined>();
  placeholder = input<string | undefined>();
  value = input<string | undefined>();
  required = input(false, { transform: parseBoolean });
  minLength = input<number | string | undefined>();
  maxLength = input<number | string | undefined>();
  pattern = input<string | undefined>();
  readonly = input(false, { transform: parseBoolean });
  disabled = input(false, { transform: parseBoolean });

  // State
  protected readonly internalValue = signal<string>('');
  protected readonly touched = signal<boolean>(false);

  // Computed
  private readonly lengthValidation = useLengthValidation(this.minLength, this.maxLength, this.internalValue);
  protected readonly minLengthVM = this.lengthValidation.minLength;
  protected readonly maxLengthVM = this.lengthValidation.maxLength;
  private readonly patternResult = computed(() => safePattern(this.pattern()));

  configErrors = computed<GmdConfigError[]>(() => {
    const errors: GmdConfigError[] = [];

    errors.push(...emptyFieldKeyErrors(this.fieldKey()), ...this.lengthValidation.configErrors());

    // Check pattern validity
    const patternResult = this.patternResult();
    if (patternResult.error) {
      errors.push({
        code: 'invalidRegex',
        message: patternResult.error.message,
        severity: 'error',
        field: 'pattern',
        value: patternResult.error.pattern,
      });
    }

    return errors;
  });

  validationErrors = computed<GmdFieldErrorCode[]>(() => {
    const v = this.internalValue();
    const errs: GmdFieldErrorCode[] = [];

    // Use normalized values
    if (this.required() && v.trim().length === 0) errs.push('required');

    errs.push(...this.lengthValidation.validationErrors());

    const pattern = this.patternResult().regex;
    if (pattern && v.length > 0 && !pattern.test(v)) {
      errs.push('pattern');
    }

    return errs;
  });

  valid = computed(() => this.validationErrors().length === 0);

  errorMessages = computed<string[]>(() => {
    const errs = this.validationErrors();
    const msgs: string[] = [];
    const minL = this.minLengthVM();
    const maxL = this.maxLengthVM();

    for (const e of errs) {
      switch (e) {
        case 'required':
          msgs.push('This field is required.');
          break;
        case 'minLength':
          msgs.push(`Minimum length is ${minL}.`);
          break;
        case 'maxLength':
          msgs.push(`Maximum length is ${maxL}.`);
          break;
        case 'pattern':
          msgs.push('Value does not match the required format.');
          break;
      }
    }
    return msgs;
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

  onInput(event: Event) {
    const value = (event.target as HTMLInputElement | null)?.value ?? '';
    this.internalValue.set(value);
  }

  onBlur() {
    this.touched.set(true);
  }

  protected trackProperties(): void {
    this.fieldKey();
    this.internalValue();
    this.required();
    this.minLength();
    this.maxLength();
    this.pattern();
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
