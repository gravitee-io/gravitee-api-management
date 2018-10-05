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
import _ = require('lodash');
import { StateService } from '@uirouter/core';

class ApiAnalyticsController {

  private api: any;

  constructor(
    private ApiService,
    private resolvedApi,
    private $scope,
    private $state: StateService,
    private Constants: any) {
  'ngInject';
    this.ApiService = ApiService;
    this.$scope = $scope;
    this.api = resolvedApi.data;

    this.$scope.apiDashboard = [{
      row: 0,
      col: 0,
      sizeY: 1,
      sizeX: 2,
      title: 'Top applications',
      subhead: 'Ordered by application calls',
      chart: {
        type: 'table',
        selectable: true,
        columns: ['Application', 'Hits'],
        paging: 5,
        request: {
          type: 'group_by',
          field: 'application',
          size: 20
        }
      }
    }, {
      row: 0,
      col: 5,
      sizeY: 1,
      sizeX: 2,
      title: 'Status',
      chart: {
        type: 'pie',
        request: {
          type: 'group_by',
          field: 'status',
          ranges: '100:199%3B200:299%3B300:399%3B400:499%3B500:599'
        },
        labels: ['1xx', '2xx', '3xx', '4xx', '5xx'],
        colors: ['#42a5f5', '#66bb6a', '#ffee58', '#ff8f2d', '#ef5350']
      }
    }, {
      row: 1,
      col: 0,
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
      col: 2,
      sizeY: 1,
      sizeX: 2,
      title: 'Top paths',
      subhead: 'Hits repartition by path',
      chart: {
        type: 'table',
        selectable: true,
        columns: ['Path', 'Hits'],
        paging: 5,
        request: {
          type: 'group_by',
          field: 'path',
          size: 1000
        }
      }
    }, {
      row: 1,
      col: 4,
      sizeY: 1,
      sizeX: 2,
      title: 'Top mapped paths',
      subhead: 'Hits repartition by mapped path',
      chart: {
        type: 'table',
        selectable: true,
        columns: ['Mapped path', 'Hits'],
        paging: 5,
        request: {
          type: 'group_by',
          field: 'mapped-path',
          size: 1000
        }
      }
    }, {
      row: 3,
      col: 0,
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
      row: 4,
      col: 0,
      sizeY: 1,
      sizeX: 6,
      title: 'Response times',
      subhead: 'Average response time for the gateway and the API',
      chart: {
        type: 'line',
        stacked: false,
        request: {
          type: 'date_histo',
          aggs: 'avg:response-time%3Bavg:api-response-time'
        },
        labels: ['Global latency (ms)', 'API latency (ms)']
      }
    }, {
      row: 5,
      col: 0,
      sizeY: 1,
      sizeX: 6,
      title: 'Hits by application',
      subhead: 'Hits repartition by application',
      chart: {
        type: 'line',
        selectable: true,
        stacked: true,
        labelPrefix: '',
        request: {
          type: 'date_histo',
          field: 'application',
          aggs: 'field:application'
        }
      }
    }];

    if (Constants.portal.dashboard && Constants.portal.dashboard.widgets) {

      for (let i = 0; i < Constants.portal.dashboard.widgets.length; i++) {

        switch (Constants.portal.dashboard.widgets[i]) {
          case 'geo_country':
            this.$scope.apiDashboard.push({
              row: 4,
              col: i * 2,
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
            });
            break;
          case 'geo_city':
            this.$scope.apiDashboard.push({
              row: 4,
              col: i * 2,
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
            });
            break;
          case 'host':
            this.$scope.apiDashboard.push({
              row: 4,
              col: i * 2,
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

    let hasTenants = _.chain(this.api.proxy.groups)
                        .map((group) =>  group.endpoints)
                        .find((endpoint) => _.has(endpoint, 'tenants'));
    if (hasTenants === undefined) {
      this.$scope.apiDashboard.push({
        row: 0,
        col: 2,
        sizeY: 1,
        sizeX: 2,
        title: 'Top slow applications',
        subhead: 'Applications ordered by the latency',
        chart: {
          type: 'table',
          columns: ['Application', 'Latency (in ms)'],
          paging: 5,
          request: {
            type: 'group_by',
            field: 'application',
            order: '-avg:response-time',
            size: 20
          }
        }
      });
    } else {
      this.$scope.apiDashboard.push({
        row: 0,
        col: 2,
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
      });
    }

    var _that = this;

    _.forEach(this.$scope.apiDashboard, function (widget) {
      _.merge(widget, {
        root: _that.api.id,
        chart: {
          service: {
            caller: _that.ApiService,
            function: _that.ApiService.analytics
          }
        }
      });
    });
  }

  viewLogs() {
    // Update the query parameter
    this.$state.transitionTo(
      'management.apis.detail.analytics.logs',
      this.$state.params);
  }
}

export default ApiAnalyticsController;
