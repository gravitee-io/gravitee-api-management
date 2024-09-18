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
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormField, MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput, MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, map, Observable, switchMap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { ApiAccessComponent } from '../../../../../components/api-access/api-access.component';
import { LoaderComponent } from '../../../../../components/loader/loader.component';
import { SubscriptionInfoComponent } from '../../../../../components/subscription-info/subscription-info.component';
import { UserApiPermissions } from '../../../../../entities/permission/permission';
import { PlanSecurityEnum, PlanUsageConfiguration } from '../../../../../entities/plan/plan';
import { SubscriptionStatusEnum } from '../../../../../entities/subscription/subscription';
import { SubscriptionsResponse } from '../../../../../entities/subscription/subscriptions-response';
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
  applicationName: string;
  planName: string;
  planSecurity: PlanSecurityEnum;
  planUsageConfiguration: PlanUsageConfiguration;
  subscriptionStatus: SubscriptionStatusEnum;
  apiKey?: string;
  entrypointUrl?: string;
  clientId?: string;
  clientSecret?: string;
}

@Component({
  imports: [
    MatIcon,
    MatCardModule,
    RouterLink,
    MatButton,
    AsyncPipe,
    CapitalizeFirstPipe,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    MatFormField,
    MatInput,
    MatIconButton,
    AsyncPipe,
    ApiAccessComponent,
    SubscriptionInfoComponent,
    LoaderComponent,
  ],
  providers: [CapitalizeFirstPipe],
  selector: 'app-subscriptions-details',
  standalone: true,
  styleUrl: './subscriptions-details.component.scss',
  templateUrl: './subscriptions-details.component.html',
})
export class SubscriptionsDetailsComponent implements OnInit {
  @Input()
  apiId!: string;

  @Input()
  subscriptionId!: string;

  subscriptionDetails$: Observable<SubscriptionDetailsVM> = of();

  constructor(
    private subscriptionService: SubscriptionService,
    private apiService: ApiService,
    private applicationService: ApplicationService,
    private permissionsService: PermissionsService,

    private planService: PlanService,
    public dialog: MatDialog,
  ) {}

  ngOnInit() {
    this.subscriptionDetails$ = this.loadDetails();
  }

  private loadDetails(): Observable<SubscriptionDetailsVM> {
    return forkJoin({
      subscription: this.subscriptionService.get(this.subscriptionId),
      permissions: this.permissionsService.getApiPermissions(this.apiId),
    }).pipe(
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
          applicationName: application.name ?? '',
          planName: plan.name,
          planSecurity: plan.securityType,
          planUsageConfiguration: plan.usageConfiguration,
          subscriptionStatus: subscription.status,
        };

        if (subscription.status === 'ACCEPTED') {
          if (plan.securityType === 'API_KEY' && subscription.api) {
            const entrypointUrl = api?.entrypoints?.[0];
            const apiKey = subscription?.keys?.length && subscription.keys[0].key ? subscription.keys[0].key : '';

            return {
              result: {
                ...subscriptionDetails,
                apiKey,
                entrypointUrl,
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

        return { result: subscriptionDetails };
      }),
      catchError(_ => of({ error: true })),
    );
  }

  private getPlanData$(
    planId: string,
    permissions: UserApiPermissions,
  ): Observable<{
    name: string;
    securityType: PlanSecurityEnum;
    usageConfiguration: PlanUsageConfiguration;
  }> {
    return of(permissions.PLAN?.includes('R') === true).pipe(
      switchMap(hasPermission => (hasPermission ? this.getPlanDataFromList$(planId) : this.getPlanDataFromMetadata$(planId))),
      map(plan => ({
        name: plan.name ?? '',
        securityType: plan.securityType ?? 'KEY_LESS',
        usageConfiguration: plan.usageConfiguration ?? {},
      })),
    );
  }

  private getPlanDataFromList$(planId: string): Observable<{
    name?: string;
    securityType?: PlanSecurityEnum;
    usageConfiguration?: PlanUsageConfiguration;
  }> {
    return this.planService.list(this.apiId).pipe(
      map(({ data }) => {
        if (data && data.some(p => p.id === planId)) {
          const foundPlan = data.find(plan => plan.id === planId);
          return { name: foundPlan?.name, securityType: foundPlan?.security, usageConfiguration: foundPlan?.usage_configuration };
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
  }> {
    return this.subscriptionService.list({ apiId: this.apiId, statuses: [] }).pipe(
      catchError(_ => of({ data: [], metadata: {}, links: {} } as SubscriptionsResponse)),
      map(({ metadata }) => {
        const planMetadata = metadata[planId];
        return { name: planMetadata?.name, securityType: planMetadata?.securityType };
      }),
    );
  }
}
