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

import ApplicationService from '../../../services/applications.service';

class ApplicationAnalyticsController {

  private application: any;
  private applicationDashboard: any;

  constructor(
    private ApplicationService: ApplicationService
  ) {
    'ngInject';

    this.applicationDashboard = [{
      col: 0,
      row: 0,
      sizeY: 1,
      sizeX: 3,
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
      col: 3,
      row: 0,
      sizeY: 1,
      sizeX: 3,
      title: "Status",
      chart: {
        type: 'pie',
        request: {
          type: "group_by",
          field: "status",
          ranges: "100:199;200:299;300:399;400:499;500:599"
        },
        labels: ["1xx", "2xx", "3xx", "4xx", "5xx"],
        colors: ['#42a5f5', '#66bb6a', '#ffee58', '#ef5350', '#8d6e63']
      }
    }, {
      col: 0,
      row: 1,
      sizeY: 1,
      sizeX: 6,
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
      row: 2,
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
      row: 3,
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
}

export default ApplicationAnalyticsController;
