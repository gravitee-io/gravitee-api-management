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
import { Component, OnInit, NgZone } from '@angular/core';
import { Api, ApiService, Application, ApplicationService, Subscription, SubscriptionService } from '@gravitee/ng-portal-webclient';
import '@gravitee/ui-components/wc/gv-table';
import { TranslateService } from '@ngx-translate/core';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { Router } from '@angular/router';
import { getApplicationTypeIcon } from '@gravitee/ui-components/src/lib/theme';
import StatusEnum = Subscription.StatusEnum;
import { ConfigurationService } from 'src/app/services/configuration.service';
import { getPictureDisplayName } from '@gravitee/ui-components/src/lib/item';

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
  options: object;
  optionsSubscriptions: object;
  subs: Array<any>;
  subsByApplication: any;
  emptyKeyApplications: string;
  emptyKeySubscriptions: string;
  selectedApplicationId: string;
  selectedSubscriptions: Array<any>;
  apikeyHeader: string;
  skeleton = true;
  public curlExample: string;

  constructor(
    private applicationService: ApplicationService,
    private subscriptionService: SubscriptionService,
    private apiService: ApiService,
    private translateService: TranslateService,
    private router: Router,
    private configurationService: ConfigurationService,
    private ngZone: NgZone,
  ) {
  }

  ngOnInit() {
    this.subsByApplication = {};
    this.subs = [];
    this.emptyKeyApplications = i18n('subscriptions.applications.init');
    this.emptyKeySubscriptions = i18n('subscriptions.subscriptions.init');
    this.apikeyHeader = this.configurationService.get('portal.apikeyHeader');
    this.options = {
      selectable: true,
      data: [
        { field: '_links.picture', type: 'image', alt: (item) => getPictureDisplayName(item) },
        {
          field: 'name',
          label: i18n('subscriptions.applications.name'),
          icon: (item) => getApplicationTypeIcon(item.applicationType),
          iconTitle: (item) => item.applicationType,
        },
        { field: 'owner.display_name', label: i18n('subscriptions.applications.owner') },
        {
          type: 'gv-button',
          width: '25px',
          attributes: {
            link: true,
            href: (item) => `/applications/${item.id}`,
            title: i18n('subscriptions.applications.navigate'),
            icon: 'communication:share',
            onClick: (item, e) => this.goToApplication(item.id),
          }
        },
      ]
    };
    this.optionsSubscriptions = {
      selectable: true,
      data: [
        { field: 'api._links.picture', type: 'image', alt: (item) => getPictureDisplayName(item.api) },
        { field: 'api.name', tag: 'api.version', label: i18n('subscriptions.subscriptions.api') },
        { field: 'plan.name', label: i18n('subscriptions.subscriptions.plan') },
        { field: 'subscription.start_at', type: 'date', label: i18n('subscriptions.subscriptions.start_date') },
        { field: 'subscription.end_at', type: 'date', label: i18n('subscriptions.subscriptions.end_date') },
        {
          type: 'gv-button',
          width: '25px',
          attributes: {
            link: true,
            href: (item) => `/applications/${this.selectedApplicationId}/subscriptions?subscription=${item.subscription.id}`,
            title: i18n('subscriptions.subscriptions.navigate'),
            icon: 'communication:share',
            onClick: (item) => this.goToSubscription(item.subscription.id),
          }
        },
      ]
    };
    this.format = (key) => this.translateService.get(key).toPromise();

    this.applicationService.getApplications({ size: -1 }).toPromise().then((response) => {
      this.applications = response.data;
      this.subscriptionService.getSubscriptions({ size: -1, statuses: [StatusEnum.ACCEPTED] }).toPromise().then((responseSubscriptions) => {
        this.subscriptions = responseSubscriptions.data;
        this.apiService.getApis({ size: -1 }).toPromise().then((responseApis) => {
          this.apis = responseApis.data;
          this.skeleton = false;
        });
      });
    });
  }

  async displayCurlExample(sub: any) {
    let entrypoints = [];
    if (!sub.api.entrypoints || !sub.api.entrypoints[0]) {
      const subscribedApi = await this.apiService.getApiByApiId({ apiId: sub.api.id }).toPromise();
      entrypoints = subscribedApi.entrypoints;
      this.apis.find((api) => sub.api.id === api.id).entrypoints = subscribedApi.entrypoints;
    } else {
      entrypoints = sub.api.entrypoints;
    }

    let keys = [];
    if (!sub.subscription.keys || !sub.subscription.keys[0]) {
      const subscriptionDetail = await this.subscriptionService.getSubscriptionById({
        subscriptionId: sub.subscription.id,
        include: ['keys']
      }).toPromise();
      keys = subscriptionDetail.keys;
      this.subscriptions.find((subscription) => sub.subscription.id === subscription.id).keys = subscriptionDetail.keys;
    } else {
      keys = sub.subscription.keys;
    }

    if (entrypoints[0] && keys[0]) {
      this.curlExample = `$ curl ${entrypoints[0]} -H "${this.apikeyHeader}:${keys[0].id}"`;
    }
  }

  goToApplication(appId: string) {
    this.ngZone.run(() => this.router.navigate(['/applications', appId]));
  }

  goToSubscription(subId: string) {
    this.ngZone.run(() =>
      this.router.navigate(['/applications', this.selectedApplicationId, 'subscriptions'], { queryParams: { subscription: subId } } )
    );
  }

  onApplicationMouseEnter({ detail }) {
    this.selectSubscriptions(detail.item);
  }

  onApplicationClick({ detail }) {
    this.selectSubscriptions(detail.items[0]);
  }

  onSubscriptionClick({ detail }) {
    if (detail.items[0]) {
      this.displayCurlExample(detail.items[0]);
    } else {
      this.curlExample = null;
    }
  }

  onFocusOut() {
    this.emptyKeySubscriptions = i18n('subscriptions.subscriptions.init');
    this.subs = [];
  }

  get rowsHeight() {
    return this.curlExample ? '25vh' : '45vh';
  }

  async selectSubscriptions(application) {
    this.selectedApplicationId = application ? application.id : null;
    this.curlExample = null;
    this.selectedSubscriptions = [];
    if (this.apis && this.selectedApplicationId) {
      if (this.subsByApplication[this.selectedApplicationId] == null) {
        const applicationSubscriptions = this.selectedApplicationId ?
          this.subscriptions.filter((subscription) => this.selectedApplicationId === subscription.application) : this.subscriptions;
        const promises = applicationSubscriptions.map(applicationSubscription => {
          return this.apiService.getApiPlansByApiId({ apiId: applicationSubscription.api, size: -1 }).toPromise().then((response) => {
            return {
              subscription: applicationSubscription,
              api: this.apis.find((api) => applicationSubscription.api === api.id),
              plan: response.data.find((plan) => applicationSubscription.plan === plan.id),
            };
          });
        });
        this.subsByApplication[this.selectedApplicationId] = await Promise.all(promises);
      }

      this.subs = this.subsByApplication[this.selectedApplicationId];

    } else {
      this.subs = [];
    }
    if (!this.subs || !this.subs.length) {
      this.emptyKeySubscriptions = i18n('subscriptions.subscriptions.empty');
    }
  }
}
