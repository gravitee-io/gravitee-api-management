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
import { Component, OnInit } from '@angular/core';
import { FieldType, FieldTypeConfig } from '@ngx-formly/core';
import { Observable, of } from 'rxjs';
import { startWith, switchMap } from 'rxjs/operators';

import { LlmProxyApiTypeService } from './llm-proxy-api-type.service';

@Component({
  selector: 'llm-proxy-model-type',
  templateUrl: './llm-proxy-model-type.component.html',
  styleUrls: ['./llm-proxy-model-type.component.scss'],
  standalone: false,
})
export class LlmProxyModelTypeComponent extends FieldType<FieldTypeConfig> implements OnInit {
  filteredModels: Observable<string[]>;

  constructor(private readonly llmProxyApiTypeService: LlmProxyApiTypeService) {
    super();
  }

  ngOnInit() {
    const llmProxyApiField = this.props.llmProxyApiField;
    if (!llmProxyApiField) {
      // eslint-disable-next-line angular/log
      console.error('LlmProxyModelTypeComponent: llmProxyApiField is undefined');
      return;
    }

    const apiControl = this.form.get(llmProxyApiField);
    if (!apiControl) {
      // eslint-disable-next-line angular/log
      console.error(`LlmProxyModelTypeComponent: form control '${llmProxyApiField}' not found`);
      return;
    }

    // When the LLM Proxy API changes, reset the model value and fetch new models
    const apiId$ = apiControl.valueChanges.pipe(startWith(apiControl.value ?? ''));

    this.filteredModels = apiId$.pipe(
      switchMap((apiId) => {
        if (!apiId) {
          return of([]);
        }
        return this.formControl.valueChanges.pipe(
          startWith(this.formControl.value ?? ''),
          switchMap((term) => this.llmProxyApiTypeService.filterModelsForApi$(apiId, term)),
        );
      }),
    );

    // Reset model value when API changes (skip initial)
    apiControl.valueChanges.subscribe(() => {
      this.formControl.setValue('', { emitEvent: true });
    });
  }
}
