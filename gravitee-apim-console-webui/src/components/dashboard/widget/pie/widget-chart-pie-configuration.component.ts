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

import { forEach, merge, last, remove } from 'lodash';

import DashboardService from '../../../../services/dashboard.service';

const WidgetChartPieConfigurationComponent: ng.IComponentOptions = {
  template: require('html-loader!./widget-chart-pie-configuration.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
  bindings: {
    chart: '<',
  },
  controller: [
    'DashboardService',
    function (DashboardService: DashboardService) {
      this.fields = DashboardService.getNumericFields();

      this.$onInit = () => {
        this.data = [];
        if (this.chart.request) {
          if (this.chart.request.ranges) {
            const ranges = this.chart.request.ranges.split('%3B');
            let i = 0;
            forEach(ranges, range => {
              if (range) {
                this.data.push({
                  min: parseInt(range.split(':')[0], 10),
                  max: parseInt(range.split(':')[1], 10),
                  label: this.chart.labels[i],
                  color: this.chart.colors[i++],
                });
              }
            });
          }
        } else {
          merge(this.chart, {
            request: {
              type: 'group_by',
              field: this.fields[0].value,
              ranges: '',
            },
            labels: [],
            colors: [],
          });
        }

        if (this.chart.request.field.startsWith('custom')) {
          this.field = this.chart.request.field.substr('custom.'.length);
          this.isCustomField = true;
        } else {
          this.field = this.chart.request.field;
          this.isCustomField = false;
        }
      };

      this.onFieldChanged = () => {
        this.chart.request.field = 'custom.' + this.field;
      };

      this.onDataChanged = () => {
        this.chart.request.ranges = '';
        this.chart.labels = [];
        this.chart.colors = [];
        const _last = last(this.data);
        forEach(this.data, data => {
          this.chart.request.ranges += data.min + ':' + data.max + (_last === data ? '' : '%3B');
          this.chart.labels.push(data.label);
          this.chart.colors.push(data.color);
        });
      };

      this.addData = () => {
        this.data.push({});
        this.onDataChanged();
      };

      this.removeData = data => {
        remove(this.data, data);
        this.onDataChanged();
      };
    },
  ],
};

export default WidgetChartPieConfigurationComponent;
