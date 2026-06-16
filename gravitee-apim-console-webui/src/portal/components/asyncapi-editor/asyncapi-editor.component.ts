/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { Component, CUSTOM_ELEMENTS_SCHEMA, input } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { GioMonacoEditorModule } from '@gravitee/ui-particles-angular';

@Component({
  selector: 'asyncapi-editor',
  standalone: true,
  templateUrl: './asyncapi-editor.component.html',
  styleUrl: './asyncapi-editor.component.scss',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: AsyncApiEditorComponent,
      multi: true,
    },
  ],
  imports: [FormsModule, GioMonacoEditorModule],
})
export class AsyncApiEditorComponent implements ControlValueAccessor {
  showPreview = input(true);

  _value = '';
  private _disabled = false;

  _onChange: (value: string) => void = () => undefined;
  _onTouched: () => void = () => undefined;

  registerOnChange(fn: (value: string) => void): void {
    this._onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  writeValue(content: string): void {
    this._value = content ?? '';
  }

  get disabled(): boolean {
    return this._disabled;
  }

  setDisabledState(isDisabled: boolean): void {
    this._disabled = isDisabled;
  }
}
