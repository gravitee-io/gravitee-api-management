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

import { Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { API_CREATION_PAYLOAD, ApiCreationStep, ApiCreationStepperService } from './models/ApiCreationStepperService';
import { ApiCreationV4Step2Component } from './steps/step-2/api-creation-v4-step-2.component';
import { ApiCreationV4Step1Component } from './steps/step-1/api-creation-v4-step-1.component';
import { ApiCreationV4StepWipComponent } from './steps/step-wip/api-creation-v4-step-wip.component';

@Component({
  selector: 'api-creation-v4',
  template: require('./api-creation-v4.component.html'),
  styles: [require('./api-creation-v4.component.scss')],
})
export class ApiCreationV4Component implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public currentStep: ApiCreationStep & { injector: Injector };

  public stepper = new ApiCreationStepperService([
    {
      number: 1,
      label: 'API Metadata',
      component: ApiCreationV4Step1Component,
    },
    {
      number: 2,
      label: 'Entrypoints',
      component: ApiCreationV4Step2Component,
    },
    {
      number: 3,
      label: 'Endpoints',
      component: ApiCreationV4StepWipComponent,
    },
    {
      number: 4,
      label: 'Security',
      component: ApiCreationV4StepWipComponent,
    },
    {
      number: 5,
      label: 'Document',
      component: ApiCreationV4StepWipComponent,
    },
    {
      number: 6,
      label: 'Summary',
      component: ApiCreationV4StepWipComponent,
      payload: { lastStep: true },
    },
  ]);

  constructor(private readonly injector: Injector) {}

  ngOnInit(): void {
    this.stepper.currentStep$.pipe(takeUntil(this.unsubscribe$)).subscribe((apiCreationStep) => {
      this.currentStep = {
        ...apiCreationStep,
        injector: Injector.create({
          providers: [
            { provide: ApiCreationStepperService, useValue: this.stepper },
            { provide: API_CREATION_PAYLOAD, useValue: apiCreationStep.payload },
          ],
          parent: this.injector,
        }),
      };
    });
    // Initialize stepper to step 0
    this.stepper.goToStep(0);
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }
}
