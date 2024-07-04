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
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { MatCard, MatCardContent, MatCardFooter, MatCardHeader } from '@angular/material/card';
import { MatRadioButton, MatRadioGroup } from '@angular/material/radio';
import { MatTooltip } from '@angular/material/tooltip';

import { getPlanSecurityTypeLabel, Plan } from '../../../entities/plan/plan';
import { ToPeriodTimeUnitLabelPipe } from '../../../pipe/time-unit.pipe';

@Component({
  selector: 'app-plan-card',
  standalone: true,
  imports: [MatCard, MatCardHeader, MatRadioButton, MatRadioGroup, MatCardContent, MatCardFooter, MatTooltip, ToPeriodTimeUnitLabelPipe],
  templateUrl: './plan-card.component.html',
  providers: [ToPeriodTimeUnitLabelPipe],
  styleUrl: './plan-card.component.scss',
})
export class PlanCardComponent implements OnInit {
  @Input() plan!: Plan;

  @Input()
  selected: boolean = false;

  @Input()
  disabled: boolean = false;

  @Output()
  selectPlan = new EventEmitter<Plan>();

  authentication: string = '';

  ngOnInit() {
    this.authentication = getPlanSecurityTypeLabel(this.plan.security);
  }

  onSelectPlan() {
    if (!this.disabled) {
      this.selectPlan.emit(this.plan);
    }
  }
}
