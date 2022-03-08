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
import { StateService } from '@uirouter/core';
import { IPromise, IScope } from 'angular';
import * as _ from 'lodash';
import * as moment from 'moment';

import { PagedResult } from '../../../../entities/pagedResult';
import { ApiService } from '../../../../services/api.service';
import NotificationService from '../../../../services/notification.service';
import ApplicationService from '../../../../services/application.service';
import { PlanSecurityType } from '../../../../entities/plan/plan';
import { ApiKeyMode } from '../../../../entities/application/application';
import { Constants } from '../../../../entities/Constants';

const defaultStatus = ['ACCEPTED', 'PENDING', 'PAUSED'];

export class SubscriptionQuery {
  status?: string[] = defaultStatus;
  applications?: string[];
  plans?: string[];
  page?: number = 1;
  size?: number = 10;
  api_key?: string;
}

const ApiSubscriptionsComponent: ng.IComponentOptions = {
  bindings: {
    api: '<',
    plans: '<',
    subscriptions: '<',
    subscribers: '<',
  },
  template: require('./subscriptions.html'),
  controller: class {
    private subscriptions: PagedResult;
    private api: any;

    private query: SubscriptionQuery = new SubscriptionQuery();

    private status = {
      ACCEPTED: 'Accepted',
      CLOSED: 'Closed',
      PAUSED: 'Paused',
      PENDING: 'Pending',
      REJECTED: 'Rejected',
    };

    private subscriptionsFiltersForm: any;

    constructor(
      private ApiService: ApiService,
      private ApplicationService: ApplicationService,
      private NotificationService: NotificationService,
      private Constants: Constants,
      private $mdDialog: angular.material.IDialogService,
      private $state: StateService,
      public $rootScope: IScope,
      private $timeout: ng.ITimeoutService,
    ) {
      'ngInject';

      this.onPaginate = this.onPaginate.bind(this);
      if (this.$state.params.status) {
        if (Array.isArray(this.$state.params.status)) {
          this.query.status = this.$state.params.status;
        } else {
          this.query.status = this.$state.params.status.split(',');
        }
      }
      if (this.$state.params.application) {
        if (Array.isArray(this.$state.params.application)) {
          this.query.applications = this.$state.params.application;
        } else {
          this.query.applications = this.$state.params.application.split(',');
        }
      }
      if (this.$state.params.plan) {
        if (Array.isArray(this.$state.params.plan)) {
          this.query.plans = this.$state.params.plan;
        } else {
          this.query.plans = this.$state.params.plan.split(',');
        }
      }
      if (this.$state.params.api_key) {
        this.query.api_key = this.$state.params.api_key;
      }
    }

    $onInit() {
      this.getSubscriptionAnalytics();
    }

    onPaginate(page) {
      this.query.page = page;
      this.doSearch();
    }

    clearFilters() {
      this.subscriptionsFiltersForm.$setPristine();
      this.query = new SubscriptionQuery();
      this.doSearch();
    }

    search() {
      this.query.page = 1;
      this.query.size = 10;
      this.doSearch();
    }

    buildQuery() {
      let query = '?page=' + this.query.page + '&size=' + this.query.size + '&';
      const parameters: any = {};

      if (this.query.status !== undefined) {
        parameters.status = this.query.status.join(',');
      }

      if (this.query.applications !== undefined) {
        parameters.application = this.query.applications.join(',');
      }

      if (this.query.plans !== undefined) {
        parameters.plan = this.query.plans.join(',');
      }

      if (this.query.api_key !== undefined) {
        parameters.api_key = this.query.api_key;
      }

      _.mapKeys(parameters, (value, key) => {
        return (query += key + '=' + value + '&');
      });

      return query;
    }

    doSearch() {
      const query = this.buildQuery();
      this.$state.transitionTo(
        this.$state.current,
        _.merge(this.$state.params, {
          status: this.query.status ? this.query.status.join(',') : '',
          application: this.query.applications ? this.query.applications.join(',') : '',
          plan: this.query.plans ? this.query.plans.join(',') : '',
          page: this.query.page,
          size: this.query.size,
          api_key: this.query.api_key,
        }),
        { notify: false },
      );

      this.ApiService.getSubscriptions(this.api.id, query).then((response) => {
        this.subscriptions = response.data as PagedResult;

        this.getSubscriptionAnalytics();
      });
    }

    getSubscriptionAnalytics() {
      if (this.subscriptions.data && this.subscriptions.data.length) {
        this.ApiService.analytics(this.api.id, {
          type: 'date_histo',
          aggs: 'field:subscription',
          interval: 86400000,
          from: moment().endOf('day').subtract(1, 'months'),
          to: moment().endOf('day'),
        }).then((result) => {
          if (result.data.values && result.data.values.length) {
            _.forEach(this.subscriptions.data, (subscription) => {
              const subBucket = _.find(result.data.values[0].buckets, (bucket) => {
                return bucket.name === subscription.id;
              });
              let subBucketData;
              if (subBucket) {
                subBucketData = subBucket.data;
              } else {
                if (
                  result.data.values[0].buckets[0] &&
                  result.data.values[0].buckets[0].data &&
                  result.data.values[0].buckets[0].data.length
                ) {
                  subBucketData = _.fill(Array(result.data.values[0].buckets[0].data.length), 0);
                } else {
                  subBucketData = [0];
                }
              }
              subscription.chartData = { series: [{ data: subBucketData }] };
            });
          }
        });
      }
    }

    showAddSubscriptionModal() {
      this.ApiService.getPublishedApiPlans(this.api.id).then((response) => {
        // Allow only subscribable plan
        const plans = _.filter(response.data, (plan: any) => {
          return plan.security !== PlanSecurityType.KEY_LESS;
        });

        this.$mdDialog
          .show({
            controller: 'DialogSubscriptionCreateController',
            controllerAs: 'dialogSubscriptionCreateController',
            template: require('./dialog/subscription.create.dialog.html'),
            clickOutsideToClose: true,
            locals: {
              api: this.api,
              plans: plans,
            },
          })
          .then((data) => {
            if (data && data.application && data.plan) {
              this.$shouldPromptForKeyMode(data.application, data.plan).then((shouldPrompt) => {
                if (shouldPrompt) {
                  this.selectKeyMode()
                    .then((mode) => this.ApplicationService.update({ ...data.application, api_key_mode: mode }))
                    .then(() => this.doSubscribe(data.application, data.plan, data.customApiKey));
                } else {
                  this.doSubscribe(data.application, data.plan, data.customApiKey);
                }
              });
            }
          });
      });
    }

    exportAsCSV() {
      this.ApiService.exportSubscriptionsAsCSV(this.api.id, this.buildQuery()).then((response) => {
        const hiddenElement = document.createElement('a');
        hiddenElement.href = 'data:attachment/csv,' + encodeURIComponent(response.data);
        hiddenElement.target = '_self';
        let fileName = 'subscriptions-' + this.api.name + '-' + this.api.version + '-' + _.now();
        fileName = fileName.replace(/[\s]/gi, '-');
        fileName = fileName.replace(/[^\w]/gi, '-');
        hiddenElement.download = fileName + '.csv';
        document.getElementById('hidden-export-container').appendChild(hiddenElement);
        this.$timeout(() => {
          hiddenElement.click();
        });
        document.getElementById('hidden-export-container').removeChild(hiddenElement);
      });
    }

    hasFilter() {
      return (
        _.difference(defaultStatus, this.query.status).length > 0 ||
        _.difference(this.query.status, defaultStatus).length > 0 ||
        (this.query.applications && this.query.applications.length) ||
        (this.query.plans && this.query.plans.length) ||
        this.query.api_key
      );
    }

    doSubscribe(application: any, plan: any, customApiKey: string) {
      this.ApiService.subscribe(this.api.id, application.id, plan.id, customApiKey).then((response) => {
        const subscription = response.data;
        this.NotificationService.show('A new subscription has been created.');
        this.$state.go('management.apis.detail.portal.subscriptions.subscription', { subscriptionId: subscription.id }, { reload: true });
      });
    }

    selectKeyMode() {
      const dialog = {
        controller: 'ApiKeyModeChoiceDialogController',
        controllerAs: '$ctrl',
        template: require('/src/components/dialog/apiKeyMode/api-key-mode-choice.dialog.html'),
        clickOutsideToClose: true,
      };

      return this.$mdDialog.show(dialog);
    }

    $shouldPromptForKeyMode(application: any, plan: any): IPromise<boolean> {
      if (plan.security === PlanSecurityType.API_KEY && this.allowsSharedApiKeys && application.api_key_mode === ApiKeyMode.UNSPECIFIED) {
        return this.$getApiKeySubscriptionsCount(application, plan).then((count) => {
          return count >= 1;
        });
      }
      return Promise.resolve(false);
    }

    $getApiKeySubscriptionsCount(application: any, plan: any): IPromise<number> {
      return this.ApplicationService.listSubscriptions(application.id, '?expand=security').then((response) => {
        const applicationSubscriptions = response.data as PagedResult;
        return applicationSubscriptions.data.filter(
          (subscription) => subscription.security === PlanSecurityType.API_KEY && subscription.api !== plan.api,
        ).length;
      });
    }

    get allowsSharedApiKeys(): boolean {
      return this.Constants.env?.settings?.plan?.security?.sharedApiKey?.enabled;
    }
  },
};

export default ApiSubscriptionsComponent;
