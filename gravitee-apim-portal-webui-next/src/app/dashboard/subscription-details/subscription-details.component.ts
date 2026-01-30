/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import {Component, DestroyRef, effect, inject, input, signal} from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatProgressBar } from '@angular/material/progress-bar';
import {ActivatedRoute, Router, RouterModule} from '@angular/router';
import { LoadingValueComponent } from './loading-value.component';
import {PlanMode, PlanSecurityEnum, PlanUsageConfiguration} from "../../../entities/plan/plan";
import {
  Subscription,
  SubscriptionConsumerConfiguration,
  SubscriptionConsumerStatusEnum, SubscriptionsResponse,
  SubscriptionStatusEnum
} from "../../../entities/subscription";
import {Api, ApiType} from "../../../entities/api/api";
import {BehaviorSubject, catchError, defer, delay, forkJoin, map, Observable, switchMap, tap} from "rxjs";
import {of} from "rxjs/internal/observable/of";
import {SubscriptionService} from "../../../services/subscription.service";
import {ApiService} from "../../../services/api.service";
import {ApplicationService} from "../../../services/application.service";
import {PermissionsService} from "../../../services/permissions.service";
import {PlanService} from "../../../services/plan.service";
import {MatDialog} from "@angular/material/dialog";
import {UserApiPermissions} from "../../../entities/permission/permission";
import {rxResource, toObservable, toSignal} from "@angular/core/rxjs-interop";

interface SubscriptionDetailsData {
  applicationName: string;
  planName: string;
  planSecurity: PlanSecurityEnum;
  planUsageConfiguration: PlanUsageConfiguration;
  subscriptionStatus: SubscriptionStatusEnum;
  consumerStatus: SubscriptionConsumerStatusEnum;
  failureCause?: string;
  createdAt?: string;
  updatedAt?: string;
  entrypointUrls?: string[];
  clientId?: string;
  clientSecret?: string;
  consumerConfiguration?: SubscriptionConsumerConfiguration;
}

@Component({
  selector: 'app-subscription-details',
  imports: [MatButton, RouterModule, LoadingValueComponent],
  templateUrl: './subscription-details.component.html',
  styleUrl: './subscription-details.component.scss',
  standalone: true,
})
export default class SubscriptionDetailsComponent {
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly applicationService = inject(ApplicationService);
  private readonly permissionsService = inject(PermissionsService);
  private readonly planService = inject(PlanService);
  private readonly apiService = inject(ApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  subscriptionId = input.required<string>();
  subscription = toSignal(toObservable(this.subscriptionId).pipe(switchMap(this.loadSubscriptionOrRedirect.bind(this))));
  api = toSignal(toObservable(this.subscription).pipe(switchMap(this.loadApi.bind(this))));
  subscriptionDetails = toSignal(toObservable(this.subscription).pipe(switchMap(this.loadDetails.bind(this))));

  private loadSubscriptionOrRedirect(subscriptionId: string) {
    return this.subscriptionService.get(subscriptionId).pipe(catchError(_ => {
      this.router.navigate(['404']);
      return of();
    }))
  }

  private loadApi(subscription?: Subscription): Observable<Api | null> {
    if (!subscription) {
      return of(null);
    }
    return this.apiService.details(subscription.api);
  }

  private loadDetails(subscription?: Subscription): Observable<SubscriptionDetailsData | null> {
    if (!subscription) {
      return of(null);
    }
    return forkJoin({
      permissions: this.permissionsService.getApiPermissions(subscription.api),
      application: this.applicationService.get(subscription.application),
    }).pipe(
      switchMap(({permissions, application}) =>
          this.getPlanData$(subscription,  permissions).pipe(map(plan => ({ plan, application }))),
      ),
      map(({ plan, application }) => {
        const subscriptionDetails: SubscriptionDetailsData = {
          applicationName: application.name ?? '',
          planName: plan.name,
          planSecurity: plan.securityType,
          planUsageConfiguration: plan.usageConfiguration,
          subscriptionStatus: subscription.status,
          consumerStatus: subscription.consumerStatus,
          failureCause: subscription.failureCause,
          createdAt: subscription.created_at,
          updatedAt: subscription.updated_at,
        };

        if (subscription.status === 'ACCEPTED') {
          if (plan.securityType === 'API_KEY' && subscription.api) {
            const apiKeyItem = subscription?.keys?.length ? subscription.keys[0] : undefined;
            const apiKey = apiKeyItem?.key ?? '';
            const apiKeyConfigUsername = apiKeyItem?.hash ?? '';

            return {
              ...subscriptionDetails,
              apiKey,
              apiKeyConfigUsername,
            };
          } else if (plan.securityType === 'OAUTH2' || plan.securityType === 'JWT') {
            if (application.settings.oauth) {
              return {
                ...subscriptionDetails,
                clientId: application.settings.oauth.client_id,
                clientSecret: application.settings.oauth.client_secret,
              };
            } else if (application.settings.app) {
              return {
                ...subscriptionDetails,
                clientId: application.settings.app.client_id,
              };
            }
          }
        }

        if (plan.planMode === 'PUSH' && !!subscription.consumerConfiguration) {
          return {
            ...subscriptionDetails,
            consumerConfiguration: subscription.consumerConfiguration,
          };
        }
        return subscriptionDetails;
      }),
      catchError(_ => of(null)),
    );
  }

  private getPlanData$(
    subscription: Subscription,
    permissions: UserApiPermissions,
  ): Observable<{
    name: string;
    securityType: PlanSecurityEnum;
    usageConfiguration: PlanUsageConfiguration;
    planMode: PlanMode;
  }> {
    const { plan: planId, api: apiId } = subscription;
    return of(permissions.PLAN?.includes('R') === true).pipe(
      switchMap(hasPermission => (hasPermission ? this.getPlanDataFromList$(planId, apiId) : this.getPlanDataFromMetadata$(planId, apiId))),
      map(plan => ({
        name: plan.name ?? '',
        securityType: plan.securityType ?? 'KEY_LESS',
        usageConfiguration: plan.usageConfiguration ?? {},
        planMode: plan.planMode ?? 'STANDARD',
      })),
    );
  }

  private getPlanDataFromList$(planId: string, apiId: string): Observable<{
    name?: string;
    securityType?: PlanSecurityEnum;
    usageConfiguration?: PlanUsageConfiguration;
    planMode?: PlanMode;
  }> {
    return this.planService.list(apiId).pipe(
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
      catchError(_ => this.getPlanDataFromMetadata$(planId, apiId)),
    );
  }

  private getPlanDataFromMetadata$(planId: string, apiId: string): Observable<{
    name?: string;
    securityType?: PlanSecurityEnum;
    usageConfiguration?: PlanUsageConfiguration;
    planMode?: PlanMode;
  }> {
    return this.subscriptionService.list({ apiId, statuses: [] }).pipe(
      catchError(_ => of({ data: [], metadata: {}, links: {} } as SubscriptionsResponse)),
      map(({ metadata }) => {
        const planMetadata = metadata[planId];
        return { name: planMetadata?.name, securityType: planMetadata?.securityType, planMode: planMetadata?.planMode };
      }),
    );
  }

  copyToClipboard() {
    navigator.clipboard.writeText(this.subscriptionId());
    alert('copied!')
    // Optional: Show a snackbar "Copied!"
  }
}
