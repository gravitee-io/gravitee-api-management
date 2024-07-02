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
import { AsyncPipe } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { catchError, map, Observable } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { PlanCardComponent } from '../../../../components/subscribe/plan-card/plan-card.component';
import { getPlanSecurityTypeLabel, PlanUsageConfiguration } from '../../../../entities/plan/plan';
import { PlanService } from '../../../../services/plan.service';

export interface SubscriptionPlan {
  validation: string;
  name: string;
  security: string;
  authentication: string;
  usageConfiguration: PlanUsageConfiguration;
  id: string;
  isDisabled: boolean;
}

@Component({
  selector: 'app-subscribe-to-api-choose-plan',
  imports: [MatCardModule, PlanCardComponent, MatButton, AsyncPipe],
  templateUrl: './subscribe-to-api-choose-plan.component.html',
  styleUrl: './subscribe-to-api-choose-plan.component.scss',
  standalone: true,
})
export class SubscribeToApiChoosePlanComponent implements OnInit {
  @Input() plans!: SubscriptionPlan[];

  @Input() subscribeForm!: FormGroup;

  @Input() apiId!: string;

  subscriptionPlans$: Observable<SubscriptionPlan[]> = of([]);

  selectedPlan = new FormControl();

  constructor(private planService: PlanService) {}

  ngOnInit() {
    this.subscriptionPlans$ = this.loadPlans$();
  }

  onChangeStepper() {
    this.subscriptionPlans$
      .pipe(
        map(response => {
          response.map((plan: SubscriptionPlan) => {
            if (plan.id === this.selectedPlan.value) {
              this.subscribeForm.controls['plan'].setValue(plan);
              this.subscribeForm.controls['step'].setValue(
                this.subscribeForm.value.step + (this.skipNextStep(this.subscribeForm.value.plan.security) ? 2 : 1),
              );
            }
          });
        }),
      )
      .subscribe();
  }

  skipNextStep(authenticationType: string): boolean {
    return authenticationType === 'KEY_LESS';
  }

  loadPlans$(): Observable<SubscriptionPlan[]> {
    return this.planService.list(this.apiId).pipe(
      map(response => {
        if (response.data) {
          return response.data.map(plan => ({
            validation: plan.validation,
            name: plan.name,
            authentication: getPlanSecurityTypeLabel(plan.security ?? ''),
            security: plan.security,
            usageConfiguration: plan.usage_configuration ?? {},
            id: plan.id,
            isDisabled: plan.security !== 'KEY_LESS',
          }));
        } else return [];
      }),
      catchError(_ => of([])),
    );
  }
}
