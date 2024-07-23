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
import { Component, computed, DestroyRef, inject, Input, OnInit, Signal, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardActions, MatCardContent, MatCardHeader } from '@angular/material/card';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, catchError, combineLatestWith, map, Observable, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { SubscribeToApiCheckoutComponent } from './subscribe-to-api-checkout/subscribe-to-api-checkout.component';
import {
  ApplicationsPagination,
  SubscribeToApiChooseApplicationComponent,
} from './subscribe-to-api-choose-application/subscribe-to-api-choose-application.component';
import { SubscribeToApiChoosePlanComponent } from './subscribe-to-api-choose-plan/subscribe-to-api-choose-plan.component';
import { LoaderComponent } from '../../../components/loader/loader.component';
import { Api } from '../../../entities/api/api';
import { Application, ApplicationsResponse } from '../../../entities/application/application';
import { Plan } from '../../../entities/plan/plan';
import { CreateSubscription, Subscription } from '../../../entities/subscription/subscription';
import { SubscriptionsResponse } from '../../../entities/subscription/subscriptions-response';
import { ApiService } from '../../../services/api.service';
import { ApplicationService } from '../../../services/application.service';
import { PlanService } from '../../../services/plan.service';
import { SubscriptionService } from '../../../services/subscription.service';

export interface ApplicationVM extends Application {
  disabled?: boolean;
  disabledMessage?: string;
}

interface ApplicationsData {
  applications: ApplicationVM[];
  pagination: ApplicationsPagination;
}

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
  message = signal<string>('');
  subscriptionInProgress = signal<boolean>(false);

  stepIsInvalid: Signal<boolean> = computed(() => {
    if (this.currentStep() === 1) {
      return this.currentPlan() === undefined;
    } else if (this.currentStep() === 2) {
      return this.currentApplication() === undefined;
    } else if (this.currentStep() === 3) {
      return this.currentPlan()?.comment_required === true && !this.message();
    }
    return false;
  });

  api$: Observable<Api> = of();
  plans$: Observable<Plan[]> = of();
  applicationsData$: Observable<ApplicationsData> = of();

  private currentApplicationsPage: BehaviorSubject<number> = new BehaviorSubject(1);
  private destroyRef = inject(DestroyRef);

  constructor(
    private planService: PlanService,
    private apiService: ApiService,
    private applicationService: ApplicationService,
    private subscriptionService: SubscriptionService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.plans$ = this.planService.list(this.apiId).pipe(
      map(({ data }) => data ?? []),
      catchError(_ => of([])),
    );
    this.api$ = this.apiService.details(this.apiId).pipe(catchError(_ => of()));

    this.applicationsData$ = this.subscriptionService.list({ apiId: this.apiId, statuses: ['PENDING', 'ACCEPTED'], size: -1 }).pipe(
      combineLatestWith(this.currentApplicationsPage),
      switchMap(([subscriptions, page]) => this.getApplicationsData$(page, subscriptions)),
      catchError(_ => of({ applications: [], pagination: { currentPage: 0, totalApplications: 0, start: 0, end: 0 } })),
    );
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

  onNextApplicationPage() {
    this.currentApplicationsPage.next(this.currentApplicationsPage.getValue() + 1);
  }

  onPreviousApplicationPage() {
    if (this.currentApplicationsPage.getValue() > 1) {
      this.currentApplicationsPage.next(this.currentApplicationsPage.getValue() - 1);
    }
  }

  subscribe() {
    this.subscriptionInProgress.set(true);

    const application = this.currentApplication()?.id;
    const plan = this.currentPlan()?.id;

    if (!application || !plan) {
      return;
    }

    const createSubscription: CreateSubscription = {
      application,
      plan,
      ...(this.message() ? { request: this.message() } : {}),
    };

    this.subscriptionService
      .subscribe(createSubscription)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ id }) => {
          this.router.navigate(['../', 'subscriptions', id], { relativeTo: this.activatedRoute });
        },
        error: err => {
          console.error(err);
          this.subscriptionInProgress.set(false);
        },
      });
  }

  private getApplicationsData$(page: number, subscriptionsResponse: SubscriptionsResponse): Observable<ApplicationsData> {
    return this.applicationService.list({ page, size: 9, forSubscriptions: true }).pipe(
      map(response => ({
        applications: this.addApplicationDisabledState(response, subscriptionsResponse),
        pagination: {
          currentPage: response.metadata?.pagination?.current_page ?? 0,
          totalApplications: response.metadata?.pagination?.total ?? 0,
          start: response.metadata?.pagination?.first ?? 0,
          end: response.metadata?.pagination?.last ?? 0,
        },
      })),
      tap(({ applications }) => {
        if (this.currentApplication() && applications.some(app => app.id === this.currentApplication()?.id && app.disabled === true)) {
          this.currentApplication.set(undefined);
        }
      }),
    );
  }
  private addApplicationDisabledState(applicationsResponse: ApplicationsResponse, subscriptions: SubscriptionsResponse): ApplicationVM[] {
    if (!applicationsResponse) {
      return [];
    }

    return applicationsResponse.data.map(application => {
      if (this.applicationHasExistingValidSubscriptionsForPlan(application, subscriptions.data)) {
        return { ...application, disabled: true, disabledMessage: 'A pending or accepted subscription already exists for this plan' };
      }
      if (this.applicationInSharedKeyModeHasExistingValidApiKeySubscriptionsForApi(application, subscriptions)) {
        return {
          ...application,
          disabled: true,
          disabledMessage: 'This application uses shared API keys and a pending or accepted API Key subscription already exists',
        };
      }
      return { ...application };
    });
  }

  private applicationHasExistingValidSubscriptionsForPlan(application: Application, subscriptions: Subscription[]): boolean {
    return subscriptions.some(
      s => s.plan === this.currentPlan()?.id && s.application === application.id && (s.status === 'PENDING' || s.status === 'ACCEPTED'),
    );
  }

  private applicationInSharedKeyModeHasExistingValidApiKeySubscriptionsForApi(
    application: Application,
    allValidApiSubscriptionsResponse: SubscriptionsResponse,
  ): boolean {
    if (application.api_key_mode !== 'SHARED' || this.currentPlan()?.security !== 'API_KEY') {
      return false;
    }
    return allValidApiSubscriptionsResponse.data.some(
      s =>
        s.application === application.id &&
        (s.status === 'ACCEPTED' || s.status === 'PENDING') &&
        allValidApiSubscriptionsResponse.metadata[s.plan]?.planMode === 'STANDARD' &&
        allValidApiSubscriptionsResponse.metadata[s.plan]?.securityType === 'API_KEY',
    );
  }
}
