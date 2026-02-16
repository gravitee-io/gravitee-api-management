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
import { IQService } from 'angular';
import { ActivatedRoute, Router } from '@angular/router';
import { filter, find, forEach, merge } from 'lodash';

import DashboardService from '../../../../services/dashboard.service';

class ApiAnalyticsOverviewControllerAjs {
  private api: any;
  private dashboard: any;
  private dashboards: any[];
  private activatedRoute: ActivatedRoute;

  constructor(
    private ApiService,
    private DashboardService: DashboardService,
    private $scope,
    private ngRouter: Router,
    private $q: IQService,
  ) {
    this.ApiService = ApiService;
    this.$scope = $scope;
  }

  $onInit() {
    this.$q
      .all({
        dashboards: this.DashboardService.list('API').then(response => response.data),
        api: this.ApiService.get(this.activatedRoute.snapshot.params.apiId).then(response => response.data),
      })
      .then(results => {
        this.api = results.api;
        this.dashboards = filter(results.dashboards, 'enabled');

        const dashboardId = this.activatedRoute.snapshot.queryParams.dashboard;
        if (dashboardId) {
          this.dashboard = find(this.dashboards, { id: dashboardId });
          if (!this.dashboard) {
            delete this.activatedRoute.snapshot.queryParams.dashboard;

            this.ngRouter.navigate(['.'], {
              relativeTo: this.activatedRoute,
              queryParams: {
                ...this.activatedRoute.snapshot.queryParams,
                dashboard: undefined,
              },
            });
          }
        } else {
          this.dashboard = this.dashboards[0];
        }

        forEach(this.dashboards, dashboard => {
          if (dashboard.definition) {
            dashboard.definition = JSON.parse(dashboard.definition);
          }
          forEach(dashboard.definition, widget => {
            merge(widget, {
              root: this.api.id,
              chart: {
                service: {
                  caller: this.ApiService,
                  function: this.ApiService.analytics,
                },
              },
            });
          });
        });
      });
  }

  viewLogs() {
    // Update the query parameter
    this.ngRouter.navigate(['../', 'analytics-logs'], {
      relativeTo: this.activatedRoute,
      queryParams: {
        ...this.activatedRoute.snapshot.queryParams,
      },
    });
  }

  onDashboardChanged() {
    this.$scope.$broadcast('dashboardReload');
    this.setDashboard(this.dashboard.id);
  }

  private setDashboard(dashboardId: string) {
    this.ngRouter.navigate(['.'], {
      relativeTo: this.activatedRoute,
      queryParams: {
        ...this.activatedRoute.snapshot.queryParams,
        dashboard: dashboardId,
      },
    });
  }
}
ApiAnalyticsOverviewControllerAjs.$inject = ['ApiService', 'DashboardService', '$scope', 'ngRouter', '$q'];

export default ApiAnalyticsOverviewControllerAjs;
