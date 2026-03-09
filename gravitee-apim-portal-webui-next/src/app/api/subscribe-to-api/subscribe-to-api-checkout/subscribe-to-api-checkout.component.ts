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
import { Component, input, Input, InputSignal, OnInit, WritableSignal } from '@angular/core';
import { MatCard, MatCardContent } from '@angular/material/card';

import { GmdFormHostComponent, GraviteeMarkdownViewerModule } from '@gravitee/gravitee-markdown';

import { ApiAccessComponent } from '../../../../components/api-access/api-access.component';
import { RadioCardComponent } from '../../../../components/radio-card/radio-card.component';
import { SubscriptionInfoComponent } from '../../../../components/subscription-info/subscription-info.component';
import { Api } from '../../../../entities/api/api';
import { Application } from '../../../../entities/application/application';
import { Plan } from '../../../../entities/plan/plan';
import { SubscriptionForm } from '../../../../entities/portal/subscription-form';
import { Subscription } from '../../../../entities/subscription/subscription';

@Component({
  selector: 'app-subscribe-to-api-checkout',
  imports: [
    SubscriptionInfoComponent,
    ApiAccessComponent,
    MatCard,
    MatCardContent,
    RadioCardComponent,
    GraviteeMarkdownViewerModule,
    GmdFormHostComponent,
  ],
  templateUrl: './subscribe-to-api-checkout.component.html',
  styleUrl: './subscribe-to-api-checkout.component.scss',
  providers: [],
})
export class SubscribeToApiCheckoutComponent implements OnInit {
  @Input()
  api!: Api;

  @Input()
  plan!: Plan;

  @Input()
  message!: WritableSignal<string>;

  @Input()
  subscriptionForm: SubscriptionForm | null = null;

  @Input()
  applicationApiKeySubscriptions: Subscription[] = [];

  @Input()
  apiKeyMode!: WritableSignal<'EXCLUSIVE' | 'SHARED' | 'UNSPECIFIED' | null>;

  @Input({ required: true })
  application: Application | undefined;

  showApiKeyModeSelection: InputSignal<boolean> = input(false);

  sharedApiKeyModeDisabled: boolean = false;

  ngOnInit() {
    this.sharedApiKeyModeDisabled =
      this.showApiKeyModeSelection() &&
      this.applicationApiKeySubscriptions.length === 1 &&
      this.applicationApiKeySubscriptions[0].api === this.api.id;
  }
}
