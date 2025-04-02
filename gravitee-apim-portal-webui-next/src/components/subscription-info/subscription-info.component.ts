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
import { Component, Input, OnInit } from '@angular/core';
import { MatCard, MatCardContent, MatCardHeader } from '@angular/material/card';

import { ApiType } from '../../entities/api/api';
import { getPlanSecurityTypeLabel, PlanSecurityEnum, PlanUsageConfiguration, PlanValidationEnum } from '../../entities/plan/plan';
import { CapitalizeFirstPipe } from '../../pipe/capitalize-first.pipe';
import { ToPeriodTimeUnitLabelPipe } from '../../pipe/time-unit.pipe';

@Component({
  selector: 'app-subscription-info',
  imports: [CapitalizeFirstPipe, MatCard, MatCardContent, MatCardHeader, ToPeriodTimeUnitLabelPipe],
  templateUrl: './subscription-info.component.html',
  styleUrl: './subscription-info.component.scss',
})
export class SubscriptionInfoComponent implements OnInit {
  @Input()
  applicationName?: string = '';

  @Input()
  planName: string = '';

  @Input()
  planSecurity!: PlanSecurityEnum;

  @Input()
  planUsageConfiguration?: PlanUsageConfiguration = {};

  @Input()
  planValidation?: PlanValidationEnum;

  @Input()
  apiType?: ApiType;

  @Input()
  subscriptionStatus: string = '';

  authentication: string = '';

  ngOnInit() {
    this.authentication = getPlanSecurityTypeLabel(this.planSecurity);
  }
}
