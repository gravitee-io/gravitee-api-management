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
import { Component, Input } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatCard } from '@angular/material/card';

import { SubscribeToApiCheckoutComponent } from './subscribe-to-api-checkout/subscribe-to-api-checkout.component';
import { SubscribeToApiChooseApplicationComponent } from './subscribe-to-api-choose-application/subscribe-to-api-choose-application.component';
import { SubscribeToApiChoosePlanComponent } from './subscribe-to-api-choose-plan/subscribe-to-api-choose-plan.component';
import { BreadcrumbNavigationComponent } from '../../../components/breadcrumb-navigation/breadcrumb-navigation.component';

export interface SubscribePlan {
  id: string;
  header: string;
  plan: string[];
  footer: string;
  isDisabled: boolean;
}

export interface SubscribeToApiForm {
  step: FormControl<number | null>;
  plan: FormControl<SubscribePlan>;
}
@Component({
  selector: 'app-subscribe-to-api',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCard,
    BreadcrumbNavigationComponent,
    SubscribeToApiCheckoutComponent,
    SubscribeToApiChoosePlanComponent,
    SubscribeToApiChooseApplicationComponent,
  ],
  templateUrl: './subscribe-to-api.component.html',
  styleUrl: './subscribe-to-api.component.scss',
  standalone: true,
})
export class SubscribeToApiComponent {
  @Input()
  apiId!: string;

  subscribeToApiForm: FormGroup<SubscribeToApiForm> = new FormGroup({
    step: new FormControl(1),
    plan: new FormControl(),
  });
}
