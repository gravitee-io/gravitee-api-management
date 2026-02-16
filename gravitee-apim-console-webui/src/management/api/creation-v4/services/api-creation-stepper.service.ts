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
import { Inject, Injectable, Type } from '@angular/core';
import { ReplaySubject, Subject } from 'rxjs';
import { cloneDeep, toNumber } from 'lodash';

import { ApiCreationPayload } from '../models/ApiCreationPayload';

export interface ApiCreationGroup {
  groupNumber: number;
  label: string;
  menuItemComponent?: Type<unknown>;
}

export type NewApiCreationStep = {
  groupNumber: number;
  component: Type<unknown>;
};

export interface ApiCreationStep {
  id: string;
  group: ApiCreationGroup;
  component: Type<unknown>;
  state: 'initial' | 'valid' | 'invalid';
  patchPayload: (lastPayload: ApiCreationPayload) => ApiCreationPayload;
}

/**
 * Service that manages the steps of the API creation process.
 */
@Injectable()
export class ApiCreationStepperService {
  private steps: ApiCreationStep[] = [];
  private currentStepIndex = -1;
  private initialPayload: ApiCreationPayload = {};

  /**
   * Observable that emits an event every time the current step changes.
   */
  public currentStep$ = new ReplaySubject<ApiCreationStep>(1);

  /**
   * Observable that emits an event every time the steps change. Emit current steps to new subscribers.
   * This is useful when the steps are dynamically added or removed.
   */
  public steps$ = new ReplaySubject<ApiCreationStep[]>(1);

  /**
   * Observable that emits an event every time the creation stepper is finished.
   * Emit the final payload.
   */
  public finished$ = new Subject<ApiCreationPayload>();

  constructor(
    @Inject('isFactory') public readonly groups: ApiCreationGroup[],
    @Inject('isFactory') initialPayload?: ApiCreationPayload,
  ) {
    this.initialPayload = initialPayload ?? {};
    this.groups = groups ?? [];
  }

  /**
   * This function compile the data for a specific step of the API creation process by using the data from previous steps
   *
   * @param {ApiCreationStep} step - the current step of the API creation process
   * @returns {ApiCreationPayload} - the data for the current step
   */
  public compileStepPayload(step: ApiCreationStep): ApiCreationPayload {
    // Get all the previous steps of the current step
    const previousSteps = this.steps.slice(0, this.steps.indexOf(step));
    // Compute the data for the current step by using reduce method on the previous steps
    // The reduce method is used to combine all the data from the previous steps using the patchPayload method
    // The initial payload is used as the starting value for the reduce method
    return [...previousSteps, step].reduce((payload, previousStep) => previousStep.patchPayload(payload), this.initialPayload);
  }

  /**
   * Add a next step and go to it.
   */
  public goToNextStep(step: NewApiCreationStep) {
    const currentStep = this.steps[this.currentStepIndex];
    const nextStep = this.steps[this.currentStepIndex + 1];
    const group = this.groups.find(({ groupNumber }) => groupNumber === step.groupNumber);

    // Add new step if is not already added
    const nextStepNumber = currentStep?.group?.groupNumber === step.groupNumber ? currentStep.id.split('-').pop() : 0;
    const stepId = `step-${group.groupNumber}-${toNumber(nextStepNumber) + 1}`;

    if (!nextStep || nextStep.id !== stepId) {
      this.steps.splice(this.currentStepIndex + 1, 0, {
        component: step.component,
        group: group,
        id: stepId,
        state: 'initial',
        patchPayload: p => p,
      });
    }

    // Go to next step
    this.goToStepIndex(this.currentStepIndex + 1);
    this.steps$.next(this.steps);
  }

  public validStep(patchPayload: ApiCreationStep['patchPayload']) {
    const currentStep = this.steps[this.currentStepIndex];

    // Save payload to current step & force new object mutation for updated payload
    currentStep.patchPayload = lastPayload => patchPayload(cloneDeep(lastPayload));
    currentStep.state = 'valid';
  }

  public finishStepper() {
    const lastStep = this.steps[this.steps.length - 1];
    this.finished$.next(this.compileStepPayload(lastStep));
  }

  public goToPreviousStep() {
    this.goToStepIndex(this.currentStepIndex - 1);
  }

  public goToStepLabel(label: string) {
    const stepIndex = this.steps.findIndex(step => step.group.label === label);
    if (stepIndex === -1) {
      throw new Error('Step not found: ' + label);
    }
    this.goToStepIndex(stepIndex);
  }

  private goToStepIndex(index: number) {
    if (index < 0 || index >= this.steps.length) {
      throw new Error('Step index is out of bounds: ' + index);
    }
    this.currentStepIndex = index;
    this.currentStep$.next(this.steps[this.currentStepIndex]);
  }

  removeAllNextSteps() {
    this.steps.splice(this.currentStepIndex + 1);
  }

  invalidateAllNextSteps() {
    this.steps.forEach((step, index) => {
      if (index > this.currentStepIndex) {
        step.state = 'invalid';
      }
    });
  }
}
