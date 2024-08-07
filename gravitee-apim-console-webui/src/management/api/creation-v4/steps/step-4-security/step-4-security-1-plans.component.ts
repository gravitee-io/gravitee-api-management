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
import { ApiType, CreatePlanV4 } from '../../../../../entities/management-api-v2';
import { AVAILABLE_PLANS_FOR_MENU, ConstantsService, PlanMenuItemVM } from '../../../../../services-ngx/constants.service';
import { ApiCreationPayload } from '../../models/ApiCreationPayload';

@Component({
  selector: 'step-4-security-1-plans',
  templateUrl: './step-4-security-1-plans.component.html',
  styleUrls: ['./step-4-security-1-plans.component.scss', '../api-creation-steps-common.component.scss'],
})
export class Step4Security1PlansComponent implements OnInit {
  public view: 'list' | 'add' | 'edit' = 'list';

  public plans: CreatePlanV4[] = [];

  public planToEdit: CreatePlanV4 | undefined = undefined;

  selectedPlanMenuItem: PlanMenuItemVM;
  public apiType: ApiType;
  public isTcpApi: boolean;

  constructor(
    private readonly stepService: ApiCreationStepService,
    private readonly constantsService: ConstantsService,
  ) {}

  ngOnInit(): void {
    const currentStepPayload = this.stepService.payload;

    // For first pass-through of creation workflow
    if (!currentStepPayload.plans) {
      this.computeDefaultApiPlans(currentStepPayload);
    } else {
      this.plans = currentStepPayload.plans;
    }

    this.apiType = currentStepPayload.type;
    this.isTcpApi = currentStepPayload.hosts?.length > 0;
  }

  private computeDefaultApiPlans(currentStepPayload: ApiCreationPayload) {
    const availablePlanMenuItems = this.constantsService.getEnabledPlanMenuItems();
    const entrypoint = currentStepPayload.selectedEntrypoints.find((e) => e.supportedListenerType !== 'SUBSCRIPTION');
    if (entrypoint && availablePlanMenuItems.some((p) => p.planFormType === 'KEY_LESS')) {
      this.plans.push({
        definitionVersion: 'V4',
        name: 'Default Keyless (UNSECURED)',
        description: 'Default unsecured plan',
        mode: 'STANDARD',
        security: {
          type: 'KEY_LESS',
          configuration: {},
        },
        validation: 'MANUAL',
      });
    }

    const subscriptionEntrypoint = currentStepPayload.selectedEntrypoints.find((e) => e.supportedListenerType === 'SUBSCRIPTION');
    if (subscriptionEntrypoint && availablePlanMenuItems.some((p) => p.planFormType === 'PUSH')) {
      this.plans.push({
        definitionVersion: 'V4',
        name: 'Default PUSH plan',
        description: 'Default push plan',
        mode: 'PUSH',
        validation: 'MANUAL',
      });
    }
  }

  onAddPlanClicked(selectedPlanMenuItem: PlanMenuItemVM) {
    this.planToEdit = undefined;
    this.selectedPlanMenuItem = selectedPlanMenuItem;
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
    const planFormType = plan.mode === 'PUSH' ? 'PUSH' : plan.security.type;
    this.selectedPlanMenuItem = AVAILABLE_PLANS_FOR_MENU.find((vm) => vm.planFormType === planFormType);
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
