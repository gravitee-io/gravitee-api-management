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
import { Component, computed, input } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { GioMonacoEditorModule } from '@gravitee/ui-particles-angular';

import { GioSwaggerUiModule } from '../../../components/documentation/gio-swagger-ui/gio-swagger-ui.module';
import { GioRedocComponent } from '../../../components/documentation/gio-redoc/gio-redoc.component';
import {
  OpenApiDocExpansion,
  OpenApiViewer,
  OpenApiViewerConfiguration,
} from '../../../entities/management-api-v2/portalPageContent/openApiViewerConfiguration';

@Component({
  selector: 'openapi-editor',
  templateUrl: './openapi-editor.component.html',
  styleUrl: './openapi-editor.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: OpenApiEditorComponent,
      multi: true,
    },
  ],
  imports: [FormsModule, GioMonacoEditorModule, GioSwaggerUiModule, GioRedocComponent],
})
export class OpenApiEditorComponent implements ControlValueAccessor {
  configuration = input<Partial<OpenApiViewerConfiguration> | null>(null);

  _value = '';
  private _disabled = false;

  _onChange: (value: string) => void = () => ({});

  _onTouched: () => void = () => ({});

  isRedocViewer = computed(() => this.configuration()?.viewer === OpenApiViewer.Redoc);

  swaggerTryItURL = computed(() => this.configuration()?.tryItURL ?? '');
  swaggerDocExpansion = computed(() => this.configuration()?.docExpansion ?? OpenApiDocExpansion.None);
  swaggerDisplayOperationId = computed(() => this.configuration()?.displayOperationId ?? false);
  swaggerFilter = computed(() => this.configuration()?.enableFiltering ?? false);
  swaggerShowExtensions = computed(() => this.configuration()?.showExtensions ?? false);
  swaggerShowCommonExtensions = computed(() => this.configuration()?.showCommonExtensions ?? false);
  swaggerMaxDisplayedTags = computed(() => {
    const raw = this.configuration()?.maxDisplayedTags;
    if (raw == null) return undefined;
    const v = Number(raw);
    return Number.isFinite(v) && v >= 0 ? v : undefined;
  });

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
