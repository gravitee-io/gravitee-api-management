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

import { CdkCopyToClipboard } from '@angular/cdk/clipboard';
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

import { SubscriptionMetadata } from '../../../../entities/subscription/subscription';
import { CapitalizeFirstPipe } from '../../../../pipe/capitalize-first.pipe';
import { ApiService } from '../../../../services/api.service';
import { ApplicationService } from '../../../../services/application.service';
import { ConfigService } from '../../../../services/config.service';
import { SubscriptionService } from '../../../../services/subscription.service';

export interface SubscriptionDetailsData {
  application?: string;
  plan?: string;
  security?: string;
  status?: string;
  authentication?: string;
  apiKey?: SubscriptionDetailsDataApiKey;
  oauth2?: SubscriptionDetailsDataOauth2;
}

export interface SubscriptionDetailsDataApiKey {
  key: string;
  baseUrl: string;
  commandLine: string;
}

export interface SubscriptionDetailsDataOauth2 {
  clientId: string;
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
    CdkCopyToClipboard,
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
  subscriptionApplicationId!: string;

  hidePassword: boolean = true;
  subscriptionDetails: Observable<SubscriptionDetailsData> = of();

  constructor(
    private configService: ConfigService,
    private subscriptionService: SubscriptionService,
    private apiService: ApiService,
    private applicationService: ApplicationService,
    private capitalizeFirstPipe: CapitalizeFirstPipe,
    public dialog: MatDialog,
  ) {}

  ngOnInit() {
    this.subscriptionDetails = this.loadDetails();
  }

  retrieveMetadataName(id: string, metadata: SubscriptionMetadata) {
    if (Object.hasOwn(metadata, id)) {
      return this.capitalizeFirstPipe.transform(<string>metadata[id]['name']);
    } else {
      return '-';
    }
  }

  private loadDetails(): Observable<SubscriptionDetailsData> {
    return this.subscriptionService.listDetails(this.subscriptionApplicationId).pipe(
      switchMap(details => {
        return forkJoin({
          details: of(details),
          list: this.subscriptionService.list(this.apiId, null),
          api: this.apiService.plans(this.apiId),
          application: this.applicationService.list(<string>details.application),
        });
      }),
      map(({ details, list, api, application }) => {
        const selectedApi = api.data?.find((item: { id: string }) => item.id === details.plan);
        const selectedSubscription = list.data.find(item => item.id === this.subscriptionApplicationId);

        let subscriptionDetails: SubscriptionDetailsData = {
          application: this.retrieveMetadataName(<string>selectedSubscription?.application, list.metadata),
          plan: this.retrieveMetadataName(<string>selectedSubscription?.plan, list.metadata),
          security: selectedApi?.security,
          authentication: this.getAuthenticationType(<string>selectedApi?.security),
          status: <string>details.status,
        };

        if (details.status === 'ACCEPTED') {
          if (selectedApi?.security === 'API_KEY' && details.api) {
            const baseUrl = list.metadata[details.api]?.entrypoints?.[0]?.target;
            const commandLine = this.formatCurlCommandLine(
              <string>list.metadata[details.api]?.entrypoints?.[0]?.target,
              <string>details.keys[0].key,
            );

            subscriptionDetails = {
              ...subscriptionDetails,
              apiKey: {
                key: <string>details?.keys[0].key,
                baseUrl: <string>baseUrl,
                commandLine: commandLine,
              },
            };
          } else if (selectedApi?.security === 'OAUTH2' || selectedApi?.security === 'JWT') {
            subscriptionDetails = {
              ...subscriptionDetails,
              oauth2: {
                clientId: <string>application.settings?.oauth.client_id,
                clientSecret: <string>application.settings?.oauth.client_secret,
              },
            };
          }
        }

        return subscriptionDetails;
      }),
      catchError(_ => {
        return of({});
      }),
    );
  }

  private formatCurlCommandLine(url: string, header: string): string {
    const headersFormatted = `--header "${this.configService.baseURL}: ${header}" \\`;
    return `curl ${headersFormatted} ${url}`;
  }

  private getAuthenticationType(authentication: string) {
    switch (authentication) {
      case 'OAUTH2':
        return 'OAuth2';
      case 'JWT':
        return 'JWT';
      case 'API_KEY':
        return 'Api Key';
      default:
        return '';
    }
  }
}
