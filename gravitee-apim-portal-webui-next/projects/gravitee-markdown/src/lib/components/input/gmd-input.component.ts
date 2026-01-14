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

import { GmdFieldErrorCode, GmdFieldState } from '../../models/formField';

@Component({
  selector: 'gmd-input',
  imports: [CommonModule],
  templateUrl: './gmd-input.component.html',
  styleUrl: './gmd-input.component.scss',
  standalone: true,
})
export class GmdInputComponent {
  // metadata key
  fieldKey = input<string | undefined>();

  // UI
  name = input<string>('');
  label = input<string | undefined>();
  placeholder = input<string | undefined>();

  // initial value (optional)
  value = input<string | undefined>();

  // validation
  required = input<boolean>(false);
  minLength = input<number | string | null | undefined>();
  maxLength = input<number | string | null | undefined>();
  pattern = input<string | undefined>();

  readonly = input<boolean>(false);
  disabled = input<boolean>(false);

  errors = computed<GmdFieldErrorCode[]>(() => {
    const v = this.internalValue();
    const errs: GmdFieldErrorCode[] = [];

    if (this.required() && v.trim().length === 0) errs.push('required');

    const minL = this.parseNumberLike(this.minLength());
    if (minL !== null && v.length < minL) errs.push('minLength');

    const maxL = this.parseNumberLike(this.maxLength());
    if (maxL !== null && v.length > maxL) errs.push('maxLength');

    const p = this.pattern();
    if (p) {
      try {
        const re = new RegExp(p);
        if (v.length > 0 && !re.test(v)) errs.push('pattern');
      } catch (e) {
        //TODO We need to figure out how to handle invalid regex patterns
      }
    }

    return errs;
  });

  valid = computed(() => this.errors().length === 0);

  errorMessages = computed<string[]>(() => {
    const errs = this.errors();
    const msgs: string[] = [];
    const minL = this.parseNumberLike(this.minLength());
    const maxL = this.parseNumberLike(this.maxLength());

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

  protected readonly HTMLInputElement = HTMLInputElement;

  private readonly el = inject(ElementRef<HTMLElement>);

  protected readonly internalValue = signal<string>('');
  protected readonly touched = signal<boolean>(false);

  constructor() {
    // init/sync from provided value
    effect(() => {
      const v = this.value();
      if (v !== undefined) {
        this.internalValue.set(String(v));
      }
    });

    // whenever something relevant changes, notify host (only if fieldKey is set)
    effect(() => {
      const key = (this.fieldKey() ?? '').trim();
      if (!key) return; // Early return if no fieldKey

      // Read all reactive values to track changes
      this.internalValue();
      this.required();
      this.minLength();
      this.maxLength();
      this.pattern();
      this.disabled();

      // Emit state only if fieldKey is present
      this.emitState();
    });
  }

  onInput(event: Event) {
    const value = (event.target as HTMLInputElement | null)?.value ?? '';
    this.internalValue.set(value);
  }

  onBlur() {
    this.touched.set(true);
    this.emitState();
  }

  private parseNumberLike(v: number | string | null | undefined): number | null {
    if (v === undefined || v === null) return null;
    const n = typeof v === 'number' ? v : Number(String(v).trim());
    return Number.isFinite(n) ? n : null;
  }

  private emitState() {
    const key = (this.fieldKey() ?? '').trim();
    if (!key) return;

    // Don't emit state for disabled fields (mimics HTML form behavior where disabled fields are not submitted)
    if (this.disabled()) return;

    // Use untracked to avoid tracking computed values during event dispatch
    const detail: GmdFieldState = untracked(() => ({
      key,
      value: this.internalValue(),
      valid: this.valid(),
      required: !!this.required(),
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
