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
import { CurrentUserService } from '../../services/current-user.service';
import { Api, Application, ApplicationService, Subscription, SubscriptionService, User } from '@gravitee/ng-portal-webclient';
import StatusEnum = Subscription.StatusEnum;
import { Router } from '@angular/router';
import { FeatureEnum } from '../../model/feature.enum';
import { ConfigurationService } from '../../services/configuration.service';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { TranslateService } from '@ngx-translate/core';
import { getApplicationTypeIcon } from '@gravitee/ui-components/src/lib/theme';
import '@gravitee/ui-components/wc/gv-button';
import '@gravitee/ui-components/wc/gv-table';
import '@gravitee/ui-components/wc/gv-stats';
import '@gravitee/ui-components/wc/gv-card-list';
import { AnalyticsService } from '../../services/analytics.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {

  public currentUser: User;
  applications: { item: Promise<Application>; metrics: Promise<{ subscribers: number }> }[];
  metrics: Array<any>;
  subscriptions: Array<any> = [];
  optionsSubscriptions: object;
  format: (key) => Promise<any>;
  stats: object;
  optionsStats: object;
  cardListGridTemplate: string;
  constructor(
    private currentUserService: CurrentUserService,
    private applicationService: ApplicationService,
    private subscriptionService: SubscriptionService,
    private router: Router,
    private config: ConfigurationService,
    private translateService: TranslateService,
    private analyticsService: AnalyticsService,
  ) {
  }

  ngOnInit() {
    this.currentUserService.get().subscribe((user) => {
      this.currentUser = user;
    });
    this.applicationService.getApplications({ size: 3, order: '-nbSubscriptions' }).toPromise().then(response => {
      this.applications = response.data.map((application, index) => {
        const metrics = this._getMetrics(application);
        const item = metrics.then(() =>  application);
        return { item, metrics };
      });
      this.cardListGridTemplate = `grid-template-columns: repeat(${this.applications?this.applications.length:0}, 1fr)`;
    });

    this.format = (key) => this.translateService.get(key).toPromise();
    this.optionsSubscriptions = {
      selectable: true,
      data: [
        {
          field: 'application._links.picture',
          type: 'image', alt: (item) => item.application.name + '  ' + item.application.applicationType,
          width: '10%',
        },
        {
          field: 'application.name',
          label: i18n('dashboard.subscriptions.application'),
          icon: (item) => getApplicationTypeIcon(item.applicationType),
          width: '30%',
        },
        { field: 'api.name', tag: 'api.version', label: i18n('dashboard.subscriptions.api'), width: '40%' },
        { field: 'plan.name', label: i18n('dashboard.subscriptions.plan'), width: '20%' },
      ]
    };
    this.analyticsService.getDefaultStatsOptions().then((result) => {
      this.optionsStats = result;
    });
  }

  private _getMetrics(application: Application) {
    return this.subscriptionService
      .getSubscriptions({ size: -1, applicationId: application.id, statuses: [StatusEnum.ACCEPTED] })
      .toPromise()
      .then((r) => {
        r.data.forEach(sub => {
          this.subscriptions = this.subscriptions.concat({
            application,
            api: { ...{ id: sub.api }, ...r.metadata[sub.api] },
            plan: r.metadata[sub.plan],
          });
        });
        return { subscribers: r.data.length };
      });
  }

  @HostListener(':gv-card-full:click', ['$event.detail'])
  goToApplication(application: Promise<Application>) {
    Promise.resolve(application).then((app) => {
      this.router.navigate(['/applications/' + app.id]);
    });
  }

  applicationCreationEnabled() {
    return this.applications && this.config.hasFeature(FeatureEnum.applicationCreation);
  }

  onSubscriptionClick({ detail }) {
    const item = detail.items[0];
    if (item) {
      const to = Date.now();
      const from = to - (7 * 24 * 60 * 60 * 1000);
      this.applicationService.getApplicationAnalytics(
        {
          applicationId: item.application.id,
          type: 'STATS',
          from,
          to,
          field: 'response-time',
          query: `(api:${item.api.id})`
        }).toPromise().then(response => this.stats = response);
    } else {
      delete this.stats;
    }
  }
}
