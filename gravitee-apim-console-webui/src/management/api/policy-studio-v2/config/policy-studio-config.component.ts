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
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';

import { PolicyStudioConfigService } from './policy-studio-config.service';

import { ApiDefinition } from '../models/ApiDefinition';
import { PolicyStudioService } from '../policy-studio.service';
import { Constants } from '../../../../entities/Constants';
import { FlowConfigurationSchema } from '../../../../entities/flow/configurationSchema';

@Component({
  selector: 'policy-studio-config',
  templateUrl: './policy-studio-config.component.html',
  styleUrls: ['./policy-studio-config.component.scss'],
  standalone: false,
})
export class PolicyStudioConfigComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<boolean>();

  public information = `There are two flow modes: default and best match. If you keep the flow mode as default,
  <br/>execution of each flow is determined independently based on the operator defined in the flow itself. Default mode allows for the execution of <strong>multiple</strong> flows.
  <br/>However, if you select best match, the gateway will choose a <strong>single</strong> flow with the closest match to the path of the API request.
  <br/>A plain text part of the path will take precedence over a path parameter.`;

  public flowConfigurationSchema: FlowConfigurationSchema;

  public apiDefinition: ApiDefinition;
  public configForm: UntypedFormGroup;

  public isReadonly = false;

  get isLoading() {
    return this.apiDefinition == null;
  }

  constructor(
    private readonly policyStudioService: PolicyStudioService,
    private readonly policyStudioSettingsService: PolicyStudioConfigService,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  ngOnInit(): void {
    combineLatest([this.policyStudioService.getApiDefinition$(), this.policyStudioSettingsService.getConfigurationSchemaForm()])
      .pipe(
        tap(([definition, flowConfigurationSchema]) => {
          this.apiDefinition = definition;

          this.isReadonly = definition.origin === 'kubernetes';

          this.configForm = new UntypedFormGroup({
            emulateV4Engine: new UntypedFormControl({
              value: definition.execution_mode === 'v4-emulation-engine',
              disabled: this.isReadonly,
            }),
            flowConfiguration: new UntypedFormControl({
              value: {
                flow_mode: definition.flow_mode,
              },
              disabled: this.isReadonly,
            }),
          });

          this.configForm.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(value => {
            this.policyStudioService.saveApiDefinition({
              ...this.apiDefinition,
              flow_mode: value.flowConfiguration.flow_mode,
              execution_mode: value.emulateV4Engine ? 'v4-emulation-engine' : 'v3',
            });
          });
          this.flowConfigurationSchema = flowConfigurationSchema;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
