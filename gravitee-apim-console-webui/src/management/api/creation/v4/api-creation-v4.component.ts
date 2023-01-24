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
import { map, takeUntil } from 'rxjs/operators';
import { Observable, Subject } from 'rxjs';
import { groupBy } from 'lodash';

import { ApiCreationStep, ApiCreationStepperService } from './services/api-creation-stepper.service';
import { ApiCreationV4Step2Component } from './steps/step-2/api-creation-v4-step-2.component';
import { ApiCreationV4Step1Component } from './steps/step-1/api-creation-v4-step-1.component';
import { ApiCreationV4StepWipComponent } from './steps/step-wip/api-creation-v4-step-wip.component';
import { ApiCreationV4Step6Component } from './steps/step-6/api-creation-v4-step-6.component';
import { ApiCreationStepService } from './services/api-creation-step.service';
import { ApiCreationPayload } from './models/ApiCreationPayload';
import { MenuStepItem } from './components/api-creation-stepper-menu/api-creation-stepper-menu.component';
import { Step1MenuItemComponent } from './steps/step-1-menu-item/step-1-menu-item.component';
import { ApiCreationV4Step3Component } from './steps/step-3/api-creation-v4-step-3.component';

@Component({
  selector: 'api-creation-v4',
  template: require('./api-creation-v4.component.html'),
  styles: [require('./api-creation-v4.component.scss')],
})
export class ApiCreationV4Component implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public currentStep: ApiCreationStep & { injector: Injector; payload: ApiCreationPayload };

  public stepper = new ApiCreationStepperService([
    {
      label: 'API Metadata',
      component: ApiCreationV4Step1Component,
      menuItemComponent: Step1MenuItemComponent,
    },
    {
      label: 'Entrypoints',
      component: ApiCreationV4Step2Component,
    },
    {
      label: 'Endpoints',
      component: ApiCreationV4Step3Component,
    },
    {
      label: 'Security',
      component: ApiCreationV4StepWipComponent,
    },
    {
      label: 'Document',
      component: ApiCreationV4StepWipComponent,
    },
    {
      label: 'Summary',
      component: ApiCreationV4Step6Component,
    },
  ]);

  menuSteps$: Observable<MenuStepItem[]> = this.stepper.steps$.pipe(
    map((steps) => {
      // Get the last step of each label. To have last payload of each label.
      const lastStepOfEachLabel = Object.entries(groupBy(steps, 'label')).map(([_, steps]) => steps[steps.length - 1]);

      return lastStepOfEachLabel.map((step) => ({
        ...step,
        payload: this.stepper.compileStepPayload(step),
      }));
    }),
  );

  constructor(private readonly injector: Injector) {}

  ngOnInit(): void {
    this.stepper.currentStep$.pipe(takeUntil(this.unsubscribe$)).subscribe((apiCreationStep) => {
      const apiCreationStepService = new ApiCreationStepService(this.stepper, apiCreationStep);

      this.currentStep = {
        ...apiCreationStep,
        payload: apiCreationStepService.payload,
        injector: Injector.create({
          providers: [{ provide: ApiCreationStepService, useValue: apiCreationStepService }],
          parent: this.injector,
        }),
      };
    });
    // Initialize stepper to step 0
    this.stepper.goToStepLabel('API Metadata');
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }
}
