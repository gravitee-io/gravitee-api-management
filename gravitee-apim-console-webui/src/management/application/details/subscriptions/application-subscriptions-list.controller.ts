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
import { StateService } from '@uirouter/core';

import { SubscriptionFilter } from './application-subscriptions.controller';

import { PagedResult } from '../../../../entities/pagedResult';
import ApplicationService from '../../../../services/application.service';

export class SubscriptionQuery {
  securityTypes?: string[];
  page?: number = 1;
  size?: number = 10;
}

class ApplicationSubscriptionsListController {
  private listLabel: string;
  private subscriptions: PagedResult;
  private query: SubscriptionQuery = new SubscriptionQuery();
  private filter: SubscriptionFilter = new SubscriptionFilter();
  private filterEvent: BehaviorSubject<SubscriptionFilter>;
  private application: any;
  private securityTypes: string[];

  private queryParamsPrefix: string;
  private pageQueryParamName: string;
  private sizeQueryParamName: string;

  private isFirstFilter = true;

  constructor(private ApplicationService: ApplicationService, private $state: StateService) {
    'ngInject';
    this.onPaginate = this.onPaginate.bind(this);
  }

  $onInit(): void {
    this.query.securityTypes = this.securityTypes;
    this.pageQueryParamName = `${this.queryParamsPrefix || ''}page`;
    this.sizeQueryParamName = `${this.queryParamsPrefix || ''}size`;

    if (this.$state.params[this.pageQueryParamName]) {
      this.query.page = this.$state.params[this.pageQueryParamName];
    }
    if (this.$state.params[this.sizeQueryParamName]) {
      this.query.size = this.$state.params[this.sizeQueryParamName];
    }

    this.filterEvent.subscribe((filter) => {
      this.filter = filter;
      this.doFilter();
    });
  }

  onPaginate(page): void {
    this.query.page = page;
    this.updatePaginationQueryParams();
    this.doSearch();
  }

  doFilter(): void {
    if (!this.isFirstFilter) {
      this.query.page = 1; // go back to first page on each new filter search from user
      this.updatePaginationQueryParams();
    }
    this.isFirstFilter = false;
    this.doSearch();
  }

  doSearch(): void {
    const query = this.buildQuery();
    this.ApplicationService.listSubscriptions(this.application.id, query).then((response) => {
      this.subscriptions = response.data as PagedResult;
    });
  }

  updatePaginationQueryParams(): void {
    this.$state.transitionTo(
      this.$state.current,
      _.merge(this.$state.params, {
        [this.pageQueryParamName]: this.query.page,
        [this.sizeQueryParamName]: this.query.size,
      }),
      { notify: false },
    );
  }

  buildQuery(): string {
    let query = '?';
    const parameters: any = {};

    parameters.page = this.query.page;
    parameters.size = this.query.size;

    if (this.filter.status !== undefined) {
      parameters.status = this.filter.status.join(',');
    }
    if (this.filter.apis !== undefined) {
      parameters.api = this.filter.apis.join(',');
    }
    if (this.filter.apiKey !== undefined) {
      parameters.api_key = this.filter.apiKey;
    }
    if (this.query.securityTypes !== undefined) {
      parameters.security_types = this.query.securityTypes.join(',');
    }
    _.mapKeys(parameters, (value, key) => {
      return (query += key + '=' + value + '&');
    });

    return query;
  }

  navigateToSubscription(subscriptionId: string): void {
    this.$state.transitionTo('management.applications.application.subscriptions.subscription', { subscriptionId, ...this.$state.params });
  }
}

export default ApplicationSubscriptionsListController;
