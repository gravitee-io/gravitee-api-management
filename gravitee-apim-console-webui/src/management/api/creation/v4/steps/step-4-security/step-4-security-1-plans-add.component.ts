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
import { FormControl, FormGroup } from '@angular/forms';

import { SecurityPlan } from './step-4-security-1-plans-list.component';

import { PlanSecurity, PlanSecurityType } from '../../../../../../entities/plan-v4';

@Component({
  selector: 'step-4-security-1-plans-add',
  template: require('./step-4-security-1-plans-add.component.html'),
  styles: [require('./step-4-security-1-plans-add.component.scss'), require('../api-creation-steps-common.component.scss')],
})
export class Step4Security1PlansAddComponent {
  @Input()
  plan: PlanSecurity;

  @Output()
  planChanges = new EventEmitter<SecurityPlan>();

  @Output()
  exitPlanCreation = new EventEmitter<void>();

  public form = new FormGroup({
    plan: new FormControl(),
  });

  save(): void {
    // TODO: add real plan
    this.planChanges.next({
      name: 'Default Keyless (UNSECURED)',
      type: PlanSecurityType.KEY_LESS,
      description: 'Default unsecured plan',
    });
  }

  onExitPlanCreation(): void {
    this.exitPlanCreation.emit();
  }
}
