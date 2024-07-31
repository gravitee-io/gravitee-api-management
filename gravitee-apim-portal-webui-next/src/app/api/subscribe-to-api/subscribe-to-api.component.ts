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
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardActions, MatCardContent, MatCardHeader } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, catchError, combineLatestWith, EMPTY, map, Observable, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import {
  TermsAndConditionsDialogComponent,
  TermsAndConditionsDialogData,
} from './components/terms-and-conditions-dialog/terms-and-conditions-dialog.component';
import { SubscribeToApiCheckoutComponent } from './subscribe-to-api-checkout/subscribe-to-api-checkout.component';
import {
  ApplicationsPagination,
  SubscribeToApiChooseApplicationComponent,
} from './subscribe-to-api-choose-application/subscribe-to-api-choose-application.component';
import { SubscribeToApiChoosePlanComponent } from './subscribe-to-api-choose-plan/subscribe-to-api-choose-plan.component';
import { LoaderComponent } from '../../../components/loader/loader.component';
import { Api } from '../../../entities/api/api';
import { Application, ApplicationsResponse } from '../../../entities/application/application';
import { Page } from '../../../entities/page/page';
import { Plan } from '../../../entities/plan/plan';
import { CreateSubscription, Subscription } from '../../../entities/subscription/subscription';
import { SubscriptionsResponse } from '../../../entities/subscription/subscriptions-response';
import { ApiService } from '../../../services/api.service';
import { ApplicationService } from '../../../services/application.service';
import { PageService } from '../../../services/page.service';
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

interface CheckoutData {
  api: Api;
  sharedApiKeyModeDisabled: boolean;
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
  applicationApiKeyMode = signal<'EXCLUSIVE' | 'SHARED' | 'UNSPECIFIED' | null>(null);
  subscriptionInProgress = signal<boolean>(false);
  showApiKeyModeSelection = signal<boolean>(false);

  stepIsInvalid: Signal<boolean> = computed(() => {
    if (this.currentStep() === 1) {
      return this.currentPlan() === undefined;
    } else if (this.currentStep() === 2) {
      return this.currentApplication() === undefined;
    } else if (this.currentStep() === 3) {
      return (
        (this.currentPlan()?.comment_required === true && !this.message()) ||
        (this.showApiKeyModeSelection() && !this.applicationApiKeyMode())
      );
    }
    return false;
  });

  api$: Observable<Api> = of();
  plans$: Observable<Plan[]> = of();
  applicationsData$: Observable<ApplicationsData> = of();
  checkoutData$: Observable<CheckoutData> = of();
  currentApplication$ = toObservable(this.currentApplication);

  hasSubscriptionError: boolean = false;

  private currentApplicationsPage: BehaviorSubject<number> = new BehaviorSubject(1);
  private destroyRef = inject(DestroyRef);

  constructor(
    private planService: PlanService,
    private apiService: ApiService,
    private applicationService: ApplicationService,
    private subscriptionService: SubscriptionService,
    private pageService: PageService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private matDialog: MatDialog,
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

    this.checkoutData$ = this.api$.pipe(
      combineLatestWith(this.handleSharedApiKeyModeDisabled$()),
      map(([api, sharedApiKeyModeDisabled]) => {
        return { api, sharedApiKeyModeDisabled };
      }),
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

    const apiKeyMode = this.applicationApiKeyMode();

    const createSubscription: CreateSubscription = {
      application,
      plan,
      ...(this.message() ? { request: this.message() } : {}),
      ...(apiKeyMode ? { api_key_mode: apiKeyMode } : {}),
    };

    this.handleTermsAndConditions$(createSubscription)
      .pipe(
        switchMap(result => {
          if (!result.general_conditions_accepted && this.currentPlan()?.general_conditions) {
            return EMPTY;
          }
          return this.subscriptionService.subscribe(result);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ id }) => {
          this.router.navigate(['../', 'subscriptions', id], { relativeTo: this.activatedRoute });
        },
        error: err => {
          this.hasSubscriptionError = true;
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

  private handleTermsAndConditions$(createSubscription: CreateSubscription): Observable<CreateSubscription> {
    const generalConditionsPageId = this.currentPlan()?.general_conditions;
    if (generalConditionsPageId) {
      return this.pageService
        .getByApiIdAndId(this.apiId, generalConditionsPageId, true)
        .pipe(switchMap(page => this.handleTermsAndConditionsDialog$(this.apiId, page, createSubscription)));
    }
    return of(createSubscription);
  }

  private handleTermsAndConditionsDialog$(
    apiId: string,
    page: Page,
    createSubscription: CreateSubscription,
  ): Observable<CreateSubscription> {
    return this.matDialog
      .open<TermsAndConditionsDialogComponent, TermsAndConditionsDialogData, boolean>(TermsAndConditionsDialogComponent, {
        data: {
          page,
          apiId,
        },
      })
      .afterClosed()
      .pipe(
        map(accepted => ({
          ...createSubscription,
          general_conditions_accepted: accepted === true,
          general_conditions_content_revision: page.contentRevisionId,
        })),
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

  private handleSharedApiKeyModeDisabled$(): Observable<boolean> {
    return this.currentApplication$.pipe(
      switchMap(app => {
        if (!!app?.id && this.currentPlan()?.security === 'API_KEY' && app.api_key_mode !== 'EXCLUSIVE' && app.api_key_mode !== 'SHARED') {
          return this.subscriptionService.list({
            applicationId: app.id,
            statuses: ['PENDING', 'ACCEPTED', 'PAUSED'],
            size: -1,
          });
        }

        return of(undefined);
      }),
      map(response => {
        if (!response) {
          this.showApiKeyModeSelection.set(false);
          return false;
        }

        const existingApiKeySubscriptions = response.data.filter(s => response.metadata[s.plan]?.securityType === 'API_KEY');
        this.showApiKeyModeSelection.set(existingApiKeySubscriptions.length === 1);

        return existingApiKeySubscriptions.length === 1 && existingApiKeySubscriptions[0].api === this.apiId;
      }),
    );
  }
}
