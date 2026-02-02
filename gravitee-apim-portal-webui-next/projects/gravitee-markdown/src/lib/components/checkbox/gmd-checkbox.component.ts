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
import { Component, ElementRef, computed, effect, inject, input, signal, untracked } from '@angular/core';

import { GmdFieldErrorCode, GmdFieldState } from '../../models/formField';

@Component({
  selector: 'gmd-checkbox',
  imports: [],
  templateUrl: './gmd-checkbox.component.html',
  styleUrl: './gmd-checkbox.component.scss',
  standalone: true,
})
export class GmdCheckboxComponent {
  // metadata key
  fieldKey = input<string | undefined>();

  // UI
  name = input<string>('');
  label = input<string | undefined>();

  // initial value (optional)
  value = input<boolean>(false);

  // validation
  required = input<boolean>(false);
  readonly = input<boolean>(false);
  disabled = input<boolean>(false);

  private readonly el = inject(ElementRef<HTMLElement>);

  protected readonly internalValue = signal<boolean>(false);
  protected readonly touched = signal<boolean>(false);

  errors = computed<GmdFieldErrorCode[]>(() => {
    const errs: GmdFieldErrorCode[] = [];

    if (this.required() && !this.internalValue()) errs.push('required');

    return errs;
  });

  valid = computed(() => this.errors().length === 0);

  errorMessages = computed<string[]>(() => {
    const errs = this.errors();
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

  constructor() {
    // init/sync from provided value
    effect(() => {
      const v = this.value();
      this.internalValue.set(v);
    });

    // whenever something relevant changes, notify host (only if fieldKey is set)
    effect(() => {
      const key = (this.fieldKey() ?? '').trim();
      if (!key) return; // Early return if no fieldKey

      // Read all reactive values to track changes
      this.internalValue();
      this.required();
      this.disabled();

      // Emit state only if fieldKey is present
      this.emitState();
    });
  }

  onChange(event: Event) {
    const checked = (event.target as HTMLInputElement | null)?.checked ?? false;
    this.internalValue.set(checked);
  }

  onBlur() {
    this.touched.set(true);
    this.emitState();
  }

  private emitState() {
    const key = (this.fieldKey() ?? '').trim();
    if (!key) return;

    // Don't emit state for disabled fields (mimics HTML form behavior where disabled fields are not submitted)
    if (this.disabled()) return;

    // Use untracked to avoid tracking computed values during event dispatch
    const detail: GmdFieldState = untracked(() => ({
      key,
      value: this.internalValue() ? 'true' : 'false',
      valid: this.valid(),
      required: this.required(),
      touched: this.touched(),
      errors: this.errors(),
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
