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
import { CdkTrapFocus } from '@angular/cdk/a11y';
import { Component, computed, DestroyRef, inject, input, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatCard, MatCardActions, MatCardContent } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { filter, finalize, Observable, of, switchMap, tap } from 'rxjs';

import { BannerComponent } from '../../../components/banner/banner.component';
import { LoaderComponent } from '../../../components/loader/loader.component';
import { PlanCardComponent } from '../../../components/subscribe/plan-card/plan-card.component';
import {
  SubscriptionCommentDialogComponent,
  SubscriptionCommentDialogData,
} from '../../../components/subscription/subscription-comment-dialog/subscription-comment-dialog.component';
import { ConsumerConfigurationComponent } from '../../../components/subscription/webhook/consumer-configuration/consumer-configuration.component';
import { ConsumerConfigurationFormData } from '../../../components/subscription/webhook/consumer-configuration/consumer-configuration.models';
import { MobileClassDirective } from '../../../directives/mobile-class.directive';
import { NarrowClassDirective } from '../../../directives/narrow-class.directive';
import { ApiProduct } from '../../../entities/api-product/api-product';
import { Application, ApplicationsResponse } from '../../../entities/application/application';
import { Plan } from '../../../entities/plan/plan';
import { PlansResponse } from '../../../entities/plan/plans-response';
import { CreateSubscription, Subscription } from '../../../entities/subscription';
import { SubscriptionsResponse } from '../../../entities/subscription/subscriptions-response';
import { ApiProductsService } from '../../../services/api-products.service';
import { ApplicationService } from '../../../services/application.service';
import { CurrentUserService } from '../../../services/current-user.service';
import { SubscriptionService } from '../../../services/subscription.service';
import { SubscribeToApiStepHeaderComponent } from '../../api/subscribe-to-api/components/subscribe-to-api-step-header/subscribe-to-api-step-header.component';
import {
  ApplicationsPagination,
  ApplicationVM,
  DEFAULT_APPLICATIONS_PAGE_SIZE,
  SubscribeToApiChooseApplicationComponent,
} from '../../api/subscribe-to-api/subscribe-to-api-choose-application/subscribe-to-api-choose-application.component';
import { CreateApplicationComponent } from '../../dashboard/applications/create-application/create-application.component';

export enum SubscribeToApiProductStep {
  PLAN_SELECTION = 'PLAN_SELECTION',
  APP_SELECTION = 'APP_SELECTION',
  PUSH_DETAILS = 'PUSH_DETAILS',
  REVIEW = 'REVIEW',
}

interface ApplicationsData {
  applications: ApplicationVM[];
  pagination: ApplicationsPagination;
}

interface ApplicationsParams {
  page: number;
  pageSize: number;
  planId: string;
  query: string;
  reload: number;
}

@Component({
  selector: 'app-subscribe-to-api-product',
  imports: [
    BannerComponent,
    CdkTrapFocus,
    ConsumerConfigurationComponent,
    CreateApplicationComponent,
    LoaderComponent,
    MatButton,
    MatCard,
    MatCardActions,
    MatCardContent,
    MatChipsModule,
    MatIcon,
    MatIconButton,
    MobileClassDirective,
    NarrowClassDirective,
    PlanCardComponent,
    SubscribeToApiChooseApplicationComponent,
    SubscribeToApiStepHeaderComponent,
  ],
  templateUrl: './subscribe-to-api-product.component.html',
  styleUrl: './subscribe-to-api-product.component.scss',
})
export class SubscribeToApiProductComponent {
  private readonly apiProductsService = inject(ApiProductsService);
  private readonly applicationService = inject(ApplicationService);
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly matDialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  private readonly currentUser = inject(CurrentUserService).user;

  readonly apiProduct = input.required<ApiProduct>();
  readonly cancelFn = input<() => void>();

  readonly SubscribeStep = SubscribeToApiProductStep;
  readonly currentStep = signal(SubscribeToApiProductStep.PLAN_SELECTION);
  readonly currentPlan = signal<Plan | undefined>(undefined);
  readonly currentApplication = signal<Application | undefined>(undefined);
  readonly currentApplicationsPage = signal(1);
  readonly currentApplicationsPageSize = signal(DEFAULT_APPLICATIONS_PAGE_SIZE);
  readonly applicationsSearchQuery = signal('');
  readonly applicationsReload = signal(0);
  readonly createApplicationOpen = signal(false);
  readonly canCreateApplication = computed(() => this.currentUser().permissions?.APPLICATION?.includes('C') ?? false);
  readonly consumerConfigurationFormData = signal<ConsumerConfigurationFormData>({ value: undefined, isValid: false });
  readonly isSubmitting = signal(false);
  readonly hasSubscriptionError = signal(false);
  readonly createdSubscription = signal<Subscription | undefined>(undefined);

  readonly activeSteps = computed(() => {
    const steps = [SubscribeToApiProductStep.PLAN_SELECTION];
    const plan = this.currentPlan();

    if (plan?.security !== 'KEY_LESS') {
      steps.push(SubscribeToApiProductStep.APP_SELECTION);
      if (plan?.mode === 'PUSH') {
        steps.push(SubscribeToApiProductStep.PUSH_DETAILS);
      }
    }

    steps.push(SubscribeToApiProductStep.REVIEW);
    return steps;
  });

  readonly plansResource = rxResource<PlansResponse, string>({
    params: () => this.apiProduct().id,
    stream: ({ params }) => this.apiProductsService.listPlans(params),
  });

  readonly subscriptionsResource = rxResource<SubscriptionsResponse, string>({
    params: () => this.apiProduct().id,
    stream: ({ params }) =>
      this.subscriptionService.list({
        apiProductIds: [params],
        statuses: ['PENDING', 'ACCEPTED', 'PAUSED'],
        size: -1,
      }),
  });

  readonly applicationsResource = rxResource<ApplicationsResponse | undefined, ApplicationsParams | null>({
    params: () => {
      const plan = this.currentPlan();
      return plan && plan.security !== 'KEY_LESS'
        ? {
            page: this.currentApplicationsPage(),
            pageSize: this.currentApplicationsPageSize(),
            planId: plan.id,
            query: this.applicationsSearchQuery(),
            reload: this.applicationsReload(),
          }
        : null;
    },
    stream: ({ params }) => {
      if (!params) {
        return of<ApplicationsResponse | undefined>(undefined);
      }
      const trimmedQuery = params.query?.trim();
      // Older Management APIs ignore `query` when forSubscription=true — fetch a wide page for local filter.
      const fetchSize = trimmedQuery ? Math.max(params.pageSize * 50, 200) : params.pageSize;
      const fetchPage = trimmedQuery ? 1 : params.page;
      return this.applicationService.list(fetchPage, fetchSize, true, trimmedQuery || undefined);
    },
  });

  readonly plans = computed(() => this.plansResource.value()?.data ?? []);
  readonly applicationsData = computed<ApplicationsData>(() => {
    const response = this.applicationsResource.value();
    const subscriptions = this.subscriptionsResource.value();
    const pageSize = this.currentApplicationsPageSize();
    const page = this.currentApplicationsPage();
    const trimmedQuery = this.applicationsSearchQuery().trim();

    if (!response || !subscriptions) {
      return {
        applications: [],
        pagination: { currentPage: page, totalApplications: 0, pageSize },
      };
    }

    let applications = this.addApplicationDisabledState(response, subscriptions);
    if (trimmedQuery) {
      const normalizedQuery = trimmedQuery.toLowerCase();
      applications = applications.filter(
        app => app.name?.toLowerCase().includes(normalizedQuery) || app.id?.toLowerCase() === normalizedQuery,
      );
      const start = (page - 1) * pageSize;
      return {
        applications: applications.slice(start, start + pageSize),
        pagination: {
          currentPage: page,
          totalApplications: applications.length,
          pageSize,
        },
      };
    }

    return {
      applications,
      pagination: {
        currentPage: response.metadata?.pagination?.current_page ?? 1,
        totalApplications: response.metadata?.pagination?.total ?? 0,
        pageSize,
      },
    };
  });
  readonly isApplicationStepLoading = computed(() => this.applicationsResource.isLoading() || this.subscriptionsResource.isLoading());
  readonly hasApplicationStepError = computed(() => !!this.applicationsResource.error() || !!this.subscriptionsResource.error());
  readonly stepIsInvalid = computed(() => {
    switch (this.currentStep()) {
      case SubscribeToApiProductStep.PLAN_SELECTION:
        return !this.currentPlan();
      case SubscribeToApiProductStep.APP_SELECTION:
        return !this.currentApplication() || this.hasApplicationStepError();
      case SubscribeToApiProductStep.PUSH_DETAILS:
        return !this.consumerConfigurationFormData().isValid;
      default:
        return false;
    }
  });

  selectPlan(plan: Plan): void {
    if (this.currentPlan()?.id !== plan.id) {
      this.currentApplication.set(undefined);
      this.currentApplicationsPage.set(1);
      this.consumerConfigurationFormData.set({ value: undefined, isValid: false });
    }
    this.currentPlan.set(plan);
  }

  selectApplication(application: Application): void {
    this.currentApplication.set(application);
  }

  consumerConfigurationFormChanges(data: ConsumerConfigurationFormData): void {
    this.consumerConfigurationFormData.set(data);
  }

  stepNumberOf(step: SubscribeToApiProductStep): number {
    return this.activeSteps().indexOf(step) + 1;
  }

  goToNextStep(): void {
    const steps = this.activeSteps();
    const currentIndex = steps.indexOf(this.currentStep());
    if (currentIndex < steps.length - 1) {
      this.currentStep.set(steps[currentIndex + 1]);
    }
  }

  goToPreviousStep(): void {
    const steps = this.activeSteps();
    const currentIndex = steps.indexOf(this.currentStep());
    if (currentIndex > 0) {
      this.currentStep.set(steps[currentIndex - 1]);
    }
  }

  onApplicationPageChange(page: number): void {
    this.currentApplicationsPage.set(page);
  }

  onApplicationPageSizeChange(pageSize: number): void {
    this.currentApplicationsPageSize.set(pageSize);
    this.currentApplicationsPage.set(1);
  }

  onApplicationsSearch(query: string): void {
    this.applicationsSearchQuery.set(query);
    this.currentApplicationsPage.set(1);
  }

  openCreateApplication(): void {
    this.createApplicationOpen.set(true);
  }

  closeCreateApplication(): void {
    this.createApplicationOpen.set(false);
  }

  onApplicationCreated(application: Application): void {
    this.createApplicationOpen.set(false);
    this.currentApplication.set(application);
    this.applicationsSearchQuery.set('');
    this.currentApplicationsPage.set(1);
    this.applicationsReload.update(value => value + 1);
  }

  retryPlans(): void {
    this.plansResource.reload();
  }

  retryApplications(): void {
    this.subscriptionsResource.reload();
    this.applicationsResource.reload();
  }

  subscribe(): void {
    const application = this.currentApplication()?.id;
    const plan = this.currentPlan();
    if (!plan || plan.security === 'KEY_LESS' || !application || this.isSubmitting()) {
      return;
    }

    this.requestComment$(plan)
      .pipe(
        filter((request): request is string | null => request !== undefined),
        tap(() => {
          this.hasSubscriptionError.set(false);
          this.isSubmitting.set(true);
        }),
        switchMap(request => {
          const createSubscription: CreateSubscription = {
            application,
            plan: plan.id,
            ...this.toConsumerConfiguration(plan),
            ...(request?.trim() ? { request: request.trim() } : {}),
          };
          return this.subscriptionService.subscribe(createSubscription);
        }),
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: subscription => this.createdSubscription.set(subscription),
        error: error => {
          console.error(error);
          this.hasSubscriptionError.set(true);
        },
      });
  }

  private requestComment$(plan: Plan): Observable<string | null | undefined> {
    if (!plan.comment_required && !plan.comment_question) {
      return of(null);
    }

    return this.matDialog
      .open<SubscriptionCommentDialogComponent, SubscriptionCommentDialogData, string>(SubscriptionCommentDialogComponent, {
        data: { plan },
        width: '500px',
      })
      .afterClosed();
  }

  private toConsumerConfiguration(plan: Plan): Pick<CreateSubscription, 'configuration'> {
    const formValue = this.consumerConfigurationFormData().value;
    if (plan.mode !== 'PUSH' || !formValue) {
      return {};
    }

    return {
      configuration: {
        entrypointId: 'webhook',
        channel: formValue.channel,
        entrypointConfiguration: {
          ...formValue.consumerConfiguration,
          headers: formValue.consumerConfiguration.headers ?? [],
        },
      },
    };
  }

  private addApplicationDisabledState(
    applicationsResponse: ApplicationsResponse,
    subscriptionsResponse: SubscriptionsResponse,
  ): ApplicationVM[] {
    const activeProductSubscriptions = subscriptionsResponse.data.filter(
      subscription =>
        subscription.reference_type === 'API_PRODUCT' &&
        subscription.reference_id === this.apiProduct().id &&
        ['PENDING', 'ACCEPTED', 'PAUSED'].includes(subscription.status),
    );

    return applicationsResponse.data.map(application => {
      if (
        activeProductSubscriptions.some(
          subscription => subscription.plan === this.currentPlan()?.id && subscription.application === application.id,
        )
      ) {
        return {
          ...application,
          disabled: true,
          disabledMessage: $localize`:@@subscribeToApiProductExistingPlanSubscription:A subscription already exists for this plan`,
        };
      }

      if (this.hasIncompatibleSecuritySubscription(application, activeProductSubscriptions, subscriptionsResponse)) {
        return this.toDisabledApplication(application);
      }

      if (this.isClientIdMissing(application)) {
        return {
          ...application,
          disabled: true,
          disabledMessage: $localize`:@@subscribeToApiProductMissingClientId:Missing Client ID`,
        };
      }

      if (this.isClientCertificateMissing(application)) {
        return {
          ...application,
          disabled: true,
          disabledMessage: $localize`:@@subscribeToApiProductMissingCertificate:Missing TLS Client Certificate`,
        };
      }

      return application;
    });
  }

  private hasIncompatibleSecuritySubscription(
    application: Application,
    subscriptions: Subscription[],
    response: SubscriptionsResponse,
  ): boolean {
    const security = this.currentPlan()?.security;
    return subscriptions.some(subscription => {
      if (subscription.application !== application.id) {
        return false;
      }

      const existingSecurity = response.metadata[subscription.plan]?.securityType;
      if (security === 'API_KEY' && application.api_key_mode === 'SHARED') {
        return existingSecurity === 'API_KEY';
      }
      if (security === 'JWT' || security === 'OAUTH2') {
        return existingSecurity === 'JWT' || existingSecurity === 'OAUTH2';
      }
      return security === 'MTLS' && existingSecurity === 'MTLS';
    });
  }

  private toDisabledApplication(application: Application): ApplicationVM {
    const security = this.currentPlan()?.security;
    if (security === 'API_KEY') {
      return {
        ...application,
        disabled: true,
        disabledMessage: $localize`:@@subscribeToApiProductExistingSharedApiKeySubscription:This application uses shared API keys and already has an active API Key subscription`,
      };
    }
    if (security === 'MTLS') {
      return {
        ...application,
        disabled: true,
        disabledMessage: $localize`:@@subscribeToApiProductExistingMtlsSubscription:Already subscribed to an mTLS plan for this API Product`,
      };
    }
    return {
      ...application,
      disabled: true,
      disabledMessage: $localize`:@@subscribeToApiProductExistingOAuthJwtSubscription:Already subscribed to an OAuth2 or JWT plan for this API Product`,
    };
  }

  private isClientIdMissing(application: Application): boolean {
    const security = this.currentPlan()?.security;
    return (security === 'OAUTH2' || security === 'JWT') && application.hasClientId !== true;
  }

  private isClientCertificateMissing(application: Application): boolean {
    return this.currentPlan()?.security === 'MTLS' && !application.settings?.tls?.client_certificate;
  }
}
