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
  private analyticsData: any;
  private events: any;
  private query: any;

  constructor(
    private EventsService,
    private AnalyticsService,
    private ApiService,
    private ApplicationService,
    private $scope,
    private Constants
  ) {
    'ngInject';
    this.eventLabels = {};
    this.eventTypes = [];
    this.selectedAPIs = [];
    this.selectedApplications = [];
    this.selectedEventTypes = [];

    this.$scope.platformDashboard = [{
      col: 0,
      row: 0,
      sizeY: 1,
      sizeX: 2,
      title: 'Top API',
      subhead: 'Ordered by API calls',
      chart: {
        type: 'table',
        selectable: true,
        link: 'api',
        columns: ['API', 'Hits'],
        paging: 5,
        request: {
          type: 'group_by',
          field: 'api',
          size: 10000
        }
      }
    }, {
      col: 2,
      row: 0,
      sizeY: 1,
      sizeX: 2,
      title: 'Top applications',
      subhead: 'Ordered by application calls',
      chart: {
        type: 'table',
        selectable: true,
        link: 'application',
        columns: ['Application', 'Hits'],
        paging: 5,
        request: {
          type: 'group_by',
          field: 'application',
          size: 10000
        }
      }
    }, {
      col: 0,
      row: 1,
      sizeY: 1,
      sizeX: 2,
      title: 'Top failed APIs',
      subhead: 'Order by API 5xx status calls',
      chart: {
        type: 'table',
        link: 'api',
        columns: ['API', 'Hits'],
        paging: 5,
        request: {
          type: 'group_by',
          field: 'api',
          query: 'status:[500 TO 599]',
          size: 10000
        }
      }
    }, {
      col: 2,
      row: 1,
      sizeY: 1,
      sizeX: 2,
      title: 'Top slow APIs',
      subhead: 'Order by API\'s average response time',
      chart: {
        type: 'table',
        link: 'api',
        columns: ['API', 'Response Time (in ms)'],
        paging: 5,
        request: {
          type: 'group_by',
          field: 'api',
          order: '-avg:response-time',
          size: 10000
        }
      }
    }, {
      col: 4,
      row: 1,
      sizeY: 1,
      sizeX: 2,
      title: 'Top overhead APIs',
      subhead: 'Order by Gateway\'s average latency',
      chart: {
        type: 'table',
        link: 'api',
        columns: ['API', 'Latency (in ms)'],
        paging: 5,
        request: {
          type: 'group_by',
          field: 'api',
          order: '-avg:proxy-latency',
          size: 10000
        }
      }
    },
    {
      col: 0,
      row: 2,
      sizeY: 1,
      sizeX: 6,
      title: 'Response Status',
      subhead: 'Hits repartition by HTTP Status',
      chart: {
        type: 'line',
        stacked: true,
        selectable: true,
        labelPrefix: 'HTTP Status',
        request: {
          type: 'date_histo',
          field: 'status',
          aggs: 'field:status'
        }
      }
    }, {
      col: 0,
      row: 3,
      sizeY: 1,
      sizeX: 6,
      title: 'Response times',
      subhead: 'Average response time for the gateway and the API',
      chart: {
        type: 'line',
        stacked: false,
        request: {
          type: 'date_histo',
          field: 'api',
          aggs: 'avg:response-time%3Bavg:api-response-time'
        },
        labels: ['Global latency (ms)', 'API latency (ms)']
      }
    },
  {
    col: 4,
    row: 0,
    sizeY: 1,
    sizeX: 2,
    title: 'Tenant repartition',
    subhead: 'Hits repartition by tenant',
    chart: {
      type: 'table',
      selectable: true,
      columns: ['Tenant', 'Hits'],
      paging: 5,
      request: {
        type: 'group_by',
        field: 'tenant',
        size: 20

      }
    }
  }];

    if (Constants.portal.dashboard && Constants.portal.dashboard.widgets) {
      let initialDashboardLength = this.$scope.platformDashboard.length;
      for (let i = 0; i < Constants.portal.dashboard.widgets.length; i++) {
        let nbWidget = this.$scope.platformDashboard.length - initialDashboardLength;
        let row = nbWidget > 2 ? 3 : 2;
        let col = nbWidget > 2 ? (nbWidget - 3) * 2 : nbWidget * 2;
        switch (Constants.portal.dashboard.widgets[i]) {
          case 'host':
            this.$scope.platformDashboard.push({
              row: row,
              col: col,
              sizeY: 1,
              sizeX: 2,
              title: 'Hits by Host ',
              subhead: 'Hits repartition by Host HTTP Header',
              chart: {
                type: 'table',
                selectable: true,
                columns: ['Host', 'Hits'],
                paging: 5,
                request: {
                  type: 'group_by',
                  field: 'host',
                  fieldLabel: 'host',
                  size: 20

                }
              }
            });
            break;
        }
      }
    }

    _.forEach(this.$scope.platformDashboard, (widget) => {
      _.merge(widget, {
        chart: {
          service: {
            caller: this.AnalyticsService,
            function: this.AnalyticsService.analytics
          }
        }
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

  onTimeframeChange(timeframe) {
    this.lastFrom = timeframe.from;
    this.lastTo = timeframe.to;

    this.searchEvents();
  }

  selectEvent(eventType) {
    let idx = this.selectedEventTypes.indexOf(eventType);
    if (idx > -1) {
      this.selectedEventTypes.splice(idx, 1);
    }
    else {
      this.selectedEventTypes.push(eventType);
    }
    this.searchEvents();
  }

  searchEvents() {
    // set apis
    let apis = this.selectedAPIs.map(function(api){ return api.id; }).join(',');
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
}

export default DashboardController;
