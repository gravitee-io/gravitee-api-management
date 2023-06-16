/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { Component, Inject, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { StateService } from '@uirouter/core';
import { DatePipe } from '@angular/common';

import { SubscriptionStatus } from '../../../../../entities/management-api-v2';
import { ApiSubscriptionV2Service } from '../../../../../services-ngx/api-subscription-v2.service';
import { UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';

interface SubscriptionDetailVM {
  id: string;
  plan: string;
  status: SubscriptionStatus;
  subscribedBy: string;
  application?: { label: string; description: string };
  publisherMessage?: string;
  subscriberMessage?: string;
  createdAt?: string;
  endingAt?: string;
  pausedAt?: string;
  processedAt?: string;
  startingAt?: string;
  closedAt?: string;
  domain?: string;
  description?: string;
}

@Component({
  selector: 'api-portal-subscription-detail',
  template: require('./api-portal-subscription-edit.component.html'),
  styles: [require('./api-portal-subscription-edit.component.scss')],
})
export class ApiPortalSubscriptionEditComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  subscription: SubscriptionDetailVM;
  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly apiSubscriptionService: ApiSubscriptionV2Service,
    private datePipe: DatePipe,
  ) {}

  ngOnInit(): void {
    const apiId = this.ajsStateParams.apiId;
    const subscriptionId = this.ajsStateParams.subscriptionId;

    this.apiSubscriptionService
      .getById(apiId, subscriptionId, ['plan', 'application', 'subscribedBy'])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((subscription) => {
        if (subscription) {
          this.subscription = {
            id: subscription.id,
            plan: subscription.plan.security ? `${subscription.plan.name} (${subscription.plan.security.type})` : subscription.plan.name,
            application: {
              label: `${subscription.application.name} (${subscription.application.primaryOwner.displayName}) - Type: ${subscription.application.type}`,
              description: subscription.application.description,
            },
            status: subscription.status,
            subscribedBy: subscription.subscribedBy.displayName,
            publisherMessage: subscription.publisherMessage ?? '-',
            subscriberMessage: subscription.consumerMessage ?? '-',
            createdAt: this.formatDate(subscription.createdAt),
            pausedAt: this.formatDate(subscription.pausedAt),
            startingAt: this.formatDate(subscription.startingAt),
            endingAt: this.formatDate(subscription.endingAt),
            processedAt: this.formatDate(subscription.processedAt),
            closedAt: this.formatDate(subscription.closedAt),
            domain: subscription.application.domain ?? '-',
          };
        }
      });
  }

  validateSubscription() {
    // Do nothing for now
  }

  rejectSubscription() {
    // Do nothing for now
  }

  transferSubscription() {
    // Do nothing for now
  }

  pauseSubscription() {
    // Do nothing for now
  }

  changeEndDate() {
    // Do nothing for now
  }

  closeSubscription() {
    // Do nothing for now
  }

  goBackToSubscriptions() {
    this.ajsState.go('management.apis.ng.subscriptions');
  }

  private formatDate(date: Date): string {
    return date ? this.datePipe.transform(date, 'MMM d, y h:mm:ss.sss a') : '-';
  }
}
