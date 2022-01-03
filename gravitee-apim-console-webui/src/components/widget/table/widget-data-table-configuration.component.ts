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
import DashboardService from '../../../services/dashboard.service';

const WidgetDataTableConfigurationComponent: ng.IComponentOptions = {
  template: require('./widget-data-table-configuration.html'),
  bindings: {
    chart: '<',
  },
  controller: function (DashboardService: DashboardService) {
    'ngInject';
    this.fields = DashboardService.getIndexedFields();
    this.projections = _.concat(
      {
        label: 'Hits',
        value: '_count',
        type: 'count',
      },
      DashboardService.getAverageableFields(),
    );
    this.projectionOrders = [
      { label: 'Desc', value: '-' },
      { label: 'Asc', value: '' },
    ];

    this.$onInit = () => {
      if (!this.chart.request) {
        _.merge(this.chart, {
          request: {
            type: 'group_by',
            field: this.fields[0].value,
          },
          columns: [],
          paging: 5,
        });
      }
      this.field = this.chart.request.field;
      if (this.chart.request.order) {
        let splittedOrder = this.chart.request.order.split(':');
        let aggregate = splittedOrder[0];
        let projection = splittedOrder[1];
        if ('-' === aggregate.charAt(0)) {
          this.order = '-';
          this.aggregate = aggregate.substring(1);
        } else {
          this.order = '';
          this.aggregate = aggregate;
        }
        this.projection = projection ? projection : '_count';
      } else {
        this.order = '-';
        this.aggregate = 'avg';
        this.projection = '_count';
      }
      this.onFieldChanged();
      this.onProjectionChanged();
    };

    this.onFieldChanged = () => {
      this.chart.request.field = this.field;
      this.chart.columns[0] = _.find(this.fields, (f) => f.value === this.field).label;
    };

    this.onProjectionChanged = () => {
      this.chart.columns[1] = _.find(this.projections, (p) => p.value === this.projection).label;
      if (this.projection) {
        this.chart.request.order = this.order + this.aggregate + ':' + this.projection;
        if (this.projection !== '_count') {
          this.chart.percent = false;
        }
      } else {
        delete this.chart.request.order;
      }
    };
  },
};

export default WidgetDataTableConfigurationComponent;
