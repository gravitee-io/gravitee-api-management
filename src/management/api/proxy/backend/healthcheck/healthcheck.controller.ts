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
import ApiService, { LogsQuery } from '../../../../../services/api.service';
import * as moment from 'moment';
import * as _ from 'lodash';
import { StateService } from '@uirouter/core';
import UserService from '../../../../../services/user.service';

class ApiHealthCheckController {
  private api: any;
  private gateway: any;
  private endpoint: any;
  private logs: {total: string; logs: any[], metadata: any};
  private transitionLogs: {total: string; logs: any[], metadata: any};
  private query: LogsQuery;
  private chartData: any;

  constructor (
    private ApiService: ApiService,
    private $scope,
    private $state: StateService,
    private ChartService,
    private $q,
    private UserService: UserService
  ) {
    'ngInject';
    this.api = this.$scope.$parent.apiCtrl.api;
    this.gateway = {availabilities:{},responsetimes:{}};
    this.endpoint = {availabilities:{},responsetimes:{}};

    this.onPaginate = this.onPaginate.bind(this);

    this.query = new LogsQuery();
    this.query.size = 10;
    this.query.page = 1;
    this.query.query = this.$state.params['q'];

    this.updateChart();
  }



  updateChart() {
    this.ApiService.apiHealth(this.api.id, 'availability')
      .then(response => {this.endpoint.availabilities.data = response.data;});

    this.ApiService.apiHealth(this.api.id, 'response_time')
      .then(response => {this.endpoint.responsetimes.data = response.data;});

    if (this.displayGatewayHC()) {
      this.ApiService.apiHealth(this.api.id, 'availability', 'gateway')
        .then(response => {this.gateway.availabilities.data = response.data;});

      this.ApiService.apiHealth(this.api.id, 'response_time', 'gateway')
        .then(response => {this.gateway.responsetimes.data = response.data;});
    }

    this.refresh();
  }

  onPaginate(page) {
    this.query.page = page;
    this.refresh();
  }

  refresh(averageFrom?, averageTo?) {
    this.ApiService.apiHealthLogs(this.api.id, this.query).then((logs) => {
      this.logs = logs.data;
    });
    this.ApiService.apiHealthLogs(this.api.id, _.assign({transition: true}, this.query)).then((logs) => {
      this.transitionLogs = logs.data;
    });

    let from = averageFrom || moment().subtract(1, 'months');
    let to = averageTo || moment();
    let interval = Math.floor((to - from)/30);
    let promises = [
      this.ApiService.apiHealthAverage(this.api.id, {from: from, to: to,
        interval: interval, type: 'RESPONSE_TIME'}),
      this.ApiService.apiHealthAverage(this.api.id, {from: from, to: to,
        interval: interval, type: 'AVAILABILITY'})
    ];

    this.$q.all(promises).then(responses => {
      let i = 0, series = [];
      _.forEach(responses, response => {
        let values = response.data.values;
        if (values && values.length > 0) {
          _.forEach(values, value => {
            _.forEach(value.buckets, bucket => {
              if (bucket) {
                let responseTimeLine = i == 0;
                series.push({
                  name: 'Average of ' + (responseTimeLine?'response time':'availability'), data: bucket.data, color: responseTimeLine?'#337AB7':'#5CB85C',
                  type: responseTimeLine?'area':'column',
                  labelSuffix: responseTimeLine?'ms':'%',
                  decimalFormat: !responseTimeLine,
                  yAxis: i,
                  zones: responseTimeLine?[]:[{
                    value: 80,
                    color: '#D9534F'
                  }, {
                    value: 95,
                    color: '#F0AD4E'
                  }, {
                    color: '#5CB85C'
                  }]
                });
              }
            });
          });
        }
        i++;
      });
      let timestamp = responses[0] && responses[0].data && responses[0].data.timestamp;
      this.chartData = {
        plotOptions: {
          series: {
            pointStart: timestamp && timestamp.from,
            pointInterval: timestamp && timestamp.interval
          }
        },
        series: series,
        xAxis: {
          type: 'datetime',
          dateTimeLabelFormats: {
            month: '%e. %b',
            year: '%b'
          }
        },
        yAxis: [{
          labels: {
            format: '{value}ms'
          },
          title: {
            text: 'Response time'
          }
        },{
          title: {
            text: 'Availability'
          },
          labels: {
            format: '{value}%'
          },
          max: 100,
          opposite: true
        }],
        chart: {
          events: {
            selection: (event) => {
              if (!event.resetSelection) {
                this.refresh(Math.floor(event.xAxis[0].min), Math.floor(event.xAxis[0].max));
              }
            }
          }
        }
      }
    });
  }

  getEndpointStatus(state) {
    switch(state) {
      case 3: return 'UP';
      case 2: return 'TRANSITIONALLY_UP';
      case 1: return 'TRANSITIONALLY_DOWN';
      case 0: return 'DOWN';
      default: return '-';
    }
  }

  viewLog(log) {
    this.$state.go('management.apis.detail.proxy.healthcheck.log', log);
  }

  displayGatewayHC() {
    return this.UserService.currentUser.isAdmin();
  }
}

export default ApiHealthCheckController;
