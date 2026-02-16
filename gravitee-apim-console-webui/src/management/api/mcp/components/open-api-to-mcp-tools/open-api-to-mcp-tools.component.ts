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
import { Component, computed, effect, forwardRef, Signal } from '@angular/core';
import { ControlValueAccessor, FormControl, NG_VALUE_ACCESSOR, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { GioIconsModule, GioMonacoEditorModule, MonacoEditorLanguageConfig } from '@gravitee/ui-particles-angular';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';

import { convertOpenApiToMcpTools, OpenApiToMcpToolsResult } from './open-api-to-mcp-tools.util';

import { MCPTool, MCPToolDefinition } from '../../../../../entities/entrypoint/mcp';
import { ToolDisplayComponent } from '../tool-display/tool-display.component';

@Component({
  selector: 'open-api-to-mcp-tools',
  imports: [GioMonacoEditorModule, ReactiveFormsModule, MatFormFieldModule, ToolDisplayComponent, MatButtonModule, GioIconsModule],
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
  // languageOptions: MonacoEditorLanguageConfig = { language: 'json' };
  toolDefinitions: MCPToolDefinition[] = [];
  errorMessages: string[] = [];

  private formControlValues = toSignal(this.formControl.valueChanges);
  languageOptions: Signal<MonacoEditorLanguageConfig> = computed(() => {
    const formControlValues: string = this.formControlValues();
    const language = !formControlValues || formControlValues.trim().startsWith('{') ? 'json' : 'yaml';
    return {
      language,
    };
  });

  private onChange: (value: MCPTool[]) => void = () => {};
  private onTouched: () => void = () => {};

  constructor() {
    effect(() => {
      this.emitValue(this.formControlValues());
    });
  }

  writeValue(value: string): void {
    // Stringify the value to ensure it is always a string
    this.formControl.setValue(`${value}`, { emitEvent: false });
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

  private emitValue(value: string): void {
    convertOpenApiToMcpTools(value).then(result => {
      this.toolDefinitions = result.result?.map(r => r.toolDefinition);
      this.handleErrors(result);
      this.onChange(result.result);
      this.onTouched();
    });
  }

  private handleErrors(result: OpenApiToMcpToolsResult): void {
    if (!result.errors?.length) {
      this.errorMessages = [];
      this.formControl.setErrors(null);
      return;
    }

    this.errorMessages = result.errors.map(({ message }) => message);

    const validationErrors: ValidationErrors = result.errors.reduce<ValidationErrors>((acc, { key, message }) => {
      acc[key] = message;
      return acc;
    }, {});
    this.formControl.setErrors(validationErrors);
  }
}
