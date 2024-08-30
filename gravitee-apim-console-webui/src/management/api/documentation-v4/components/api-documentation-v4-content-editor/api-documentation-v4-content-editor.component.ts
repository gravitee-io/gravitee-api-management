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
import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { GioMonacoEditorModule } from '@gravitee/ui-particles-angular';
import { MarkdownComponent } from 'ngx-markdown';
import { MatButtonModule } from '@angular/material/button';

import { PageType } from '../../../../../entities/management-api-v2';
import { GioSwaggerUiModule } from '../../../../../components/documentation/gio-swagger-ui/gio-swagger-ui.module';
import { GioAsyncApiModule } from '../../../../../components/documentation/gio-async-api/gio-async-api-module';

@Component({
  selector: 'api-documentation-content',
  templateUrl: './api-documentation-v4-content-editor.component.html',
  styleUrls: ['./api-documentation-v4-content-editor.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ApiDocumentationV4ContentEditorComponent),
      multi: true,
    },
  ],
  standalone: true,
  imports: [FormsModule, GioMonacoEditorModule, MarkdownComponent, GioSwaggerUiModule, GioAsyncApiModule, MatButtonModule],
})
export class ApiDocumentationV4ContentEditorComponent implements ControlValueAccessor {
  @Input()
  published = false;

  @Input()
  pageType: PageType;

  preview = true;
  _value: string;
  private _disabled = false;

  public _onChange: (_selection: string) => void = () => ({});

  protected _onTouched: () => void = () => ({});

  // From ControlValueAccessor interface
  public registerOnChange(fn: (value: string) => void): void {
    this._onChange = fn;
  }

  // From ControlValueAccessor interface
  public registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  writeValue(content: string): void {
    this._value = content;
  }

  get disabled(): boolean {
    return this._disabled;
  }

  setDisabledState(isDisabled: boolean) {
    this._disabled = isDisabled;
  }
}
