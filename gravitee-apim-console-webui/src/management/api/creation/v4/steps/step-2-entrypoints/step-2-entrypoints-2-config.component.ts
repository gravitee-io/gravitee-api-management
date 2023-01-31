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
import { FormBuilder, FormGroup } from '@angular/forms';
import { Subject } from 'rxjs';

import { EntrypointService } from '../../../../../../services-ngx/entrypoint.service';
import { ApiCreationStepService } from '../../services/api-creation-step.service';

@Component({
  selector: 'step-2-entrypoints-2-config',
  template: require('./step-2-entrypoints-2-config.component.html'),
  styles: [require('./step-2-entrypoints-2-config.component.scss'), require('../api-creation-steps-common.component.scss')],
})
export class Step2Entrypoints2ConfigComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public formGroup: FormGroup;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly entrypointService: EntrypointService,
    private readonly stepService: ApiCreationStepService,
  ) {}

  ngOnInit(): void {
    // const currentStepPayload = this.stepService.payload;

    this.formGroup = this.formBuilder.group({});

    // TODO: remove me when the step is implemented and tested
    this.stepService.validStepAndGoNext((previousPayload) => ({ ...previousPayload }));
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  save(): void {
    this.stepService.validStepAndGoNext((previousPayload) => ({ ...previousPayload }));
  }

  goBack(): void {
    this.stepService.goToPreviousStep();
  }
}
