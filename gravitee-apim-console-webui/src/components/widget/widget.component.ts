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
import AnalyticsService from '../../services/analytics.service';

const WidgetComponent: ng.IComponentOptions = {
  template: require('./widget.html'),
  bindings: {
    widget: '<',
    updateMode: '<',
    globalQuery: '<',
  },
  controller: function ($scope, $state, AnalyticsService: AnalyticsService) {
    'ngInject';
    this.$state = $state;
    this.AnalyticsService = AnalyticsService;

    $scope.$on('gridster-resized', function () {
      $scope.$broadcast('onWidgetResize');
    });

    $scope.$on('onTimeframeChange', (event, timeframe) => {
      if (this.widget.chart && this.widget.chart.request) {
        let query;

        if (this.widget.chart.request.query) {
          query = this.widget.chart.request.query;
        }

        // Associate the new timeframe to the chart request
        _.assignIn(this.widget.chart.request, {
          interval: timeframe.interval,
          from: timeframe.from,
          to: timeframe.to,
          query: query,
        });
        this.reload();
      }
    });

    let unregisterFn = $scope.$on('onQueryFilterChange', (event, query) => {
      if (this.widget.chart && this.widget.chart.request) {
        this.reload();
      }
    });

    $scope.$on('$destroy', unregisterFn);

    this.reload = () => {
      // Call the analytics service
      this.fetchData = true;

      // Prepare arguments
      let chartRequest = _.cloneDeep(this.widget.chart.request);
      let field = this.widget.chart.request.field;
      if (this.widget.chart.request.aggs && this.widget.chart.request.aggs.includes('field:')) {
        field = this.widget.chart.request.aggs.replace('field:', '');
      }
      let queryFilters = this.AnalyticsService.getQueryFilters();
      if (queryFilters) {
        let filters;
        if (Object.keys(queryFilters).find((q) => q === field)) {
          filters = Object.keys(queryFilters).filter((q) => chartRequest.ranges || q !== field);
        } else {
          filters = Object.keys(queryFilters);
        }

        chartRequest.query = [
          // Specific initial query or empty string
          chartRequest.query,
          filters.map((f) => `(${f}:${queryFilters[f].map((qp) => this.AnalyticsService.buildQueryParam(qp, f)).join(' OR ')})`),
        ]
          .reduce((acc, val) => acc.concat(val), [])
          .filter((part) => part)
          .join(' AND ');
      }

      if (this.globalQuery) {
        if (chartRequest.query) {
          if (!_.includes(chartRequest.query, this.globalQuery)) {
            chartRequest.query += ' AND ' + this.globalQuery;
          }
        } else {
          chartRequest.query = this.globalQuery;
        }
      }

      let args = [this.widget.root, chartRequest];

      if (!this.widget.root) {
        args.splice(0, 1);
      }

      this.widget.chart.service.function.apply(this.widget.chart.service.caller, args).then((response) => {
        this.results = response.data;
        if (this.widget.chart.percent) {
          delete chartRequest.query;
          chartRequest.type = 'count';
          this.widget.chart.service.function
            .apply(this.widget.chart.service.caller, args)
            .then((responseTotal) => {
              _.forEach(this.results.values, (value, key) => {
                this.results.values[key] = value + '/' + _.round((value / responseTotal.data.hits) * 100, 2);
              });
            })
            .finally(() => (this.fetchData = false));
        } else {
          this.fetchData = false;
        }
      });
    };

    this.delete = () => {
      $scope.$emit('onWidgetDelete', this.widget);
    };

    this.getIconFromType = () => {
      switch (this.widget.chart.type) {
        case 'line':
          return 'multiline_chart';
        case 'map':
          return 'map';
        case 'pie':
          return 'pie_chart';
        case 'stats':
          return 'bubble_chart';
        case 'table':
          return 'view_list';
        default:
          return 'insert_chart';
      }
    };
  },
};

export default WidgetComponent;
