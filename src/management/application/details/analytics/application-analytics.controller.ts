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

import ApplicationService from '../../../../services/applications.service';
import { StateService } from '@uirouter/core';

class ApplicationAnalyticsController {

  private application: any;
  private applicationDashboard: any;

  constructor(
    private ApplicationService: ApplicationService,
    private $state: StateService,
    private Constants: any
  ) {
    'ngInject';

    this.applicationDashboard = [{
      col: 0,
      row: 1,
      sizeY: 1,
      sizeX: 2,
      title: "Top API",
      subhead: 'Ordered by API calls',
      chart: {
        type: 'table',
        selectable: true,
        columns: ['API', 'Hits'],
        paging: 5,
        request: {
          type: "group_by",
          field: "api",
          size: 20
        }
      }
    }, {
      col: 2,
      row: 1,
      sizeY: 1,
      sizeX: 2,
      title: 'Top failed APIs',
      subhead: 'Order by API 5xx status calls',
      chart: {
        type: 'table',
        selectable: true,
        columns: ['API', 'Hits'],
        paging: 5,
        request: {
          type: 'group_by',
          field: 'api',
          query: 'status:[500 TO 599]',
          size: 20
        }
      }
    }, {
      col: 4,
      row: 1,
      sizeY: 1,
      sizeX: 2,
      title: "Top slow API",
      subhead: 'Order by API response time calls',
      chart: {
        type: 'table',
        selectable: true,
        columns: ['API', 'Latency (in ms)'],
        paging: 5,
        request: {
          type: 'group_by',
          field: 'api',
          order: '-avg:response-time',
          size: 20
        }
      }
    }, {
      col: 0,
      row: 0,
      sizeY: 1,
      sizeX: 2,
      title: "Status",
      chart: {
        type: 'pie',
        request: {
          type: "group_by",
          field: "status",
          ranges: "100:199%3B200:299%3B300:399%3B400:499%3B500:599"
        },
        labels: ["1xx", "2xx", "3xx", "4xx", "5xx"],
        colors: ['#42a5f5', '#66bb6a', '#ffee58', '#ef5350', '#8d6e63']
      }
    }, {
      col: 0,
      row: 2,
      sizeY: 1,
      sizeX: 3,
      title: 'Top paths',
      subhead: 'Hits repartition by path',
      chart: {
        type: 'table',
        selectable: true,
        columns: ['Mapped path', 'Hits'],
        paging: 5,
        request: {
          type: 'group_by',
          field: 'path',
          size: 1000
        }
      }
    }, {
      col: 3,
      row: 2,
      sizeY: 1,
      sizeX: 3,
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
      col: 2,
      row: 0,
      sizeY: 1,
      sizeX: 4,
      title: "Response Status",
      subhead: "Hits repartition by HTTP Status",
      chart: {
        type: 'line',
        stacked: true,
        labelPrefix: 'HTTP Status',
        request: {
          "type": "date_histo",
          "aggs": "field:status"
        }
      }
    }, {
      col: 0,
      row: 4,
      sizeY: 1,
      sizeX: 6,
      title: "Response times",
      subhead: "Average response time",
      chart: {
        type: 'line',
        stacked: false,
        request: {
          "type": "date_histo",
          "aggs": "avg:response-time"
        },
        labels: ["Global latency (ms)"]
      }
    }, {
      col: 0,
      row: 5,
      sizeY: 1,
      sizeX: 6,
      title: "Hits by API",
      subhead: "Hits repartition by API",
      chart: {
        type: 'line',
        stacked: true,
        labelPrefix: '',
        request: {
          "type": "date_histo",
          "aggs": "field:api"
        }
      }
    }];

    if (Constants.portal.dashboard && Constants.portal.dashboard.widgets) {

      for (let i = 0; i < Constants.portal.dashboard.widgets.length; i++) {

        switch (Constants.portal.dashboard.widgets[i]) {
          case 'geo_country':
          this.applicationDashboard.push({
              col: i * 3,
              row: 5,
              sizeY: 1,
              sizeX: 3,
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
                  ize: 20

                }
              }
            });
            break;
          case 'geo_city':
            this.applicationDashboard.push({
              col: i * 3,
              row: 5,
              sizeY: 1,
              sizeX: 3,
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
        }
      };
    }

  }

  $onInit() {
    const that = this;

    _.forEach(this.applicationDashboard, function (widget) {
      _.merge(widget, {
        root: that.application.id,
        chart: {
          service: {
            caller: that.ApplicationService,
            function: that.ApplicationService.analytics
          }
        }
      });
    });
  }

  viewLogs() {
    // update the query parameter
    this.$state.transitionTo(
      'management.applications.application.logs',
      this.$state.params);
  }
}

export default ApplicationAnalyticsController;
