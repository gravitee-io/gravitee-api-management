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
    private $state
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
      subhead: 'Order by API response time calls',
      chart: {
        type: 'table',
        link: 'api',
        columns: ['API', 'Latency (in ms)'],
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
      subhead: 'Order by gateway latency',
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
      row: 3,
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
      row: 4,
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
    }, {
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
    }, {
        row: 3,
        col: 0,
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
      }];

    this.$scope.geoDashboard = [{
      row: 0,
      col: 0,
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
      row: 0,
      col: 2,
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
      row: 0,
      col: 4,
      sizeY: 1,
      sizeX: 2,
      title: 'Top plans',
      subhead: 'Hits repartition by API plan',
      chart: {
        type: 'table',
        selectable: true,
        columns: ['Plan', 'Hits'],
        paging: 5,
        request: {
          type: 'group_by',
          field: 'plan',
          size: 20
        }
      }
    }, {
      row: 1,
      col: 0,
      sizeY: 1,
      sizeX: 2,
      title: 'Geolocation by country',
      subhead: 'Hits repartition by country',
      chart: {
        type: 'table',
        selectable: true,
        columns: ['Country', 'Hits'],
        paging: 5,
        request: {
          type: 'group_by',
          field: 'geoip.country_iso_code',
          fieldLabel: 'country',
          size: 20
        }
      }
    }, {
      row: 2,
      col: 0,
      sizeY: 1,
      sizeX: 2,
      title: 'Geolocation by city',
      subhead: 'Hits repartition by city',
      chart: {
        type: 'table',
        selectable: true,
        columns: ['City', 'Hits'],
        paging: 5,
        request: {
          type: 'group_by',
          field: 'geoip.city_name',
          fieldLabel: 'city',
          size: 20
        }
      }
    }, {
      row: 1,
      col: 2,
      sizeY: 2,
      sizeX: 4,
      title: 'Geomap',
      subhead: 'Hits by location',
      chart: {
        type: 'map',
        request: {
          type: 'group_by',
          field: 'geoip.country_iso_code'
        }
      }
    }];

    this.$scope.deviceDashboard = [{
      row: 3,
      col: 0,
      sizeY: 1,
      sizeX: 2,
      title: 'Hits by user agent',
      subhead: 'Hits repartition by user agent name',
      chart: {
        type: 'table',
        selectable: true,
        columns: ['User agent name', 'Hits'],
        paging: 5,
        request: {
          type: 'group_by',
          field: 'user_agent.name',
          fieldLabel: 'User agent name',
          size: 20
        }
      }
    }, {
      row: 3,
      col: 2,
      sizeY: 1,
      sizeX: 2,
      title: 'Hits by OS',
      subhead: 'Hits repartition by OS name',
      chart: {
        type: 'table',
        selectable: true,
        columns: ['OS name', 'Hits'],
        paging: 5,
        request: {
          type: 'group_by',
          field: 'user_agent.os_name',
          fieldLabel: 'OS name',
          size: 20
        }
      }
    }];

    this.dashboards = [{
      id: 'global',
      label: 'Global dashboard',
      widgets: this.$scope.platformDashboard
    }, {
      id: 'geo',
      label: 'Geo dashboard',
      widgets: this.$scope.geoDashboard
    }, {
      id: 'device',
      label: 'Device dashboard',
      widgets: this.$scope.deviceDashboard
    }];

    let dashboardId = this.$state.params.dashboard;
    if (dashboardId) {
      this.dashboard = _.find(this.dashboards, {'id': dashboardId});
    } else {
      this.dashboard = this.dashboards[0];
    }

    _.forEach(this.dashboards, (dashboard) => {
      _.forEach(dashboard.widgets, (widget) => {
        _.merge(widget, {
          chart: {
            service: {
              caller: this.AnalyticsService,
              function: this.AnalyticsService.analytics
            }
          }
        });
      })
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
      _.merge(this.$state.params, {dashboard: dashboardId}));
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
