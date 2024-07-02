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
import { Component, Input } from '@angular/core';
import { MatCard, MatCardContent, MatCardHeader } from '@angular/material/card';

import { getPlanSecurityTypeLabel, PlanSecurityEnum, PlanUsageConfiguration } from '../../entities/plan/plan';
import { CapitalizeFirstPipe } from '../../pipe/capitalize-first.pipe';

@Component({
  selector: 'app-subscription-info',
  standalone: true,
  imports: [CapitalizeFirstPipe, MatCard, MatCardContent, MatCardHeader],
  templateUrl: './subscription-info.component.html',
  styleUrl: './subscription-info.component.scss',
})
export class SubscriptionInfoComponent {
  @Input()
  applicationName: string = '';

  @Input()
  planName: string = '';

  @Input()
  planSecurity!: PlanSecurityEnum;

  @Input()
  planUsageConfiguration: PlanUsageConfiguration = {};

  @Input()
  subscriptionStatus: string = '';

  protected readonly getPlanSecurityTypeLabel = getPlanSecurityTypeLabel;
}
