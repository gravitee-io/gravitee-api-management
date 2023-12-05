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

import * as _ from 'lodash';
import { BehaviorSubject } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';

import { PagedResult } from '../../../../entities/pagedResult';

const defaultStatus = ['ACCEPTED', 'PENDING', 'PAUSED'];

export class SubscriptionFilter {
  status?: string[] = defaultStatus;
  apis?: string[];
  apiKey?: string;
}

class ApplicationSubscriptionsController {
  private subscribers: any[];
  private application: any;
  private exclusiveSubscriptions = new PagedResult();
  private activatedRoute: ActivatedRoute;
  private sharedSubscriptions = new PagedResult();
  private filter = new SubscriptionFilter();
  private $filterEvent = new BehaviorSubject<SubscriptionFilter>(new SubscriptionFilter());

  private status = {
    ACCEPTED: 'Accepted',
    CLOSED: 'Closed',
    PAUSED: 'Paused',
    PENDING: 'Pending',
    REJECTED: 'Rejected',
  };

  constructor(private readonly ngRouter: Router) {}

  $onInit(): void {
    if (this.activatedRoute.snapshot.queryParams.status) {
      if (Array.isArray(this.activatedRoute.snapshot.queryParams.status)) {
        this.filter.status = this.activatedRoute.snapshot.queryParams.status;
      } else {
        this.filter.status = this.activatedRoute.snapshot.queryParams.status.split(',');
      }
    }
    if (this.activatedRoute.snapshot.queryParams.api) {
      if (Array.isArray(this.activatedRoute.snapshot.queryParams.api)) {
        this.filter.apis = this.activatedRoute.snapshot.queryParams.api;
      } else {
        this.filter.apis = this.activatedRoute.snapshot.queryParams.api.split(',');
      }
    }
    if (this.activatedRoute.snapshot.queryParams.api_key) {
      this.filter.apiKey = this.activatedRoute.snapshot.queryParams.api_key;
    }

    this.doFilter();
  }

  hasFilter(): boolean {
    return (
      (this.filter.apis && this.filter.apis.length > 0) ||
      !_.isEmpty(this.filter.apiKey) ||
      _.difference(defaultStatus, this.filter.status).length > 0 ||
      _.difference(this.filter.status, defaultStatus).length > 0
    );
  }

  areSharedApiKeysDisplayed(): boolean {
    return this.sharedSubscriptions.data.filter((subscription) => subscription.processed_at).length !== 0;
  }

  clearFilter(): void {
    this.filter = new SubscriptionFilter();
    this.doFilter();
  }

  doFilter(): void {
    this.ngRouter
      .navigate([], {
        relativeTo: this.activatedRoute,
        queryParams: {
          status: this.filter.status ? this.filter.status.join(',') : '',
          api: this.filter.apis ? this.filter.apis.join(',') : '',
          api_key: this.filter.apiKey ? this.filter.apiKey : '',
        },
        queryParamsHandling: 'merge',
      })
      .then((_) => this.$filterEvent.next(this.filter));
  }

  goToSubscribe() {
    this.ngRouter.navigate(['subscribe'], { relativeTo: this.activatedRoute });
  }
}
ApplicationSubscriptionsController.$inject = ['ngRouter'];

export default ApplicationSubscriptionsController;
