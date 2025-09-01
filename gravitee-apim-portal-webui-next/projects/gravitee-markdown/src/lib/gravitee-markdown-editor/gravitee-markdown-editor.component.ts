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
import { ChangeDetectorRef, Component, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { isString } from 'lodash';

@Component({
  selector: 'gmd-editor',
  templateUrl: './gravitee-markdown-editor.component.html',
  styleUrl: './gravitee-markdown-editor.component.scss',
  // eslint-disable-next-line @angular-eslint/prefer-standalone
  standalone: false,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GraviteeMarkdownEditorComponent),
      multi: true,
    },
  ],
})
export class GraviteeMarkdownEditorComponent implements ControlValueAccessor {
  value = '';
  isDisabled = false;

  constructor(private readonly changeDetectorRef: ChangeDetectorRef) {}

  public writeValue(value: string | null): void {
    if (value !== null && value !== undefined) {
      this.value = isString(value) ? value : JSON.stringify(value);
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
    this._onChange(value);
  }

  public onTouched(): void {
    this._onTouched();
  }

  protected _onChange: (_value: string | null) => void = () => ({});
  protected _onTouched: () => void = () => ({});
}
