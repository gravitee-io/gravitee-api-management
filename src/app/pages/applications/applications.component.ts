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
import { Application, ApplicationsService, SubscriptionService } from '@gravitee/ng-portal-webclient';
import '@gravitee/ui-components/wc/gv-table';

@Component({
  selector: 'app-applications',
  templateUrl: './applications.component.html',
  styleUrls: ['./applications.component.css']
})
export class ApplicationsComponent implements OnInit {
  nbApplications: number;
  applications: Array<Application>;
  metrics: Array<any>;

  constructor(
    private applicationsService: ApplicationsService,
    private subscriptionService: SubscriptionService,
  ) {
    this.metrics = [];
  }

  ngOnInit() {
    this.applicationsService.getApplications({ size: -1 }).toPromise().then((response) => {
      this.applications = response.data;
      this.metrics = this.applications.map((application) => this._getMetrics(application));
      // @ts-ignore
      this.nbApplications = response.metadata.data.total;
    });
  }
​
  private _getMetrics(application: Application) {
    return this.subscriptionService
      .getSubscriptions({ size: -1, applicationId: application.id, statuses: [ 'ACCEPTED' ] })
      .toPromise()
      .then((r) => ({ subscribers: r.data.length }));
  }
}
