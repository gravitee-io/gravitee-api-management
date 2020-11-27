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
import { ChangeDetectorRef, Component, NgZone, OnInit } from '@angular/core';
import {
  Api,
  ApiService,
  Application,
  ApplicationService,
  Subscription,
  SubscriptionService
} from '../../../../projects/portal-webclient-sdk/src/lib';
import '@gravitee/ui-components/wc/gv-table';
import { TranslateService } from '@ngx-translate/core';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { Router } from '@angular/router';
import { getApplicationTypeIcon } from '@gravitee/ui-components/src/lib/theme';
import { ConfigurationService } from 'src/app/services/configuration.service';
import { getPictureDisplayName } from '@gravitee/ui-components/src/lib/item';
import StatusEnum = Subscription.StatusEnum;

@Component({
  selector: 'app-subscriptions',
  templateUrl: './subscriptions.component.html',
  styleUrls: ['./subscriptions.component.css']
})
export class SubscriptionsComponent implements OnInit {
  applications: Array<Application>;
  subscriptions: Array<Subscription>;
  subscriptionsMetadata: any;
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
    private ref: ChangeDetectorRef,
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
          type: 'gv-icon',
          width: '30px',
          attributes: {
            shape: (item) => getApplicationTypeIcon(item.applicationType),
            title: (item) => item.applicationType,
          },
        },
        {
          field: 'name',
          label: i18n('subscriptions.applications.name'),
        },
        { field: 'owner.display_name', label: i18n('subscriptions.applications.owner') },
        {
          type: 'gv-button',
          width: '30px',
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
        {
          field: (item) => this.subscriptionsMetadata[item.subscription.api].pictureUrl,
          type: 'image',
          alt: (item) => getPictureDisplayName(item.api)
        },
        {
          field: (item) => this.subscriptionsMetadata[item.subscription.api].name,
          tag: 'api.version',
          label: i18n('subscriptions.subscriptions.api'),
        },
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
        this.subscriptionsMetadata = responseSubscriptions.metadata;
        this.apiService.getApis({ size: -1 }).toPromise().then((responseApis) => {
          this.apis = responseApis.data;
          this.skeleton = false;
        });
      });
    });
  }

  async displayCurlExample(sub: any) {
    delete this.curlExample;
    this.ref.detectChanges();

    let keys;
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

    const entrypoints = this.subscriptionsMetadata[sub.subscription.api].entrypoints;
    const entrypoint = entrypoints && entrypoints[0] && entrypoints[0].target;
    if (entrypoint && keys[0]) {
      this.curlExample = `curl ${entrypoint} -H "${this.apikeyHeader}:${keys[0].id}"`;
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
        this.subsByApplication[this.selectedApplicationId] = applicationSubscriptions.map(applicationSubscription => {
          return {
            subscription: applicationSubscription,
            api: this.apis.find((api) => applicationSubscription.api === api.id),
            plan: this.subscriptionsMetadata[applicationSubscription.plan],
          };
        });
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
