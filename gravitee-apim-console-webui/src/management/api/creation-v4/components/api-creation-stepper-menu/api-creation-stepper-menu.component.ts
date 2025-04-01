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
import { Component, EventEmitter, InjectionToken, Input, Output } from '@angular/core';

import { ApiCreationPayload } from '../../models/ApiCreationPayload';
import { ApiCreationGroup, ApiCreationStep } from '../../services/api-creation-stepper.service';

export const MENU_ITEM_PAYLOAD = new InjectionToken('MENU_ITEM_PAYLOAD');

export interface MenuStepItem extends ApiCreationGroup {
  state: 'initial' | 'valid' | 'invalid';
  payload: ApiCreationPayload;
}

@Component({
  selector: 'api-creation-stepper-menu',
  templateUrl: './api-creation-stepper-menu.component.html',
  styleUrls: ['./api-creation-stepper-menu.component.scss'],
  standalone: false,
})
export class ApiCreationStepperMenuComponent {
  @Input()
  public menuSteps: MenuStepItem[];

  @Input()
  public currentStep: ApiCreationStep;

  @Output()
  goToStep = new EventEmitter<string>();
}
