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
import { Component, forwardRef } from '@angular/core';
import { ControlValueAccessor, FormControl, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { GioMonacoEditorModule, MonacoEditorLanguageConfig } from '@gravitee/ui-particles-angular';
import { JsonPipe } from '@angular/common';

import { MCPTool } from '../../../../../entities/entrypoint/mcp';

@Component({
  selector: 'open-api-to-mcp-tools',
  imports: [GioMonacoEditorModule, ReactiveFormsModule, JsonPipe],
  templateUrl: './open-api-to-mcp-tools.component.html',
  styleUrl: './open-api-to-mcp-tools.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => OpenApiToMcpToolsComponent),
      multi: true,
    },
  ],
})
export class OpenApiToMcpToolsComponent implements ControlValueAccessor {
  formControl: FormControl<string> = new FormControl<string>('');
  languageOptions: MonacoEditorLanguageConfig = { language: 'json' };

  private onChange: (value: MCPTool[]) => void = () => {};
  private onTouched: () => void = () => {};

  constructor() {
    this.formControl.valueChanges.subscribe((value) => {
      if (value !== null) {
        this.languageOptions = {
          language: value.trim().startsWith('{') ? 'json' : 'yaml',
        };
        this.emitValue(value);
      }
    });
  }

  writeValue(value: string): void {
    this.formControl.setValue(value, { emitEvent: false });
  }

  registerOnChange(fn: (value: MCPTool[]) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.formControl.disable();
    } else {
      this.formControl.enable();
    }
  }

  private emitValue(_value: string): void {
    // TODO: have util return gateway mapping as well to then emit tools as value
    // TODO: show errors in UI and set the control as invalid
    // const resultErrors = convertOpenApiToMcpTools(value.trim());
    this.onChange([]);
    this.onTouched();
  }
}
