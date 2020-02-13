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
import { Component, OnInit } from '@angular/core';
import { Api, ApiService, Application, ApplicationService, Subscription, SubscriptionService } from '@gravitee/ng-portal-webclient';
import '@gravitee/ui-components/wc/gv-table';
import { TranslateService } from '@ngx-translate/core';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';

@Component({
  selector: 'app-subscriptions',
  templateUrl: './subscriptions.component.html',
  styleUrls: ['./subscriptions.component.css']
})
export class SubscriptionsComponent implements OnInit {
  applications: Array<Application>;
  subscriptions: Array<Subscription>;
  apis: Array<Api>;
  format: (key) => Promise<any>;
  options: Array<any>;
  optionsSubscriptions: Array<any>;
  subs: Array<any>;
  subsByApplication: any;
  emptyKeyApplications: string;
  emptyKeySubscriptions: string;

  constructor(
    private applicationService: ApplicationService,
    private subscriptionService: SubscriptionService,
    private apiService: ApiService,
    private translateService: TranslateService,
  ) {}

  ngOnInit() {
    this.subsByApplication = {};
    this.emptyKeyApplications = i18n('subscriptions.applications.init');
    this.emptyKeySubscriptions = i18n('subscriptions.subscriptions.init');
    this.options = [
      { data: '_links.picture', type: 'image', alt: 'name' },
      {
        data: 'name',
        label: i18n('subscriptions.applications.name'),
        icon: (item) => {
          switch (item.applicationType.toLowerCase()) {
            case 'browser':
            case 'web':
              return 'devices:laptop';
            case 'native':
              return 'devices:android';
            case 'backend_to_backend':
              return 'devices:server';
            default:
              return 'layout:layout-top-panel-2';
          }
        }
      },
      { data: 'owner.display_name', label: i18n('subscriptions.applications.owner') },
      { data: 'updated_at', type: 'date', label: i18n('subscriptions.applications.last_update') },
    ];
    this.optionsSubscriptions = [
      { data: 'api._links.picture', type: 'image', alt: 'name' },
      { data: 'api.name', tag: 'api.version', label: i18n('subscriptions.subscriptions.api') },
      { data: 'plan.name', label: i18n('subscriptions.subscriptions.plan') },
      { data: 'subscription.start_at', type: 'date', label: i18n('subscriptions.subscriptions.start_date') },
      { data: 'subscription.end_at', type: 'date', label: i18n('subscriptions.subscriptions.end_date') },
    ];
    this.format = (key) => this.translateService.get(key).toPromise();

    this.applicationService.getApplications({ size: -1 }).toPromise().then((response) => {
      this.applications = response.data;
      this.subscriptionService.getSubscriptions({ size: -1, statuses: [ 'ACCEPTED' ] }).toPromise().then((responseSubscriptions) => {
        this.subscriptions = responseSubscriptions.data;
        this.apiService.getApis({ size: -1 }).toPromise().then((responseApis) => {
          this.apis = responseApis.data;
        });
      });
    });
  }

  onApplicationFocus(event?) {
    this.selectSubscriptions(event);
  }

  onApplicationClick(event) {
    this.selectSubscriptions(event);
  }

  onFocusOut() {
    this.emptyKeySubscriptions = i18n('subscriptions.subscriptions.init');
    this.subs = [];
  }

  async selectSubscriptions(event?) {
    if (this.apis) {
      const applicationId = event ? event.detail.item.id : '';
      if (this.subsByApplication[applicationId]) {
        this.subs = this.subsByApplication[applicationId];
      } else {
        const applicationSubscriptions = applicationId ?
          this.subscriptions.filter((subscription) => applicationId === subscription.application) : this.subscriptions;
        const promises = applicationSubscriptions.map(applicationSubscription => {
          return this.apiService.getApiPlansByApiId({ apiId: applicationSubscription.api, size: -1 }).toPromise().then((response) => {
            return {
              subscription: applicationSubscription,
              api: this.apis.find((api) => applicationSubscription.api === api.id),
              plan: response.data.find((plan) => applicationSubscription.plan === plan.id),
            };
          });
        });
        this.subs = this.subsByApplication[applicationId] = await Promise.all(promises);
      }
      if (!this.subs || !this.subs.length) {
        this.emptyKeySubscriptions = i18n('subscriptions.subscriptions.empty');
      }
    }
  }
}
