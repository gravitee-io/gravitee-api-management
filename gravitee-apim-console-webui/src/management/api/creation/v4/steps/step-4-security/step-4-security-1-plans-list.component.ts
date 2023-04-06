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

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';

import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { Step5DocumentationComponent } from '../step-5-documentation/step-5-documentation.component';
import { NewPlan } from '../../../../../../entities/plan-v4';

@Component({
  selector: 'step-4-security-1-plans-list',
  template: require('./step-4-security-1-plans-list.component.html'),
  styles: [require('./step-4-security-1-plans-list.component.scss'), require('../api-creation-steps-common.component.scss')],
})
export class Step4Security1PlansListComponent {
  @Input()
  plans: NewPlan[] = [];

  @Output()
  addPlanClicked = new EventEmitter();

  public form = new FormGroup({});
  displayedColumns: string[] = ['name', 'security', 'actions'];

  constructor(private readonly stepService: ApiCreationStepService) {}

  save(): void {
    this.stepService.validStep((previousPayload) => ({
      ...previousPayload,
      plans: this.plans,
    }));

    this.stepService.goToNextStep({ groupNumber: 5, component: Step5DocumentationComponent });
  }

  goBack(): void {
    this.stepService.goToPreviousStep();
  }

  addPlan() {
    this.addPlanClicked.next();
  }

  removePlan(plan: NewPlan) {
    this.plans = this.plans.filter((listedPlan) => listedPlan !== plan);
  }
}
