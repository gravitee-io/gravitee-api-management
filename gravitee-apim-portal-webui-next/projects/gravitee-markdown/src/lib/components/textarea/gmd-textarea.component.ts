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
import { Component, ElementRef, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { uniqueId } from 'lodash';

import { GmdConfigError, GmdFieldErrorCode, GmdFieldState } from '../../models/formField';
import { emptyFieldKeyErrors, normalizedRowsInput, normalizedValueWarning, parseBoolean, useLengthValidation } from '../form-helpers';

@Component({
  selector: 'gmd-textarea',
  imports: [CommonModule],
  templateUrl: './gmd-textarea.component.html',
  styleUrl: './gmd-textarea.component.scss',
  standalone: true,
})
export class GmdTextareaComponent {
  private readonly el = inject(ElementRef<HTMLElement>);
  private readonly id = uniqueId();

  fieldKey = input<string | undefined>();
  name = input<string>('');
  label = input<string | undefined>();
  placeholder = input<string | undefined>();
  value = input<string | undefined>();
  required = input(false, { transform: parseBoolean });
  minLength = input<number | string | undefined>();
  maxLength = input<number | string | undefined>();
  rows = input<number | string>(4);
  readonly = input(false, { transform: parseBoolean });
  disabled = input(false, { transform: parseBoolean });

  protected readonly internalValue = signal<string>('');
  protected readonly touched = signal<boolean>(false);

  private readonly rowsInput = normalizedRowsInput(this.rows);
  private readonly lengthValidation = useLengthValidation(this.minLength, this.maxLength, this.internalValue);

  configErrors = computed<GmdConfigError[]>(() => {
    const errors: GmdConfigError[] = [];

    errors.push(...emptyFieldKeyErrors(this.fieldKey()), ...this.lengthValidation.configErrors());

    // Check rows normalization
    const rowsResult = this.rowsInput.result();
    const rowsWarning = normalizedValueWarning('rows', rowsResult);
    if (rowsWarning) errors.push(rowsWarning);

    return errors;
  });

  validationErrors = computed<GmdFieldErrorCode[]>(() => {
    const v = this.internalValue();
    const errs: GmdFieldErrorCode[] = [];

    if (this.required() && v.trim().length === 0) errs.push('required');

    errs.push(...this.lengthValidation.validationErrors());

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
      }
    }
    return msgs;
  });

  protected readonly rowsVM = this.rowsInput.value;
  protected readonly minLengthVM = this.lengthValidation.minLength;
  protected readonly maxLengthVM = this.lengthValidation.maxLength;
  protected errorId = computed(() => `${this.name()}-error`);
  protected hasErrors = computed(() => this.touched() && this.errorMessages().length > 0);

  constructor() {
    // init/sync from provided value
    effect(() => {
      const v = this.value();
      if (v !== undefined) {
        this.internalValue.set(String(v));
      }
    });

    // whenever something relevant changes, notify host
    effect(() => {
      this.fieldKey();
      this.internalValue();
      this.required();
      this.minLength();
      this.maxLength();
      this.disabled();

      this.emitState();
    });
  }

  onInput(event: Event) {
    const value = (event.target as HTMLTextAreaElement | null)?.value ?? '';
    this.internalValue.set(value);
  }

  onBlur() {
    this.touched.set(true);
    this.emitState();
  }

  private emitState() {
    const fieldKey = (this.fieldKey() ?? '').trim();
    const configErrors = this.configErrors();

    // Don't emit state for disabled fields
    if (this.disabled()) return;

    // Use untracked to avoid tracking computed values during event dispatch
    const detail: GmdFieldState = untracked(() => ({
      id: this.id,
      fieldKey: fieldKey,
      value: this.internalValue(),
      valid: this.valid(),
      required: this.required(),
      touched: this.touched(),
      validationErrors: this.validationErrors(),
      configErrors,
    }));

    // Dispatch event asynchronously to avoid blocking the event loop
    setTimeout(() => {
      this.el.nativeElement.dispatchEvent(
        new CustomEvent<GmdFieldState>('gmdFieldStateChange', {
          detail,
          bubbles: true,
          composed: true,
        }),
      );
    }, 0);
  }
}
