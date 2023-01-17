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
import { Injectable, InjectionToken, Type } from '@angular/core';
import { Subject } from 'rxjs';
import { cloneDeep } from 'lodash';

import { ApiCreationPayload } from './ApiCreationPayload';

export const API_CREATION_PAYLOAD = new InjectionToken<ApiCreationPayload>('API_CREATION_PAYLOAD');

export interface ApiCreationStep {
  number: number;
  component: Type<unknown>;
  payload: ApiCreationPayload;
  type: 'primary' | 'secondary';
}

type ApiCreationStepPayload = Omit<ApiCreationStep, 'type' | 'payload'> & { payload?: ApiCreationPayload };

@Injectable()
export class ApiCreationStepperService {
  private steps: ApiCreationStep[];

  private currentStepIndex = 0;

  public currentStep$ = new Subject<ApiCreationStep>();

  constructor(payload: ApiCreationStepPayload[]) {
    this.steps = payload.map((stepPayload) => ({
      payload: {},
      ...stepPayload,
      type: 'primary',
    }));
  }

  goToStep(index: number, payload?: ApiCreationPayload) {
    if (index < 0 || index >= this.steps.length) {
      throw new Error('Step index is out of bounds: ' + index);
    }
    this.currentStepIndex = index;
    if (payload) {
      this.steps[this.currentStepIndex].payload = payload;
    }
    this.currentStep$.next(this.steps[this.currentStepIndex]);
  }

  goToNextStep(_payload: ApiCreationPayload) {
    // Force new object mutation for updated payload
    const payload = cloneDeep(_payload);

    // Save payload to current step
    this.steps[this.currentStepIndex].payload = payload;

    // Give current payload to next step
    this.goToStep(this.currentStepIndex + 1, payload);
  }

  goToPreviousStep() {
    this.goToStep(this.currentStepIndex - 1);
  }
}
