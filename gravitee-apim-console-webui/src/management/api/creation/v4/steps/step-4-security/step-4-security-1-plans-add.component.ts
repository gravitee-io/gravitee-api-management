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

import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';

import { NewPlan } from '../../../../../../entities/plan-v4';
import { PlanSecurityVM } from '../../../../../../services-ngx/constants.service';

@Component({
  selector: 'step-4-security-1-plans-add',
  template: require('./step-4-security-1-plans-add.component.html'),
  styles: [require('./step-4-security-1-plans-add.component.scss'), require('../api-creation-steps-common.component.scss')],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Step4Security1PlansAddComponent implements OnInit {
  @Input()
  plan?: NewPlan;

  @Input()
  securityType: PlanSecurityVM;

  @Output()
  planChanges = new EventEmitter<NewPlan>();

  @Output()
  exitPlanCreation = new EventEmitter<void>();

  public form: FormGroup;

  ngOnInit() {
    this.form = new FormGroup({
      plan: new FormControl(this.plan),
    });
  }

  onAddPlan(): void {
    this.planChanges.next(this.form.get('plan').value);
  }

  onExitPlanCreation(): void {
    this.exitPlanCreation.emit();
  }
}
