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

import { Component, OnInit } from '@angular/core';

import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { CreatePlanV4 } from '../../../../../../entities/management-api-v2';
import { PLAN_SECURITY_TYPES, PlanSecurityVM } from '../../../../../../services-ngx/constants.service';

@Component({
  selector: 'step-4-security-1-plans',
  template: require('./step-4-security-1-plans.component.html'),
  styles: [require('./step-4-security-1-plans.component.scss'), require('../api-creation-steps-common.component.scss')],
})
export class Step4Security1PlansComponent implements OnInit {
  public view: 'list' | 'add' | 'edit' = 'list';

  public plans: CreatePlanV4[] = [];

  public planToEdit: CreatePlanV4 | undefined = undefined;

  securityType: PlanSecurityVM;

  constructor(private readonly stepService: ApiCreationStepService) {}

  ngOnInit(): void {
    const currentStepPayload = this.stepService.payload;

    // For first pass-through of creation workflow
    if (!currentStepPayload.plans) {
      this.plans.push({
        definitionVersion: 'V4',
        name: 'Default Keyless (UNSECURED)',
        description: 'Default unsecured plan',
        security: {
          type: 'KEY_LESS',
          configuration: {},
        },
        validation: 'MANUAL',
      });
    } else {
      this.plans = currentStepPayload.plans;
    }
  }

  onAddPlanClicked(securityType: PlanSecurityVM) {
    this.planToEdit = undefined;
    this.securityType = securityType;
    this.view = 'add';
  }

  addPlan(plan: CreatePlanV4) {
    this.plans.push(plan);
    this.view = 'list';
  }

  onExitPlanCreation() {
    this.view = 'list';
  }

  onEditPlanClicked(plan: CreatePlanV4) {
    this.planToEdit = plan;
    this.securityType = PLAN_SECURITY_TYPES.find((vm) => vm.id === plan.security.type);
    this.view = 'edit';
  }

  editPlan(plan: CreatePlanV4) {
    const planToEditIndex = this.plans.findIndex((plan) => plan === this.planToEdit);
    this.plans.splice(planToEditIndex, 1, plan);
    this.view = 'list';
  }

  onRemovePlanClicked(plan: CreatePlanV4) {
    this.plans = this.plans.filter((listedPlan) => listedPlan !== plan);
  }

  protected readonly undefined = undefined;
}
