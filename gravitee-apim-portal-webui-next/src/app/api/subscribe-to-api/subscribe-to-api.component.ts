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
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
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
import { ConfigService } from '../../../services/config.service';
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
  applicationApiKeySubscriptions: Subscription[];
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
    MatChipsModule,
    MatIcon,
  ],
  templateUrl: './subscribe-to-api.component.html',
  styleUrl: './subscribe-to-api.component.scss',
})
export class SubscribeToApiComponent implements OnInit {
  @Input() api!: Api;

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

  plans$: Observable<Plan[]> = of();
  applicationsData$: Observable<ApplicationsData> = of();
  checkoutData$: Observable<CheckoutData> = of();
  currentApplication$ = toObservable(this.currentApplication);

  hasSubscriptionError: boolean = false;

  private currentApplicationsPage: BehaviorSubject<number> = new BehaviorSubject(1);
  private destroyRef = inject(DestroyRef);
  private configuration = inject(ConfigService).configuration;

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
    this.plans$ = this.planService.list(this.api.id).pipe(
      map(({ data }) => data ?? []),
      catchError(_ => of([])),
    );

    this.applicationsData$ = this.subscriptionService.list({ apiId: this.api.id, statuses: ['PENDING', 'ACCEPTED'], size: -1 }).pipe(
      combineLatestWith(this.currentApplicationsPage),
      switchMap(([subscriptions, page]) => this.getApplicationsData$(page, subscriptions)),
      catchError(_ => of({ applications: [], pagination: { currentPage: 0, totalApplications: 0, start: 0, end: 0 } })),
    );

    this.checkoutData$ = this.handleCheckoutData$(this.api).pipe(
      tap(({ applicationApiKeySubscriptions }) => {
        this.showApiKeyModeSelection.set(
          this.configuration.plan?.security?.sharedApiKey?.enabled === true &&
            this.api.definitionVersion !== 'FEDERATED' &&
            applicationApiKeySubscriptions.length === 1,
        );
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
            this.subscriptionInProgress.set(false);
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
    return this.applicationService.list(page, 9, true).pipe(
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
        .getByApiIdAndId(this.api.id, generalConditionsPageId, true)
        .pipe(switchMap(page => this.handleTermsAndConditionsDialog$(this.api.id, page, createSubscription)));
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

      if (this.existingValidOAuth2OrJWTSubscription(application, subscriptions)) {
        return {
          ...application,
          disabled: true,
          disabledMessage: 'Already subscribed to an OAuth2 or JWT plan for this API',
        };
      }

      if (this.missingClientId(application)) {
        return {
          ...application,
          disabled: true,
          disabledMessage: 'Missing Client ID',
        };
      }

      if (this.existingValidMtlsSubscription(application, subscriptions)) {
        return {
          ...application,
          disabled: true,
          disabledMessage: 'Already subscribed to a mTLS plan for this API',
        };
      }

      if (this.missingTlsClientCertificate(application)) {
        return {
          ...application,
          disabled: true,
          disabledMessage: 'Missing TLS Client Certificate',
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

  private existingValidOAuth2OrJWTSubscription(application: Application, allValidApiSubscriptionsResponse: SubscriptionsResponse): boolean {
    if (this.currentPlan()?.security !== 'OAUTH2' && this.currentPlan()?.security !== 'JWT') {
      return false;
    }
    return allValidApiSubscriptionsResponse.data.some(
      s =>
        s.application === application.id &&
        (s.status === 'ACCEPTED' || s.status === 'PENDING' || s.status === 'PAUSED') &&
        (allValidApiSubscriptionsResponse.metadata[s.plan]?.securityType === 'OAUTH2' ||
          allValidApiSubscriptionsResponse.metadata[s.plan]?.securityType === 'JWT'),
    );
  }

  private missingClientId(application: Application): boolean {
    return (this.currentPlan()?.security === 'OAUTH2' || this.currentPlan()?.security === 'JWT') && application.hasClientId !== true;
  }

  private existingValidMtlsSubscription(application: Application, allValidApiSubscriptionsResponse: SubscriptionsResponse): boolean {
    if (this.currentPlan()?.security !== 'MTLS') {
      return false;
    }
    return allValidApiSubscriptionsResponse.data.some(
      s =>
        s.application === application.id &&
        (s.status === 'ACCEPTED' || s.status === 'PENDING' || s.status === 'PAUSED') &&
        allValidApiSubscriptionsResponse.metadata[s.plan]?.securityType === 'MTLS',
    );
  }

  private missingTlsClientCertificate(application: Application): boolean {
    return this.currentPlan()?.security === 'MTLS' && !application.settings?.tls?.client_certificate;
  }

  private handleCheckoutData$(api: Api): Observable<CheckoutData> {
    if (api.definitionVersion === 'FEDERATED') {
      return of({ api, applicationApiKeySubscriptions: [] });
    }
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
          return { applicationApiKeySubscriptions: [] };
        }

        const applicationApiKeySubscriptions = response.data.filter(s => response.metadata[s.plan]?.securityType === 'API_KEY');
        return { applicationApiKeySubscriptions };
      }),
    );
  }
}
