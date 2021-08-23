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

import { ITimeframe, TimeframeRanges } from '../../../components/quick-time-range/quick-time-range.component';
import { EventService } from '../../../services/event.service';

class HomeDashboardController {
  private eventLabels: any;
  private eventTypes: any[];
  private selectedAPIs: any[];
  private selectedApplications: any[];
  private selectedEventTypes: any[];
  private events: any;
  private query: any;
  private dashboard: any;
  private customTimeframe: any;
  private customTimeframeLastDay: any;
  private timeframe: ITimeframe;

  constructor(
    private readonly eventService: EventService,
    private EnvironmentService,
    private $scope,
    private dashboards,
    private UserService,
  ) {
    'ngInject';
    this.eventLabels = {};
    this.eventTypes = [];
    this.selectedAPIs = [];
    this.selectedApplications = [];
    this.selectedEventTypes = [];
    this.dashboards = this.dashboards.filter((dashboards) => dashboards.enabled);

    this.timeframe = TimeframeRanges.LAST_MINUTE;

    if (this.dashboards.length > 0) {
      this.dashboard = this.dashboards[0];
    }

    if (this.dashboard.definition) {
      this.dashboard.definition = JSON.parse(this.dashboard.definition);
      this.dashboard.definition.forEach((widget) => {
        _.merge(widget, {
          chart: {
            service: {
              caller: this.EnvironmentService,
              function: this.EnvironmentService.analytics,
            },
          },
        });
      });
    }

    // init events
    this.eventLabels.start_api = 'Start';
    this.eventLabels.stop_api = 'Stop';
    this.eventLabels.publish_api = 'Deploy';
    this.eventLabels.unpublish_api = 'Undeploy';
    this.eventTypes = ['START_API', 'STOP_API', 'PUBLISH_API', 'UNPUBLISH_API'];

    this.initPagination();
    this.refresh();

    // Need to bind the function here to have the correct `this` when it is called by MdTable (see the html)
    this.searchEvents = this.searchEvents.bind(this);
  }

  searchEvents() {
    if (
      this.UserService.currentUser &&
      (this.UserService.currentUser.userPermissions.includes('api-event-r') ||
        this.UserService.currentUser.userEnvironmentPermissions.includes('environment-platform-r'))
    ) {
      // set apis
      const apis = this.selectedAPIs.map((api) => api.id).join(',');
      // set event types
      let types: any = this.eventTypes;
      if (this.selectedEventTypes.length > 0) {
        types = this.selectedEventTypes.join(',');
      }

      // search
      this.$scope.eventsFetchData = true;
      this.eventService
        .search(types, apis, this.customTimeframe.from, this.customTimeframe.to, this.query.page - 1, this.query.limit)
        .then((response) => {
          this.events = response.data;
          this.$scope.eventsFetchData = false;
        });
    }
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

  refresh(timeframe?: ITimeframe) {
    if (timeframe) {
      this.timeframe = timeframe;
    }
    const now = Date.now();

    this.customTimeframe = {
      interval: this.timeframe.interval,
      from: now - this.timeframe.range,
      to: now,
    };

    this.searchEvents();
  }
}

export default HomeDashboardController;
