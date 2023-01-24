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
import { Component, Input, OnChanges } from '@angular/core';

import { ApiCreationStep } from '../../services/api-creation-stepper.service';

@Component({
  selector: 'api-creation-stepper-menu',
  template: require('./api-creation-stepper-menu.component.html'),
  styles: [require('./api-creation-stepper-menu.component.scss')],
})
export class ApiCreationStepperMenuComponent implements OnChanges {
  @Input()
  public steps: ApiCreationStep[];

  @Input()
  public currentStep: ApiCreationStep;

  public lastPrimaryStepIndex = 0;

  public primarySteps: ApiCreationStep[] = [];

  ngOnChanges() {
    if (!this.steps || !this.currentStep) {
      return;
    }

    this.primarySteps = this.steps.filter((s) => s.type === 'primary');

    const currentStepIndex = this.steps.findIndex((s) => s.label === this.currentStep.label);

    this.lastPrimaryStepIndex = this.steps
      .map((step, index) => ({ index, ...step }))
      .slice(0, currentStepIndex + 1)
      .reverse()
      .find((s) => s.type === 'primary').index;
  }
}
