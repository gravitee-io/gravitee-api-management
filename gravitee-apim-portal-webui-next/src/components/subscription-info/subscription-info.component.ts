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
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent, MatCardHeader } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { Api } from '../../entities/api/api';
import { Application } from '../../entities/application/application';
import { getPlanSecurityTypeLabel, PlanSecurityEnum, PlanUsageConfiguration, PlanValidationEnum } from '../../entities/plan/plan';
import { Subscription, SubscriptionConsumerStatusEnum } from '../../entities/subscription';
import { CapitalizeFirstPipe } from '../../pipe/capitalize-first.pipe';
import { ToPeriodTimeUnitLabelPipe } from '../../pipe/time-unit.pipe';
import { BannerComponent } from '../banner/banner.component';
import { LoaderComponent } from '../loader/loader.component';

@Component({
  selector: 'app-subscription-info',
  imports: [
    CapitalizeFirstPipe,
    MatCard,
    MatCardContent,
    MatCardHeader,
    ToPeriodTimeUnitLabelPipe,
    BannerComponent,
    MatIcon,
    MatButton,
    LoaderComponent,
    RouterLink,
  ],
  templateUrl: './subscription-info.component.html',
  styleUrl: './subscription-info.component.scss',
})
export class SubscriptionInfoComponent implements OnInit {
  @Input({ required: true })
  application?: Application;

  @Input()
  planName: string = '';

  @Input()
  planSecurity!: PlanSecurityEnum;

  @Input()
  planUsageConfiguration?: PlanUsageConfiguration = {};

  @Input()
  planValidation?: PlanValidationEnum;

  @Input()
  api?: Api;

  @Input()
  subscription?: Subscription;

  @Input()
  isLoadingStatus: boolean = false;

  @Input()
  consumerStatus: SubscriptionConsumerStatusEnum = SubscriptionConsumerStatusEnum.STARTED;

  @Input()
  failureCause?: string = '';

  @Input()
  createdAt?: string = '';

  @Input()
  updatedAt?: string = '';

  @Output()
  resumeConsumerStatus = new EventEmitter<void>();

  public authentication: string = '';

  protected readonly SubscriptionConsumerStatusEnum = SubscriptionConsumerStatusEnum;

  ngOnInit() {
    this.authentication = getPlanSecurityTypeLabel(this.planSecurity);
  }
}
