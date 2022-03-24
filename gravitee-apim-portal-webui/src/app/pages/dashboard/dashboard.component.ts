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
import { Component, HostListener, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { TranslateService } from '@ngx-translate/core';
import { getApplicationTypeIcon } from '@gravitee/ui-components/src/lib/theme';
import '@gravitee/ui-components/wc/gv-button';
import '@gravitee/ui-components/wc/gv-table';
import '@gravitee/ui-components/wc/gv-stats';
import '@gravitee/ui-components/wc/gv-card-list';
import { getPictureDisplayName } from '@gravitee/ui-components/src/lib/item';

import { CurrentUserService } from '../../services/current-user.service';
import { AnalyticsService } from '../../services/analytics.service';
import { ConfigurationService } from '../../services/configuration.service';
import { FeatureEnum } from '../../model/feature.enum';
import {
  Application,
  ApplicationService,
  Subscription,
  SubscriptionService,
  User,
} from '../../../../projects/portal-webclient-sdk/src/lib';

const StatusEnum = Subscription.StatusEnum;

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
})
export class DashboardComponent implements OnInit {
  public currentUser: User;
  applications: { item: Application; metrics: Promise<{ subscribers: { clickable: boolean; value: number; title: string } }> }[];
  metrics: Array<any>;
  subscriptions: Array<any> = [];
  optionsSubscriptions: object;
  format: (key) => Promise<any>;
  stats: object;
  optionsStats: object;
  cardListGridTemplate: string;
  empty: boolean;

  constructor(
    private currentUserService: CurrentUserService,
    private applicationService: ApplicationService,
    private subscriptionService: SubscriptionService,
    private router: Router,
    private config: ConfigurationService,
    private translateService: TranslateService,
    private analyticsService: AnalyticsService,
  ) {}

  ngOnInit() {
    this.currentUserService.get().subscribe(user => {
      this.currentUser = user;
    });
    if (this.hasApplicationPermission()) {
      this.applicationService
        .getApplications({ size: 3, order: '-nbSubscriptions' })
        .toPromise()
        .then(response => {
          const subscriptionsByAppId: { [p: string]: object } = response.metadata?.subscriptions;
          this.applications = response.data.map(application => {
            const applicationSubscriptions = (subscriptionsByAppId?.[application.id] as Array<Subscription>) || [];
            if (applicationSubscriptions.length > 0) {
              // Load subscriptions details
              this.subscriptionService
                .getSubscriptions({
                  size: -1,
                  applicationId: application.id,
                  statuses: [StatusEnum.ACCEPTED],
                })
                .toPromise()
                .then(subscriptionsResponse => {
                  subscriptionsResponse.data.forEach(sub => {
                    this.subscriptions = this.subscriptions.concat({
                      application,
                      api: { ...{ id: sub.api }, ...subscriptionsResponse.metadata[sub.api] },
                      plan: subscriptionsResponse.metadata[sub.plan],
                    });
                  });
                });
            }

            const metrics = this._getMetrics(application, applicationSubscriptions);
            return { item: application, metrics };
          });
          this.empty = this.applications.length === 0;
          this.cardListGridTemplate = `grid-template-columns: repeat(${this.applications ? this.applications.length : 0}, 1fr)`;
        });
    }

    this.format = key => this.translateService.get(key).toPromise();
    this.optionsSubscriptions = {
      selectable: true,
      data: [
        {
          field: 'application._links.picture',
          type: 'image',
          alt: item => getPictureDisplayName(item.application),
        },
        {
          field: 'name',
          type: 'gv-icon',
          width: '30px',
          attributes: {
            shape: item => getApplicationTypeIcon(item.application.applicationType),
            title: item => item.application.applicationType,
          },
        },
        {
          field: 'application.name',
          label: i18n('dashboard.subscriptions.application'),
        },
        { field: 'api.name', tag: 'api.version', label: i18n('dashboard.subscriptions.api') },
        { field: 'plan.name', label: i18n('dashboard.subscriptions.plan') },
      ],
    };
    this.analyticsService.getDefaultStatsOptions().then(result => {
      this.optionsStats = result;
    });
  }

  hasApplicationPermission() {
    return (
      this.currentUser && this.currentUser.permissions && this.currentUser.permissions.APPLICATION.find(permission => 'R' === permission)
    );
  }

  private async _getMetrics(application: Application, subscriptions: Array<Subscription>) {
    const count = subscriptions.length;
    const title = await this.translateService
      .get('applications.subscribers.title', {
        count,
        appName: application.name,
      })
      .toPromise();

    return { subscribers: { value: count, clickable: true, title } };
  }

  @HostListener(':gv-card-full:click', ['$event.detail'])
  goToApplication(application: Promise<Application>) {
    Promise.resolve(application).then(app => {
      this.router.navigate(['/applications/' + app.id]);
    });
  }

  @HostListener(':gv-metrics:click', ['$event.detail'])
  onClickToAppSubscribers({ key, item }) {
    if (key === 'subscribers') {
      this.router.navigate(['/applications/' + item.id + '/subscriptions']);
    }
  }

  applicationCreationEnabled() {
    return (
      this.applications &&
      this.config.hasFeature(FeatureEnum.applicationCreation) &&
      this.currentUser?.permissions?.APPLICATION.find(permission => 'C' === permission)
    );
  }

  onSubscriptionClick({ detail }) {
    const item = detail.items[0];
    if (item) {
      const to = Date.now();
      const from = to - 7 * 24 * 60 * 60 * 1000;
      this.applicationService
        .getApplicationAnalytics({
          applicationId: item.application.id,
          type: 'STATS',
          from,
          to,
          field: 'response-time',
          query: `(api:${item.api.id})`,
        })
        .toPromise()
        .then(response => (this.stats = response));
    } else {
      delete this.stats;
    }
  }
}
