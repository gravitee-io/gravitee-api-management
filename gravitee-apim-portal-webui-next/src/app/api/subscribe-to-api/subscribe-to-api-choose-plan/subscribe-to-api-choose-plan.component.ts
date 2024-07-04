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
import { Component, computed, EventEmitter, Input, Output, Signal, WritableSignal } from '@angular/core';

import { PlanCardComponent } from '../../../../components/subscribe/plan-card/plan-card.component';
import { Plan } from '../../../../entities/plan/plan';

@Component({
  selector: 'app-subscribe-to-api-choose-plan',
  imports: [PlanCardComponent],
  templateUrl: './subscribe-to-api-choose-plan.component.html',
  styleUrl: './subscribe-to-api-choose-plan.component.scss',
  standalone: true,
})
export class SubscribeToApiChoosePlanComponent {
  @Input() plans!: Plan[];

  @Input()
  selectedPlan!: WritableSignal<Plan | undefined>;

  @Output()
  selectPlan = new EventEmitter<Plan>();

  selectedPlanId: Signal<string> = computed(() => this.selectedPlan()?.id ?? '');
}
