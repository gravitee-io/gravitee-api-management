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
import { Component, HostListener, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Application, Dashboard } from '@gravitee/ng-portal-webclient';
import '@gravitee/ui-components/wc/gv-chart-line';
import '@gravitee/ui-components/wc/gv-chart-pie';
import '@gravitee/ui-components/wc/gv-chart-map';
import '@gravitee/ui-components/wc/gv-stats';
import { GvAnalyticsDashboardComponent } from '../../../components/gv-analytics-dashboard/gv-analytics-dashboard.component';
import { GvAnalyticsFiltersComponent } from '../../../components/gv-analytics-filters/gv-analytics-filters.component';

@Component({
  selector: 'app-application-analytics',
  templateUrl: './application-analytics.component.html',
  styleUrls: ['./application-analytics.component.css']
})
export class ApplicationAnalyticsComponent implements OnInit {

  application: Application;
  dashboards: Array<any>;
  dashboardsSelect: Array<any>;
  dashboard: Dashboard;

  @ViewChild(GvAnalyticsFiltersComponent, { static: false })
  filtersComponent: GvAnalyticsFiltersComponent;
  @ViewChild(GvAnalyticsDashboardComponent, { static: false })
  dashboardComponent: GvAnalyticsDashboardComponent;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    ) {
  }

  ngOnInit() {
    this.application = this.route.snapshot.data.application;
    this.dashboards = this.route.snapshot.data.dashboards;
    if (this.dashboards) {
      if (this.route.snapshot.queryParams.dashboard) {
        this.dashboard = this.dashboards.find((dashboard) => dashboard.id === this.route.snapshot.queryParams.dashboard);
      } else {
        this.dashboard = this.dashboards[0];
      }
      this.dashboardsSelect = this.dashboards.map((dashboard) => {
        return { label: dashboard.name, value: dashboard.id };
      });
    }
  }

  @HostListener(':gv-select:select', ['$event.detail'])
  _onChangDisplay({ id }) {
    this.router.navigate([], {
      queryParams: { dashboard: id },
      queryParamsHandling: 'merge',
      fragment: 'dashboard'
    }).then(() => {
      this.dashboardComponent.dashboard = this.dashboards.find((dashboard) => dashboard.id === id);
      this.filtersComponent.reset();
      this.dashboardComponent.refresh();
    });
  }
}
