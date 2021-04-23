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
import DashboardService from '../../../services/dashboard.service';
import * as _ from 'lodash';
const WidgetDataStatsConfigurationComponent: ng.IComponentOptions = {
  template: require('./widget-data-stats-configuration.html'),
  bindings: {
    chart: '<',
  },
  controller: function (DashboardService: DashboardService) {
    'ngInject';
    this.fields = DashboardService.getAverageableFields();
    this.stats = [
      {
        key: 'min',
        label: 'min',
        unit: 'ms',
        color: '#66bb6a',
      },
      {
        key: 'max',
        label: 'max',
        unit: 'ms',
        color: '#ef5350',
      },
      {
        key: 'avg',
        label: 'avg',
        unit: 'ms',
        color: '#42a5f5',
      },
      {
        key: 'rps',
        label: 'requests per second',
        color: '#ff8f2d',
        fallback: [
          {
            key: 'rpm',
            label: 'requests per minute',
          },
          {
            key: 'rph',
            label: 'requests per hour',
          },
        ],
      },
      {
        key: 'count',
        label: 'total',
        color: 'black',
      },
    ];
    this.statKeys = _.map(this.stats, 'key');

    this.$onInit = () => {
      if (!this.chart.data) {
        _.merge(this.chart, {
          request: {
            type: 'stats',
            field: this.fields[0].value,
          },
          data: this.stats,
        });
      }
      this.selectedStats = _.map(this.chart.data, 'key');
    };

    this.onStatsChanged = () => {
      this.chart.data = _.filter(this.stats, (stat) => _.includes(this.selectedStats, stat.key));
    };
  },
};

export default WidgetDataStatsConfigurationComponent;
