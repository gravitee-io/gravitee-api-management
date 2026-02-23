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
import { Component, computed, effect, input, signal } from '@angular/core';

import { GmdConfigError, GmdFieldErrorCode, GmdFieldState } from '../../models/formField';
import { GmdFormFieldBase } from '../form-field-base/gmd-form-field-base.component';
import { emptyFieldKeyErrors, parseBoolean } from '../form-helpers';

@Component({
  selector: 'gmd-checkbox',
  imports: [],
  templateUrl: './gmd-checkbox.component.html',
  styleUrl: './gmd-checkbox.component.scss',
  standalone: true,
})
export class GmdCheckboxComponent extends GmdFormFieldBase {
  // Inputs
  fieldKey = input<string | undefined>();
  name = input<string>('');
  label = input<string | undefined>();
  value = input(false, { transform: parseBoolean });
  required = input(false, { transform: parseBoolean });
  readonly = input(false, { transform: parseBoolean });
  disabled = input(false, { transform: parseBoolean });

  // State
  protected readonly internalValue = signal<boolean>(false);
  protected readonly touched = signal<boolean>(false);

  // Computed
  configErrors = computed<GmdConfigError[]>(() => {
    const errors: GmdConfigError[] = [];

    errors.push(...emptyFieldKeyErrors(this.fieldKey()));

    return errors;
  });

  validationErrors = computed<GmdFieldErrorCode[]>(() => {
    const errs: GmdFieldErrorCode[] = [];

    if (this.required() && !this.internalValue()) errs.push('required');

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

  protected errorId = computed(() => `${this.name()}-error`);
  protected hasErrors = computed(() => this.touched() && this.errorMessages().length > 0);

  // Init/sync from provided value
  private readonly valueSync = effect(() => {
    const v = this.value();
    this.internalValue.set(v);
  });

  onChange(event: Event) {
    const checked = (event.target as HTMLInputElement | null)?.checked ?? false;
    this.internalValue.set(checked);
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
      value: this.internalValue() ? 'true' : 'false',
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
