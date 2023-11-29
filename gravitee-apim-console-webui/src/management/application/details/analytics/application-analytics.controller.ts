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
import { StateService } from '@uirouter/core';
import * as _ from 'lodash';
import { ActivatedRoute, Router } from '@angular/router';

import ApplicationService from '../../../../services/application.service';
import DashboardService from '../../../../services/dashboard.service';

class ApplicationAnalyticsController {
  private application: any;
  private dashboard: any;
  private dashboards: any;
  private activatedRoute: ActivatedRoute;

  constructor(
    private ApplicationService: ApplicationService,
    private DashboardService: DashboardService,
    private $state: StateService,
    private ngRouter: Router,
  ) {}

  $onInit() {
    this.DashboardService.list('APPLICATION', true).then((response) => {
      this.dashboards = _.filter(response.data, 'enabled');

      const dashboardId = this.activatedRoute.snapshot.queryParams.dashboard;
      if (dashboardId) {
        this.dashboard = _.find(this.dashboards, { id: dashboardId });
        if (!this.dashboard) {
          const params = { ...this.activatedRoute.snapshot.queryParams };
          delete params.dashboard;
          this.ngRouter.navigate(['./'], {
            relativeTo: this.activatedRoute,
            queryParams: params,
          });
        }
      } else {
        this.dashboard = this.dashboards[0];
      }

      _.forEach(this.dashboards, (dashboard) => {
        if (dashboard.definition) {
          dashboard.definition = JSON.parse(dashboard.definition);
        }
        _.forEach(dashboard.definition, (widget) => {
          _.merge(widget, {
            root: this.application.id,
            chart: {
              service: {
                caller: this.ApplicationService,
                function: this.ApplicationService.analytics,
              },
            },
          });
        });
      });
    });
  }

  onDashboardChanged() {
    this.ngRouter.navigate(['./'], {
      relativeTo: this.activatedRoute,
      queryParams: {
        ...this.activatedRoute.snapshot.queryParams,
        dashboard: this.dashboard.id,
      },
    });
  }

  viewLogs() {
    // update the query parameter
    this.ngRouter.navigate(['../', 'logs'], { relativeTo: this.activatedRoute });
  }
}
ApplicationAnalyticsController.$inject = ['ApplicationService', 'DashboardService', '$state', 'ngRouter'];

export default ApplicationAnalyticsController;
