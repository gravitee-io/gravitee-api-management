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
import '@gravitee/ui-components/wc/gv-input';
import '@gravitee/ui-components/wc/gv-autocomplete';
import { ActivatedRoute, Router } from '@angular/router';
import { Component, HostListener, OnInit } from '@angular/core';
import { Dashboard } from '../../../../projects/portal-webclient-sdk/src/lib';
import { AnalyticsService } from '../../services/analytics.service';

@Component({
  selector: 'app-gv-select-dashboard',
  templateUrl: './gv-select-dashboard.component.html',
})
export class GvSelectDashboardComponent implements OnInit {
  dashboardsSelect: Array<any>;
  dashboard: Dashboard;

  constructor(public route: ActivatedRoute, public router: Router, public analyticsService: AnalyticsService) {}

  ngOnInit() {
    if (this.route.firstChild && this.route.firstChild.firstChild && this.route.firstChild.firstChild.firstChild) {
      const dashboards = this.route.firstChild.firstChild.firstChild.snapshot.data.dashboards;
      if (dashboards && dashboards.length) {
        if (this.route.snapshot.queryParams.dashboard) {
          this.dashboard = dashboards.find(dashboard => dashboard.id === this.route.snapshot.queryParams.dashboard);
        } else {
          this.dashboard = dashboards[0];
        }
        this._onChangDisplay(this.dashboard.id);
        this.dashboardsSelect = dashboards.map(dashboard => {
          return { label: dashboard.name, value: dashboard.id };
        });
      }
    }
  }

  @HostListener(':gv-select:select', ['$event.detail'])
  _onChangDisplay(id) {
    this.router.navigate([], {
      queryParams: { dashboard: id },
      queryParamsHandling: 'merge',
      fragment: this.analyticsService.fragment,
    });
  }
}
