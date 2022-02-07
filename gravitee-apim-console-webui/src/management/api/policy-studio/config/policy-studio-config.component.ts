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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { combineLatest, Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';
import { FlowConfigurationSchema } from '@gravitee/ui-policy-studio-angular';
import '@gravitee/ui-components/wc/gv-icon';
import '@gravitee/ui-components/wc/gv-schema-form';

import { PolicyStudioConfigService } from './policy-studio-config.service';

import { ApiDefinition } from '../models/ApiDefinition';
import { PolicyStudioService } from '../policy-studio.service';
import { GvSchemaFormChangeEvent } from '../models/GvSchemaFormChangeEvent';

@Component({
  selector: 'policy-studio-config',
  template: require('./policy-studio-config.component.html'),
  styles: [require('./policy-studio-config.component.scss')],
})
export class PolicyStudioConfigComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<boolean>();

  public information = `By default, the selection of a flow is based on the operator defined in the flow itself. 
  <br/>This operator allows either to select a flow when the path matches exactly, or when the start of the path matches. 
  <br/>The "Best match" option allows you to select the flow from the path that is closest.`;

  public schema: FlowConfigurationSchema;

  apiDefinition: ApiDefinition;

  constructor(
    private readonly policyStudioService: PolicyStudioService,
    private readonly policyStudioSettingsService: PolicyStudioConfigService,
  ) {}

  ngOnInit(): void {
    combineLatest([this.policyStudioService.getApiDefinition$(), this.policyStudioSettingsService.getConfigurationSchemaForm()])
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(([definition, schema]) => {
          this.apiDefinition = definition;
          this.schema = schema;
        }),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onChange($event: GvSchemaFormChangeEvent<ApiDefinition>) {
    this.apiDefinition = { ...this.apiDefinition, ...$event.detail.values };
    this.policyStudioService.emitApiDefinition(this.apiDefinition);
  }

  get isLoading() {
    return this.apiDefinition == null;
  }
}
