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
import * as _ from 'lodash';

class DashboardController {
  private eventLabels: any;
  private eventTypes: any[];
  private selectedAPIs: any[];
  private selectedApplications: any[];
  private selectedEventTypes: any[];
  private lastFrom: any;
  private lastTo: any;
  private events: any;
  private query: any;
  private dashboards: any;
  private dashboard: any;

  constructor(
    private EventsService,
    private AnalyticsService,
    private ApiService,
    private ApplicationService,
    private $scope,
    private Constants,
    private $state,
    private dashboards
  ) {
    'ngInject';
    this.eventLabels = {};
    this.eventTypes = [];
    this.selectedAPIs = [];
    this.selectedApplications = [];
    this.selectedEventTypes = [];
    this.dashboards = _.filter(this.dashboards, 'enabled');

    let dashboardId = this.$state.params.dashboard;
    if (dashboardId) {
      this.dashboard = _.find(this.dashboards, {id: dashboardId});
      if (!this.dashboard) {
        delete this.$state.params.dashboard;
        this.$state.go(this.$state.current);
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
          chart: {
            service: {
              caller: this.AnalyticsService,
              function: this.AnalyticsService.analytics
            }
          }
        });
      });
    });

    // init events
    this.eventLabels.start_api = 'Start';
    this.eventLabels.stop_api = 'Stop';
    this.eventLabels.publish_api = 'Deploy';
    this.eventLabels.unpublish_api = 'Undeploy';
    this.eventTypes = ['START_API', 'STOP_API', 'PUBLISH_API', 'UNPUBLISH_API'];

    this.initPagination();
    this.searchEvents = this.searchEvents.bind(this);
  }

  onDashboardChanged(dashboardId: string) {
    this.$state.transitionTo(
      this.$state.current,
      _.merge(this.$state.params, {dashboard: dashboardId}), {reload: true});
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
    let idx = this.selectedEventTypes.indexOf(eventType);
    if (idx > -1) {
      this.selectedEventTypes.splice(idx, 1);
    } else {
      this.selectedEventTypes.push(eventType);
    }
    this.searchEvents();
  }

  searchEvents() {
    // set apis
    let apis = this.selectedAPIs.map(function(api) { return api.id; }).join(',');
    // set event types
    // TODO: types is type any[], and then string !!! beurk beurk beurk
    let types: any = this.eventTypes;
    if (this.selectedEventTypes.length > 0) {
      types = this.selectedEventTypes.join(',');
    }

    // search
    this.$scope.eventsFetchData = true;
    this.EventsService.search(types, apis, this.lastFrom, this.lastTo, this.query.page - 1, this.query.limit).then(response => {
      this.events = response.data;
      this.$scope.eventsFetchData = false;
    });
  }

  initPagination() {
    this.query = {
      limit: 10,
      page: 1
    };
  }

  getEventLabel(label) {
    return this.eventLabels[label];
  }

  viewLogs() {
    // Update the query parameter
    this.$state.transitionTo(
      'management.logs',
      this.$state.params);
  }
}

export default DashboardController;
