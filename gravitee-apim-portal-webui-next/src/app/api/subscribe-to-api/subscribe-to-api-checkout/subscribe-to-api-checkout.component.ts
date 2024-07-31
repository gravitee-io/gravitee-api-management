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
import { Component, Input, WritableSignal } from '@angular/core';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatRadioGroup } from '@angular/material/radio';

import { ApiAccessComponent } from '../../../../components/api-access/api-access.component';
import { RadioCardComponent } from '../../../../components/radio-card/radio-card.component';
import { SubscriptionInfoComponent } from '../../../../components/subscription-info/subscription-info.component';
import { Api } from '../../../../entities/api/api';
import { Application } from '../../../../entities/application/application';
import { Plan } from '../../../../entities/plan/plan';

@Component({
  selector: 'app-subscribe-to-api-checkout',
  imports: [
    SubscriptionInfoComponent,
    ApiAccessComponent,
    MatCard,
    MatCardContent,
    MatFormField,
    MatInput,
    MatLabel,
    MatRadioGroup,
    RadioCardComponent,
  ],
  templateUrl: './subscribe-to-api-checkout.component.html',
  styleUrl: './subscribe-to-api-checkout.component.scss',
  providers: [],
  standalone: true,
})
export class SubscribeToApiCheckoutComponent {
  @Input()
  api!: Api;

  @Input()
  plan!: Plan;

  @Input()
  message!: WritableSignal<string>;

  @Input()
  showApiKeyModeSelection!: boolean;

  @Input()
  sharedApiKeyModeDisabled: boolean = false;

  @Input()
  apiKeyMode!: WritableSignal<'EXCLUSIVE' | 'SHARED' | 'UNSPECIFIED' | null>;

  @Input()
  application?: Application;
}
