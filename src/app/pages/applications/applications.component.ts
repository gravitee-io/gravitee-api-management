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
import {
  Application,
  ApplicationService,
  Subscription,
  SubscriptionService
} from '../../../../projects/portal-webclient-sdk/src/lib';
import '@gravitee/ui-components/wc/gv-card-list';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import StatusEnum = Subscription.StatusEnum;

@Component({
  selector: 'app-applications',
  templateUrl: './applications.component.html',
  styleUrls: ['./applications.component.css']
})
export class ApplicationsComponent implements OnInit {
  nbApplications: number;
  applications: { item: Application; metrics: Promise<{ subscribers: { clickable: boolean; value: number } }> }[] = [];
  metrics: Array<any>;
  empty: boolean;

  constructor(
    private applicationService: ApplicationService,
    private subscriptionService: SubscriptionService,
    private router: Router,
    private translateService: TranslateService,
  ) {
    this.metrics = [];
  }

  ngOnInit() {
    this.empty = false;
    this.applicationService.getApplications({ size: -1 }).toPromise().then((response) => {
      // @ts-ignore
      this.nbApplications = response.metadata.data.total;
      this.applications = response.data.map((application) => ({
        item: application,
        metrics: this._getMetrics(application)
      }));
      this.empty = this.applications.length === 0;
    });
  }

  private _getMetrics(application: Application) {
    return this.subscriptionService
      .getSubscriptions({ size: -1, applicationId: application.id, statuses: [StatusEnum.ACCEPTED] })
      .toPromise()
      .then(async (r) => {
        const count = r.data.length;
        const title = await this.translateService.get('applications.subscribers.title', {
          count,
          appName: application.name,
        }).toPromise();
        return {
          subscribers: {
            value: r.data.length,
            clickable: true,
            title
          }
        };
      });
  }

  @HostListener(':gv-card-full:click', ['$event.detail'])
  onClickToApp(application: Promise<Application>) {
    Promise.resolve(application).then((_application) => {
      this.router.navigate(['/applications', _application.id]);
    });
  }

  @HostListener(':gv-metrics:click', ['$event.detail'])
  onClickToAppSubscribers({ key, item }) {
    if (key === 'subscribers') {
      this.router.navigate(['/applications/' + item.id + '/subscriptions']);
    }
  }

}
