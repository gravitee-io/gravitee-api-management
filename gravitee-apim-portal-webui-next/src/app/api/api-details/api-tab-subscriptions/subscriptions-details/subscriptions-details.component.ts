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
import { Component, DestroyRef, Input, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, filter, forkJoin, map, Observable, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { SubscriptionConsumerConfigurationComponent } from './subscription-consumer-configuration';
import { ApiAccessComponent } from '../../../../../components/api-access/api-access.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../../components/confirm-dialog/confirm-dialog.component';
import { LoaderComponent } from '../../../../../components/loader/loader.component';
import { SubscriptionInfoComponent } from '../../../../../components/subscription-info/subscription-info.component';
import { Api } from '../../../../../entities/api/api';
import { Application } from '../../../../../entities/application/application';
import { UserApiPermissions } from '../../../../../entities/permission/permission';
import { PlanMode, PlanSecurityEnum, PlanUsageConfiguration } from '../../../../../entities/plan/plan';
import {
  SubscriptionConsumerStatusEnum,
  SubscriptionConsumerConfiguration,
  SubscriptionsResponse,
  Subscription,
} from '../../../../../entities/subscription';
import { CapitalizeFirstPipe } from '../../../../../pipe/capitalize-first.pipe';
import { ApiService } from '../../../../../services/api.service';
import { ApplicationService } from '../../../../../services/application.service';
import { PermissionsService } from '../../../../../services/permissions.service';
import { PlanService } from '../../../../../services/plan.service';
import { SubscriptionService } from '../../../../../services/subscription.service';

interface SubscriptionDetailsVM {
  result?: SubscriptionDetailsData;
  error?: boolean;
}

interface SubscriptionDetailsData {
  application: Application;
  planName: string;
  planSecurity: PlanSecurityEnum;
  planUsageConfiguration: PlanUsageConfiguration;
  subscription: Subscription;
  consumerStatus: SubscriptionConsumerStatusEnum;
  failureCause?: string;
  createdAt?: string;
  updatedAt?: string;
  apiKey?: string;
  apiKeyConfigUsername?: string;
  entrypointUrls?: string[];
  clientId?: string;
  clientSecret?: string;
  api?: Api;
  consumerConfiguration?: SubscriptionConsumerConfiguration;
}

@Component({
  imports: [
    MatIcon,
    MatCardModule,
    RouterLink,
    AsyncPipe,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    AsyncPipe,
    ApiAccessComponent,
    SubscriptionInfoComponent,
    LoaderComponent,
    SubscriptionConsumerConfigurationComponent,
  ],
  providers: [CapitalizeFirstPipe],
  selector: 'app-subscriptions-details',
  styleUrl: './subscriptions-details.component.scss',
  templateUrl: './subscriptions-details.component.html',
})
export class SubscriptionsDetailsComponent implements OnInit {
  @Input()
  apiId!: string;

  @Input()
  subscriptionId!: string;

  _subscriptionDetails = new BehaviorSubject<boolean>(true);

  subscriptionDetails$: Observable<SubscriptionDetailsVM> = of();

  isLoadingStatus: boolean = true;

  constructor(
    private subscriptionService: SubscriptionService,
    private apiService: ApiService,
    private applicationService: ApplicationService,
    private permissionsService: PermissionsService,
    private destroyRef: DestroyRef,
    private planService: PlanService,
    public dialog: MatDialog,
  ) {}

  ngOnInit() {
    this.subscriptionDetails$ = this.loadDetails();
  }

  closeSubscription() {
    const dialogData: ConfirmDialogData = {
      title: $localize`:@@titleCancelSubscriptionDialog:Close this subscription?`,
      content: $localize`:@@contentCancelSubscriptionDialog:You will lose access to the API.`,
      confirmLabel: $localize`:@@confirmCancelSubscriptionDialog:Yes, close`,
      cancelLabel: $localize`:@@cancelCancelSubscriptionDialog:Cancel`,
    };
    this.dialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
        role: 'alertdialog',
        id: 'confirmDialog',
        data: dialogData,
      })
      .afterClosed()
      .pipe(
        filter(confirmed => !!confirmed),
        switchMap(() => this.subscriptionService.close(this.subscriptionId)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: _ => this._subscriptionDetails.next(true),
        error: err => console.error(err),
      });
  }

  resumeConsumerStatus() {
    this.isLoadingStatus = true;
    this.subscriptionService
      .resumeConsumerStatus(this.subscriptionId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: _ => {
          this._subscriptionDetails.next(true);
        },
        error: err => {
          this.isLoadingStatus = false;
          console.error(err);
        },
      });
  }

  private loadDetails() {
    return this._subscriptionDetails.pipe(
      switchMap(() =>
        forkJoin({
          subscription: this.subscriptionService.get(this.subscriptionId),
          permissions: this.permissionsService.getApiPermissions(this.apiId),
        }),
      ),
      switchMap(({ subscription, permissions }) =>
        forkJoin({
          subscription: of(subscription),
          plan: this.getPlanData$(subscription.plan, permissions),
          api: this.apiService.details(this.apiId),
          application: this.applicationService.get(subscription.application),
        }),
      ),
      map(({ subscription, plan, api, application }) => {
        const subscriptionDetails: SubscriptionDetailsData = {
          application,
          subscription,
          api,
          planName: plan.name,
          planSecurity: plan.securityType,
          planUsageConfiguration: plan.usageConfiguration,
          consumerStatus: subscription.consumerStatus,
          failureCause: subscription.failureCause,
          createdAt: subscription.created_at,
          updatedAt: subscription.updated_at,
          entrypointUrls: api?.entrypoints,
        };

        if (subscription.status === 'ACCEPTED') {
          if (plan.securityType === 'API_KEY' && subscription.api) {
            const apiKeyItem = subscription?.keys?.length ? subscription.keys[0] : undefined;
            const apiKey = apiKeyItem?.key ?? '';
            const apiKeyConfigUsername = apiKeyItem?.hash ?? '';

            return {
              result: {
                ...subscriptionDetails,
                apiKey,
                apiKeyConfigUsername,
              },
            };
          } else if (plan.securityType === 'OAUTH2' || plan.securityType === 'JWT') {
            if (application.settings.oauth) {
              return {
                result: {
                  ...subscriptionDetails,
                  clientId: application.settings.oauth.client_id,
                  clientSecret: application.settings.oauth.client_secret,
                },
              };
            } else if (application.settings.app) {
              return {
                result: {
                  ...subscriptionDetails,
                  clientId: application.settings.app.client_id,
                },
              };
            }
          }
        }

        if (plan.planMode === 'PUSH' && !!subscription.consumerConfiguration) {
          return {
            result: {
              ...subscriptionDetails,
              consumerConfiguration: subscription.consumerConfiguration,
            },
          };
        }
        return { result: subscriptionDetails };
      }),
      catchError(_ => of({ error: true })),
      tap(() => (this.isLoadingStatus = false)),
    );
  }

  private getPlanData$(
    planId: string,
    permissions: UserApiPermissions,
  ): Observable<{
    name: string;
    securityType: PlanSecurityEnum;
    usageConfiguration: PlanUsageConfiguration;
    planMode: PlanMode;
  }> {
    return of(permissions.PLAN?.includes('R') === true).pipe(
      switchMap(hasPermission => (hasPermission ? this.getPlanDataFromList$(planId) : this.getPlanDataFromMetadata$(planId))),
      map(plan => ({
        name: plan.name ?? '',
        securityType: plan.securityType ?? 'KEY_LESS',
        usageConfiguration: plan.usageConfiguration ?? {},
        planMode: plan.planMode ?? 'STANDARD',
      })),
    );
  }

  private getPlanDataFromList$(planId: string): Observable<{
    name?: string;
    securityType?: PlanSecurityEnum;
    usageConfiguration?: PlanUsageConfiguration;
    planMode?: PlanMode;
  }> {
    return this.planService.list(this.apiId).pipe(
      map(({ data }) => {
        if (data && data.some(p => p.id === planId)) {
          const foundPlan = data.find(plan => plan.id === planId);
          return {
            name: foundPlan?.name,
            securityType: foundPlan?.security,
            usageConfiguration: foundPlan?.usage_configuration,
            planMode: foundPlan?.mode,
          };
        }
        return {};
      }),
      catchError(_ => this.getPlanDataFromMetadata$(planId)),
    );
  }

  private getPlanDataFromMetadata$(planId: string): Observable<{
    name?: string;
    securityType?: PlanSecurityEnum;
    usageConfiguration?: PlanUsageConfiguration;
    planMode?: PlanMode;
  }> {
    return this.subscriptionService.list({ apiIds: [this.apiId], statuses: [] }).pipe(
      catchError(_ => of({ data: [], metadata: {}, links: {} } as SubscriptionsResponse)),
      map(({ metadata }) => {
        const planMetadata = metadata[planId];
        return { name: planMetadata?.name, securityType: planMetadata?.securityType, planMode: planMetadata?.planMode };
      }),
    );
  }
}
