/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Component, computed, EventEmitter, input, Input, Output } from '@angular/core';

import { ApiType } from '../../../entities/api/api';
import { getPlanSecurityTypeLabel, Plan, PlanSecurityEnum } from '../../../entities/plan/plan';
import { ToPeriodTimeUnitLabelPipe } from '../../../pipe/time-unit.pipe';
import { RadioCardComponent } from '../../radio-card/radio-card.component';

@Component({
  selector: 'app-plan-card',
  imports: [ToPeriodTimeUnitLabelPipe, RadioCardComponent],
  templateUrl: './plan-card.component.html',
  providers: [ToPeriodTimeUnitLabelPipe],
  styleUrl: './plan-card.component.scss',
})
export class PlanCardComponent {
  @Input()
  apiType?: ApiType;

  @Input()
  selected: boolean = false;

  @Input()
  disabled: boolean = false;

  @Output()
  selectPlan = new EventEmitter<Plan>();

  plan = input.required<Plan>();

  authentication = computed(() => {
    if (this.apiType === 'NATIVE') {
      return this.getNativeSecurityLabel(this.plan().security);
    }
    return getPlanSecurityTypeLabel(this.plan().security);
  });

  onSelectPlan() {
    if (!this.disabled) {
      this.selectPlan.emit(this.plan());
    }
  }

  private getNativeSecurityLabel(planSecurity: PlanSecurityEnum): string {
    switch (planSecurity) {
      case 'API_KEY':
        return 'SASL/SSL with SASL mechanisms PLAIN, SCRAM-256, and SCRAM-512';
      case 'JWT':
      case 'OAUTH2':
        return 'SASL/SSL with SASL mechanism OAUTHBEARER';
      default:
        return 'SSL';
    }
  }
}
