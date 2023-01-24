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
import { groupBy } from 'lodash';

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

  public lastStepPerLabelNumber: ApiCreationStep[] = [];

  ngOnChanges() {
    if (!this.steps || !this.currentStep) {
      return;
    }

    this.lastStepPerLabelNumber = Object.entries(groupBy(this.steps, 'labelNumber')).map(([_, steps]) => steps[steps.length - 1]);
  }
}
