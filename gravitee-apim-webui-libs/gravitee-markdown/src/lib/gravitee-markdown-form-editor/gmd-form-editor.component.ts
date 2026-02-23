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
import { ChangeDetectorRef, Component, forwardRef, inject } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { isString } from 'lodash';

import { GraviteeMarkdownEditorModule } from '../gravitee-markdown-editor/gravitee-markdown-editor.module';
import { GmdFormHostComponent } from '../gravitee-markdown-form-host/gmd-form-host.component';
import { GmdFormValidationPanelComponent } from '../gravitee-markdown-form-validation-panel/gmd-form-validation-panel.component';
import { GraviteeMarkdownViewerModule } from '../gravitee-markdown-viewer/gravitee-markdown-viewer.module';

@Component({
  selector: 'gmd-form-editor',
  imports: [
    CommonModule,
    GraviteeMarkdownEditorModule,
    GraviteeMarkdownViewerModule,
    GmdFormHostComponent,
    GmdFormValidationPanelComponent,
  ],
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
export class GmdFormEditorComponent implements ControlValueAccessor {
  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  value = '';
  isDisabled = false;

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
