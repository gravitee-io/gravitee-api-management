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

import { Component, Inject, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { StateService } from '@uirouter/core';

import { UIRouterState } from '../../../../../../ajs-upgraded-providers';
import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { Step5DocumentationComponent } from '../step-5-documentation/step-5-documentation.component';

@Component({
  selector: 'step-4-security',
  template: require('./step-4-security.component.html'),
  styles: [require('./step-4-security.component.scss'), require('../api-creation-steps-common.component.scss')],
})
export class Step4SecurityComponent implements OnInit {
  public form = new FormGroup({});

  constructor(@Inject(UIRouterState) readonly ajsState: StateService, private readonly stepService: ApiCreationStepService) {}

  ngOnInit(): void {
    // const currentStepPayload = this.stepService.payload;
  }

  save(): void {
    this.stepService.validStep((previousPayload) => ({
      ...previousPayload,
    }));

    this.stepService.goToNextStep({ groupNumber: 5, component: Step5DocumentationComponent });
  }

  goBack(): void {
    this.stepService.goToPreviousStep();
  }
}
