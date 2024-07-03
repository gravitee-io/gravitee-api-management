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
import { Component, Input } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatCard, MatCardContent, MatCardFooter, MatCardHeader } from '@angular/material/card';
import { MatRadioButton, MatRadioGroup } from '@angular/material/radio';
import { MatTooltip } from '@angular/material/tooltip';

import { SubscriptionPlan } from '../../../app/api/subscribe-to-api/subscribe-to-api-choose-plan/subscribe-to-api-choose-plan.component';
import { ToPeriodTimeUnitLabelPipe } from '../../../pipe/time-unit.pipe';

@Component({
  selector: 'app-plan-card',
  standalone: true,
  imports: [
    MatCard,
    MatCardHeader,
    MatRadioButton,
    MatRadioGroup,
    ReactiveFormsModule,
    MatCardContent,
    MatCardFooter,
    MatTooltip,
    ToPeriodTimeUnitLabelPipe,
  ],
  templateUrl: './plan-card.component.html',
  providers: [ToPeriodTimeUnitLabelPipe],
  styleUrl: './plan-card.component.scss',
})
export class PlanCardComponent {
  @Input() selectedPlanControl = new FormControl();

  @Input() card!: SubscriptionPlan;

  isPlanSelected = false;

  selectPlan() {
    if (!this.card.isDisabled) {
      this.selectedPlanControl.setValue(this.card.id);
      this.isPlanSelected = true;
    }
  }

  isSelected() {
    return this.selectedPlanControl.value === this.card.id;
  }
}
