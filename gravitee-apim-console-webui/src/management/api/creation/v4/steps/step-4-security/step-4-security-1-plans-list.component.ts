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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';

import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { Step5DocumentationComponent } from '../step-5-documentation/step-5-documentation.component';
import { ConstantsService, PlanSecurityVM } from '../../../../../../services-ngx/constants.service';
import { CreatePlanV4 } from '../../../../../../entities/management-api-v2';

@Component({
  selector: 'step-4-security-1-plans-list',
  template: require('./step-4-security-1-plans-list.component.html'),
  styles: [require('./step-4-security-1-plans-list.component.scss'), require('../api-creation-steps-common.component.scss')],
})
export class Step4Security1PlansListComponent implements OnInit {
  @Input()
  plans: CreatePlanV4[] = [];

  @Output()
  addPlanClicked = new EventEmitter<PlanSecurityVM>();

  @Output()
  editPlanClicked = new EventEmitter<CreatePlanV4>();

  planSecurityOptions: PlanSecurityVM[];

  public form = new FormGroup({});
  displayedColumns: string[] = ['name', 'security', 'actions'];

  constructor(private readonly stepService: ApiCreationStepService, private readonly constantsService: ConstantsService) {}

  ngOnInit(): void {
    this.planSecurityOptions = this.constantsService.getEnabledPlanSecurityTypes();
  }

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

  addPlan(securityType: PlanSecurityVM) {
    this.addPlanClicked.emit(securityType);
  }

  editPlan(plan: CreatePlanV4) {
    this.editPlanClicked.emit(this.plans.find((listedPlan) => listedPlan === plan));
  }

  removePlan(plan: CreatePlanV4) {
    this.plans = this.plans.filter((listedPlan) => listedPlan !== plan);
  }
}
