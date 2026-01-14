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
import { AfterViewInit, ChangeDetectorRef, Component, computed, ElementRef, forwardRef, inject, OnDestroy, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { isString } from 'lodash';

import { GraviteeMarkdownEditorModule } from '../gravitee-markdown-editor/gravitee-markdown-editor.module';
import { GraviteeMarkdownViewerModule } from '../gravitee-markdown-viewer/gravitee-markdown-viewer.module';
import { GmdFieldErrorCode, GmdFieldState } from '../models/formField';

@Component({
  selector: 'gmd-form-editor',
  imports: [CommonModule, GraviteeMarkdownEditorModule, GraviteeMarkdownViewerModule],
  templateUrl: './gmd-form-editor.component.html',
  styleUrl: './gmd-form-editor.component.scss',
  standalone: true,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GmdFormEditorComponent),
      multi: true,
    },
  ],
})
export class GmdFormEditorComponent implements ControlValueAccessor, AfterViewInit, OnDestroy {
  value = '';
  isDisabled = false;

  // Computed form validity: all required fields must be valid
  formValid = computed(() => {
    const fields = this.fieldsMap();
    if (fields.size === 0) return true; // No fields with fieldKey means form is valid

    for (const state of fields.values()) {
      if (state.required && !state.valid) {
        return false;
      }
    }
    return true;
  });

  // Count of required fields
  requiredFieldsCount = computed(() => {
    const fields = this.fieldsMap();
    let count = 0;
    for (const state of fields.values()) {
      if (state.required) count++;
    }
    return count;
  });

  // Count of valid required fields
  validRequiredFieldsCount = computed(() => {
    const fields = this.fieldsMap();
    let count = 0;
    for (const state of fields.values()) {
      if (state.required && state.valid) count++;
    }
    return count;
  });

  // List of invalid fields with their errors
  invalidFields = computed(() => {
    const fields = this.fieldsMap();
    const invalid: Array<{ key: string; errors: string[] }> = [];
    for (const [key, state] of fields.entries()) {
      if (!state.valid && state.errors.length > 0) {
        invalid.push({ key, errors: state.errors });
      }
    }
    return invalid;
  });

  // List of field values for preview
  fieldValues = computed(() => {
    const fields = this.fieldsMap();
    const values: Array<{ key: string; value: string }> = [];
    for (const [key, state] of fields.entries()) {
      values.push({ key, value: state.value });
    }
    return values;
  });

  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly hostElement = inject(ElementRef<HTMLElement>);

  // Map of field states keyed by fieldKey
  private readonly fieldsMap = signal<Map<string, GmdFieldState>>(new Map());

  private eventHandler?: (event: Event) => void;

  ngAfterViewInit(): void {
    const host = this.hostElement.nativeElement;
    this.eventHandler = (event: Event) => {
      const customEvent = event as CustomEvent<GmdFieldState>;
      if (customEvent.detail) {
        const state = customEvent.detail;
        const currentMap = this.fieldsMap();

        const existingState = currentMap.get(state.key);
        if (
          !existingState ||
          existingState.value !== state.value ||
          existingState.valid !== state.valid ||
          existingState.required !== state.required ||
          existingState.touched !== state.touched ||
          !this.sameErrors(existingState.errors, state.errors)
        ) {
          const newMap = new Map(currentMap);
          newMap.set(state.key, state);
          this.fieldsMap.set(newMap);
        }
      }
    };

    host.addEventListener('gmdFieldStateChange', this.eventHandler);
  }

  ngOnDestroy(): void {
    if (this.eventHandler) {
      const host = this.hostElement.nativeElement;
      host.removeEventListener('gmdFieldStateChange', this.eventHandler);
    }
  }

  public writeValue(value: string | null): void {
    if (value !== null && value !== undefined) {
      this.value = isString(value) ? value : JSON.stringify(value);
      this.fieldsMap.set(new Map());
    }
  }

  public registerOnChange(fn: (_value: string | null) => void): void {
    this._onChange = fn;
  }

  public registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  public setDisabledState(isDisabled: boolean): void {
    this.isDisabled = isDisabled;
    this.changeDetectorRef.detectChanges();
  }

  public onValueChange(value: string): void {
    this.value = value;
    // Reset fields map when markdown content changes
    this.fieldsMap.set(new Map());
    this._onChange(value);
  }

  public onTouched(): void {
    this._onTouched();
  }

  public formatValue(value: string): string {
    return value.trim().length === 0 ? '(empty)' : value;
  }

  protected _onChange: (_value: string | null) => void = () => ({});
  protected _onTouched: () => void = () => ({});

  private sameErrors(a: GmdFieldErrorCode[], b: GmdFieldErrorCode[]): boolean {
    if (a.length !== b.length) {
      return false;
    }
    return a.every((error, index) => error === b[index]);
  }
}
