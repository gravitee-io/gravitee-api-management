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
import { Component, effect, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

import { BreadcrumbService } from '../../../services/breadcrumb.service';
import { SubscriptionService } from '../../../services/subscription.service';
import { SubscriptionsDetailsComponent } from '../../api/api-details/api-tab-subscriptions/subscriptions-details/subscriptions-details.component';
import { subscriptionListBreadcrumb } from '../subscriptions/subscription-breadcrumbs';

@Component({
  selector: 'app-subscription-details',
  imports: [SubscriptionsDetailsComponent],
  templateUrl: './subscription-details.component.html',
  styleUrl: './subscription-details.component.scss',
})
export default class SubscriptionDetailsComponent {
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly breadcrumbService = inject(BreadcrumbService);
  subscriptionId = input.required<string>();
  apiId = rxResource({
    params: this.subscriptionId,
    stream: ({ params }) => this.subscriptionService.get(params).pipe(map(subscription => subscription.api)),
  });

  constructor() {
    effect(() => {
      const id = this.subscriptionId();
      this.breadcrumbService.set([
        subscriptionListBreadcrumb(true),
        { id: `subscription-${id}`, label: $localize`:@@subscriptionTitle:Subscription ` + id },
      ]);
    });
  }
}
