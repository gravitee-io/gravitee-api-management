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
import { ActivatedRoute, Router } from '@angular/router';
import { filter, find, forEach, merge } from 'lodash';

import { EventService } from '../../../../services/event.service';
import DashboardService from '../../../../services/dashboard.service';

class AnalyticsDashboardControllerAjs {
  private eventLabels: any;
  private eventTypes: any[];
  private selectedAPIs: any[];
  private selectedApplications: any[];
  private selectedEventTypes: any[];
  private lastFrom: any;
  private lastTo: any;
  private events: any;
  private query: any;
  private dashboard: any;
  private dashboards: any;
  private activatedRoute: ActivatedRoute;

  constructor(
    private eventService: EventService,
    private AnalyticsService,
    private ApiService,
    private ApplicationService,
    private $scope,
    private Constants,
    private dashboardService: DashboardService,
    private ngRouter: Router,
  ) {
    this.eventLabels = {};
    this.eventTypes = [];
    this.selectedAPIs = [];
    this.selectedApplications = [];
    this.selectedEventTypes = [];

    // init events
    this.eventLabels.start_api = 'Start';
    this.eventLabels.stop_api = 'Stop';
    this.eventLabels.publish_api = 'Deploy';
    this.eventLabels.unpublish_api = 'Undeploy';
    this.eventTypes = ['START_API', 'STOP_API', 'PUBLISH_API', 'UNPUBLISH_API'];

    this.initPagination();
  }

  $onInit() {
    this.dashboardService.list('PLATFORM').then((response) => {
      this.dashboards = filter(response.data, 'enabled');

      const dashboardId = this.activatedRoute.snapshot.queryParams.dashboard;
      if (dashboardId) {
        this.dashboard = find(this.dashboards, { id: dashboardId });
        if (!this.dashboard) {
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

      forEach(this.dashboards, (dashboard) => {
        if (dashboard.definition) {
          dashboard.definition = JSON.parse(dashboard.definition);
        }
        forEach(dashboard.definition, (widget) => {
          merge(widget, {
            chart: {
              service: {
                caller: this.AnalyticsService,
                function: this.AnalyticsService.analytics,
              },
            },
          });
        });
      });

      this.searchEvents = this.searchEvents.bind(this);
    });
  }

  onDashboardChanged(dashboardId: string) {
    this.ngRouter.navigate(['.'], {
      relativeTo: this.activatedRoute,
      queryParams: {
        ...this.activatedRoute.snapshot.queryParams,
        dashboard: dashboardId,
      },
    });
  }

  onTimeframeChange(timeframe) {
    this.lastFrom = timeframe.from;
    this.lastTo = timeframe.to;

    // display events only on first dashboard
    if (this.dashboard === this.dashboards[0]) {
      this.searchEvents();
    }
  }

  selectEvent(eventType) {
    const idx = this.selectedEventTypes.indexOf(eventType);
    if (idx > -1) {
      this.selectedEventTypes.splice(idx, 1);
    } else {
      this.selectedEventTypes.push(eventType);
    }
    this.searchEvents();
  }

  searchEvents() {
    // set apis
    const apis = this.selectedAPIs
      .map((api) => {
        return api.id;
      })
      .join(',');
    // set event types
    // TODO: types is type any[], and then string !!! beurk beurk beurk
    let types: any = this.eventTypes;
    if (this.selectedEventTypes.length > 0) {
      types = this.selectedEventTypes.join(',');
    }

    // search
    this.$scope.eventsFetchData = true;
    this.eventService.search(types, apis, this.lastFrom, this.lastTo, this.query.page - 1, this.query.limit).then((response) => {
      this.events = response.data;
      this.$scope.eventsFetchData = false;
    });
  }

  initPagination() {
    this.query = {
      limit: 10,
      page: 1,
    };
  }

  getEventLabel(label) {
    return this.eventLabels[label];
  }

  viewLogs() {
    this.ngRouter.navigate(['../logs'], {
      relativeTo: this.activatedRoute,
      queryParams: {
        ...this.activatedRoute.snapshot.queryParams,
      },
    });
  }

  gotToApiAnalytics(apiId: string) {
    this.ngRouter.navigate(['../../apis', apiId, 'v2', 'analytics-overview'], {
      relativeTo: this.activatedRoute,
      queryParamsHandling: 'preserve',
    });
  }
}
AnalyticsDashboardControllerAjs.$inject = [
  'eventService',
  'AnalyticsService',
  'ApiService',
  'ApplicationService',
  '$scope',
  'Constants',
  'DashboardService',
  'ngRouter',
];

const AnalyticsDashboardComponentAjs: ng.IComponentOptions = {
  bindings: {
    activatedRoute: '<',
  },
  template: require('html-loader!./analytics-dashboard.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
  controller: AnalyticsDashboardControllerAjs,
};

export default AnalyticsDashboardComponentAjs;
