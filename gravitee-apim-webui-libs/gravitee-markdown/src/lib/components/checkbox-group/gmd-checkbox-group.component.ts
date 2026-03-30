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
import { elFallbackErrors, emptyFieldKeyErrors, isElExpression, parseBoolean, parseElFallback } from '../form-helpers';

@Component({
  selector: 'gmd-checkbox-group',
  imports: [CommonModule],
  templateUrl: './gmd-checkbox-group.component.html',
  styleUrl: './gmd-checkbox-group.component.scss',
  standalone: true,
})
export class GmdCheckboxGroupComponent extends GmdFormFieldBase {
  // Inputs
  fieldKey = input<string | undefined>();
  name = input<string>('');
  label = input<string | undefined>();
  value = input<string | undefined>();
  required = input(false, { transform: parseBoolean });
  options = input<string>('');
  disabled = input(false, { transform: parseBoolean });

  // State
  protected readonly internalValues = signal<Set<string>>(new Set());
  protected readonly touched = signal<boolean>(false);

  // Computed
  configErrors = computed<GmdConfigError[]>(() => [...emptyFieldKeyErrors(this.fieldKey()), ...elFallbackErrors(this.options())]);

  validationErrors = computed<GmdFieldErrorCode[]>(() => {
    const errs: GmdFieldErrorCode[] = [];

    if (this.required() && this.internalValues().size === 0) errs.push('required');

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

    // EL expression: show only fallback values in form builder preview
    if (isElExpression(opts)) {
      return parseElFallback(opts);
    }

    return opts
      .split(',')
      .map(opt => opt.trim())
      .filter(opt => opt.length > 0);
  });

  protected errorId = computed(() => `${this.name()}-error`);
  protected groupLabelId = computed(() => `${this.name()}-label`);
  protected hasErrors = computed(() => this.touched() && this.errorMessages().length > 0);

  // Init/sync from provided value (comma-separated preselection)
  private readonly valueSync = effect(() => {
    const v = this.value();
    if (v === undefined) return;

    const items = v
      .split(',')
      .map(s => s.trim())
      .filter(s => s.length > 0);
    this.internalValues.set(new Set(items));
  });

  onChange(event: Event, option: string) {
    const checked = (event.target as HTMLInputElement | null)?.checked ?? false;
    this.internalValues.update(current => this.toggleOption(current, option, checked));
  }

  onBlur() {
    this.touched.set(true);
  }

  protected trackProperties(): void {
    this.fieldKey();
    this.internalValues();
    this.required();
    this.disabled();
  }

  protected buildFieldState(): GmdFieldState {
    return {
      id: this.id,
      fieldKey: (this.fieldKey() ?? '').trim(),
      value: Array.from(this.internalValues()).sort().join(','),
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

  private toggleOption(current: Set<string>, option: string, checked: boolean): Set<string> {
    const next = new Set(current);
    if (checked) {
      next.add(option);
      return next;
    }

    next.delete(option);
    return next;
  }
}
