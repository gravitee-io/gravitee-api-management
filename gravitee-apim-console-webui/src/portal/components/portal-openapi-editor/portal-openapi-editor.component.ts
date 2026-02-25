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
import { Component } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { GioMonacoEditorModule } from '@gravitee/ui-particles-angular';
import { MatButtonModule } from '@angular/material/button';

import { GioSwaggerUiModule } from '../../../components/documentation/gio-swagger-ui/gio-swagger-ui.module';

@Component({
  selector: 'portal-openapi-editor',
  templateUrl: './portal-openapi-editor.component.html',
  styleUrl: './portal-openapi-editor.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: PortalOpenApiEditorComponent,
      multi: true,
    },
  ],
  imports: [FormsModule, GioMonacoEditorModule, GioSwaggerUiModule, MatButtonModule],
})
export class PortalOpenApiEditorComponent implements ControlValueAccessor {
  preview = true;
  _value = '';
  private _disabled = false;

  public _onChange: (value: string) => void = () => ({});

  protected _onTouched: () => void = () => ({});

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
