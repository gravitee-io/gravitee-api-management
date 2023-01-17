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
import { FormBuilder, FormGroup } from '@angular/forms';

import { ApiCreationPayload } from '../../models/ApiCreationPayload';
import { API_CREATION_PAYLOAD, ApiCreationStepperService } from '../../models/ApiCreationStepperService';

/**
 * Wip component for the API creation wizard.
 * This component is used to init new steps.
 */
@Component({
  selector: 'api-creation-v4-step-wip',
  template: require('./api-creation-v4-step-wip.component.html'),
  styles: [require('./api-creation-v4-step-wip.component.scss')],
})
export class ApiCreationV4StepWipComponent implements OnInit {
  public form: FormGroup;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly stepper: ApiCreationStepperService,
    @Inject(API_CREATION_PAYLOAD) readonly currentStepPayload: ApiCreationPayload,
  ) {}

  ngOnInit(): void {
    this.form = this.formBuilder.group({});

    this.save();
  }

  save() {
    if (!this.currentStepPayload.lastStep) {
      this.stepper.goToNextStep(this.currentStepPayload);
    }
  }
}
