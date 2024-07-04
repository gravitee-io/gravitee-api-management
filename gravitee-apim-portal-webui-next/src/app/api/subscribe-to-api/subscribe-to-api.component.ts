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
import { CommonModule } from '@angular/common';
import { Component, computed, Input, OnInit, Signal, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardActions, MatCardContent, MatCardHeader } from '@angular/material/card';
import { catchError, map, Observable } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { SubscribeToApiCheckoutComponent } from './subscribe-to-api-checkout/subscribe-to-api-checkout.component';
import { SubscribeToApiChooseApplicationComponent } from './subscribe-to-api-choose-application/subscribe-to-api-choose-application.component';
import { SubscribeToApiChoosePlanComponent } from './subscribe-to-api-choose-plan/subscribe-to-api-choose-plan.component';
import { LoaderComponent } from '../../../components/loader/loader.component';
import { Api } from '../../../entities/api/api';
import { Application } from '../../../entities/application/application';
import { Plan } from '../../../entities/plan/plan';
import { ApiService } from '../../../services/api.service';
import { PlanService } from '../../../services/plan.service';

@Component({
  selector: 'app-subscribe-to-api',
  imports: [
    CommonModule,
    MatCard,
    SubscribeToApiCheckoutComponent,
    SubscribeToApiChoosePlanComponent,
    SubscribeToApiChooseApplicationComponent,
    MatCardActions,
    MatCardHeader,
    MatCardContent,
    MatButton,
    LoaderComponent,
  ],
  templateUrl: './subscribe-to-api.component.html',
  styleUrl: './subscribe-to-api.component.scss',
  standalone: true,
})
export class SubscribeToApiComponent implements OnInit {
  @Input()
  apiId!: string;

  currentStep = signal(1);
  currentPlan = signal<Plan | undefined>(undefined);
  currentApplication = signal<Application | undefined>(undefined);

  stepIsInvalid: Signal<boolean> = computed(() => {
    if (this.currentStep() === 1) {
      return this.currentPlan() === undefined;
    } else if (this.currentStep() === 2) {
      return this.currentApplication() === undefined;
    }

    return false;
  });

  api$: Observable<Api> = of();
  plans$: Observable<Plan[]> = of();

  constructor(
    private planService: PlanService,
    private apiService: ApiService,
  ) {}

  ngOnInit(): void {
    this.plans$ = this.planService.list(this.apiId).pipe(
      map(({ data }) => data ?? []),
      catchError(err => {
        console.log(err);
        return of([]);
      }),
    );
    this.api$ = this.apiService.details(this.apiId).pipe(catchError(_ => of()));
  }

  goToNextStep(): void {
    if (this.currentPlan()?.security === 'KEY_LESS' && this.currentStep() === 1) {
      this.currentStep.set(3);
    } else if (this.currentStep() < 3) {
      this.currentStep.update(currentStep => currentStep + 1);
    }
  }

  goToPreviousStep(): void {
    if (this.currentPlan()?.security === 'KEY_LESS' && this.currentStep() === 3) {
      this.currentStep.set(1);
    } else if (this.currentStep() > 1) {
      this.currentStep.update(currentStep => currentStep - 1);
    }
  }

  subscribe() {
    // TODO: To be implemented
  }
}
