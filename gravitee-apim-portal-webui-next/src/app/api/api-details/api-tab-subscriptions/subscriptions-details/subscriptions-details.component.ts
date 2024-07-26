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
import { PlanSecurityEnum, PlanUsageConfiguration } from '../../../../../entities/plan/plan';
import { SubscriptionStatusEnum } from '../../../../../entities/subscription/subscription';
import { CapitalizeFirstPipe } from '../../../../../pipe/capitalize-first.pipe';
import { ApiService } from '../../../../../services/api.service';
import { ApplicationService } from '../../../../../services/application.service';
import { PlanService } from '../../../../../services/plan.service';
import { SubscriptionService } from '../../../../../services/subscription.service';

export interface SubscriptionDetailsData {
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

  subscriptionDetails$: Observable<SubscriptionDetailsData> = of();

  constructor(
    private subscriptionService: SubscriptionService,
    private apiService: ApiService,
    private applicationService: ApplicationService,

    private planService: PlanService,
    public dialog: MatDialog,
  ) {}

  ngOnInit() {
    this.subscriptionDetails$ = this.loadDetails();
  }

  private loadDetails(): Observable<SubscriptionDetailsData> {
    return this.subscriptionService.get(this.subscriptionId).pipe(
      switchMap(details => {
        return forkJoin({
          details: of(details),
          plans: this.planService.list(this.apiId),
          list: this.subscriptionService.list({ apiId: this.apiId, statuses: null }),
          api: this.apiService.details(this.apiId),
          application: this.applicationService.get(details.application),
        });
      }),
      map(({ details, plans, list, application }) => {
        const foundPlan = plans.data?.find(plan => plan.id === details.plan);
        const planSecurityType = foundPlan?.security ?? 'KEY_LESS';

        const subscriptionDetails: SubscriptionDetailsData = {
          applicationName: application.name ?? '',
          planName: foundPlan?.name ?? '',
          planSecurity: planSecurityType,
          planUsageConfiguration: foundPlan?.usage_configuration ?? {},
          subscriptionStatus: details.status,
        };

        if (details.status === 'ACCEPTED') {
          if (foundPlan?.security === 'API_KEY' && details.api) {
            const entrypointUrl = list.metadata[details.api]?.entrypoints?.[0]?.target;
            const apiKey = details?.keys?.length && details.keys[0].key ? details.keys[0].key : '';

            return {
              ...subscriptionDetails,
              apiKey,
              entrypointUrl,
            };
          } else if (foundPlan?.security === 'OAUTH2' || foundPlan?.security === 'JWT') {
            return {
              ...subscriptionDetails,
              clientId: application.settings?.oauth.client_id,
              clientSecret: application.settings?.oauth.client_secret,
            };
          }
        }

        return subscriptionDetails;
      }),
      catchError(_ => {
        return of();
      }),
    );
  }
}
