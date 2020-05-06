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
const WidgetChartPieConfigurationComponent: ng.IComponentOptions = {
  template: require('./widget-chart-pie-configuration.html'),
  bindings: {
    chart: '<'
  },
  controller: function (DashboardService: DashboardService) {
    'ngInject';
    this.fields = DashboardService.getNumericFields();

    this.$onInit = () => {
      this.data = [];
      if (this.chart.request) {
        if (this.chart.request.ranges) {
          let ranges = this.chart.request.ranges.split('%3B');
          let i = 0;
          _.forEach(ranges, (range) => {
            if (range) {
              this.data.push({
                min: parseInt(range.split(':')[0], 10),
                max: parseInt(range.split(':')[1], 10),
                label: this.chart.labels[i],
                color: this.chart.colors[i++]
              });
            }
          });
        }
      } else {
        _.merge(this.chart, {
          request: {
            type: 'group_by',
            field: this.fields[0].value,
            ranges: ''
          },
          labels: [],
          colors: []
        });
      }
    };

    this.onDataChanged = () => {
      this.chart.request.ranges = '';
      this.chart.labels = [];
      this.chart.colors = [];
      let last = _.last(this.data);
      _.forEach(this.data, (data) => {
        this.chart.request.ranges += data.min + ':' + data.max + (last === data ? '' : '%3B');
        this.chart.labels.push(data.label);
        this.chart.colors.push(data.color);
      });
    };

    this.addData = () => {
      this.data.push({});
      this.onDataChanged();
    };

    this.removeData = (data) => {
      _.remove(this.data, data);
      this.onDataChanged();
    };
  }
};

export default WidgetChartPieConfigurationComponent;
