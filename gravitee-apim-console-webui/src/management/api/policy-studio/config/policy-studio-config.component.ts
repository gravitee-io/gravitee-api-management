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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { combineLatest, Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';
import { FormControl, FormGroup } from '@angular/forms';

import { PolicyStudioConfigService } from './policy-studio-config.service';

import { ApiDefinition } from '../models/ApiDefinition';
import { PolicyStudioService } from '../policy-studio.service';
import { Constants } from '../../../../entities/Constants';
import { FlowConfigurationSchema } from '../../../../entities/flow/configurationSchema';

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

  public flowConfigurationSchema: FlowConfigurationSchema;

  public apiDefinition: ApiDefinition;
  public configForm: FormGroup;

  public isReadonly = false;

  get isLoading() {
    return this.apiDefinition == null;
  }

  public displayJupiterToggle = false;

  constructor(
    private readonly policyStudioService: PolicyStudioService,
    private readonly policyStudioSettingsService: PolicyStudioConfigService,
    @Inject('Constants') private readonly constants: Constants,
  ) {}

  ngOnInit(): void {
    this.displayJupiterToggle = this.constants.org?.settings?.jupiterMode?.enabled ?? false;
    combineLatest([this.policyStudioService.getApiDefinition$(), this.policyStudioSettingsService.getConfigurationSchemaForm()])
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(([definition, flowConfigurationSchema]) => {
          this.apiDefinition = definition;

          this.isReadonly = definition.origin === 'kubernetes';

          this.configForm = new FormGroup({
            jupiterModeEnabled: new FormControl({
              value: definition.execution_mode === 'jupiter',
              disabled: this.isReadonly,
            }),
            flowConfiguration: new FormControl({
              value: {
                flow_mode: definition.flow_mode,
              },
              disabled: this.isReadonly,
            }),
          });

          this.configForm.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe((value) => {
            this.policyStudioService.saveApiDefinition({
              ...this.apiDefinition,
              flow_mode: value.flowConfiguration.flow_mode,
              execution_mode: value.jupiterModeEnabled ? 'jupiter' : 'v3',
            });
          });
          this.flowConfigurationSchema = flowConfigurationSchema;
        }),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
