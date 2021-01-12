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
import AnalyticsService from '../../../services/analytics.service';
// tslint:disable-next-line:no-var-requires
require('@gravitee/ui-components/wc/gv-chart-line');

const WidgetChartLineComponent: ng.IComponentOptions = {
  template: require('./widget-chart-line.html'),
  bindings: {
    data: '<'
  },
  require: {
    parent: '^gvWidget'
  },
  controller: function($scope, $element, $rootScope, ChartService, AnalyticsService: AnalyticsService) {
    'ngInject';

    this.AnalyticsService = AnalyticsService;
    this.$scope = $scope;

    this.$onInit = () => {
      this.widget = this.parent.widget;
    };

    this.$onChanges = (changes) => {
      if (changes.data) {
        let data = changes.data.currentValue;
        this.series = {values: []};
        this.gvChartLine = $element.children()[0];

        if (data.values && data.values.length > 0) {
          this.prepareData(data);

          // Send data to gv-chart-line
          this.gvChartLine.setAttribute('series', JSON.stringify(this.series));
          this.gvChartLine.setAttribute('options', JSON.stringify(this.options));

          // Events from gv-chart-line and dashboard resizing
          this.gvChartLine.addEventListener('gv-chart-line:zoom', this.onZoom.bind(this));
          this.gvChartLine.addEventListener('gv-chart-line:select', this.onSelect.bind(this));
          $scope.$on('onWidgetResize', this.onResize.bind(this));

        } else {
          this.gvChartLine.setAttribute('series', JSON.stringify([]));
        }
      }
    };

    this.prepareData = (data) => {
      let i;
      data.values.forEach( (value, idx) => {
        value.buckets.forEach((bucket) => {
          if (bucket) {
            i++;
            let lineColor = ChartService.colorByBucket[i % ChartService.colorByBucket.length];
            let bgColor = ChartService.bgColorByBucket[i % ChartService.bgColorByBucket.length];
            let isFieldRequest = this.parent.widget.chart.request.aggs.split('%3B')[idx].includes('field:');
            let query = this.parent.widget.chart.request.query;
            if (bucket.name === '1' || bucket.name.match('^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$')) {
              isFieldRequest = false;
            }
            let label = this.parent.widget.chart.labels ? this.parent.widget.chart.labels[idx] : '';
            if (!label || (value.metadata && value.metadata[bucket.name])) {
              label = value.metadata[bucket.name].name;
            }

            this.series.values.push({
              name: isFieldRequest ? bucket.name : label,
              data: bucket.data, color: lineColor, fillColor: bgColor,
              labelPrefix: isFieldRequest ? label : '',
              id: bucket.name,
              visible: this.parent.widget.chart.selectable && query ? query.includes(bucket.name) : true
            });
          }
        });
      });

      let timestamp = data.timestamp;

      this.options = {
        labelPrefix: 'HTTP Status',
        pointStart: timestamp.from,
        pointInterval: timestamp.interval,
        stacking: this.parent.widget.chart.stacked ? 'normal' : null
      };
    };

    this.onZoom = (event) => {
      $rootScope.$broadcast('timeframeZoom', {
        from: Math.floor(event.detail.from),
        to: Math.round(event.detail.to)
      });
    };

    this.onSelect = (event) => {
      let selected = event.detail.chart.series[event.detail.index];

      if (this.parent.widget.chart.selectable) {
        let query = this.parent.widget.chart.request.query;
        if (query && query.includes(selected.name)) {
          this.updateQuery(selected, false);
        } else {
          this.updateQuery(selected, true);
        }
      } else {

        this.series.values.forEach(serie => {
          if (serie.name === selected.name) {
            serie.visible = !selected.visible;
          }
        });

        this.gvChartLine.setAttribute('series', JSON.stringify(this.series));
      }
    };

    this.onResize = () => {
      this.options = {
        ...this.options,
        height: this.gvChartLine.offsetHeight,
        width: this.gvChartLine.offsetWidth,
      };

      this.gvChartLine.setAttribute('options', JSON.stringify(this.options));
    };

    this.updateQuery = (item, add) => {

      if (this.widget && item.userOptions) {
        this.$scope.$emit('filterItemChange', {
          widget: this.widget.$uid,
          field: this.widget.chart.request.aggs.split(':')[1],
          key: item.userOptions.id,
          name: item.name,
          mode: (add) ? 'add' : 'remove',
        });
      }
    };
  }
};

export default WidgetChartLineComponent;
