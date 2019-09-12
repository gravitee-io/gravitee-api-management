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

const WidgetComponent: ng.IComponentOptions = {
  template: require('./widget.html'),
  bindings: {
    widget: '<'
  },
  controller: function($scope, $state) {
    'ngInject';

    this.$state = $state;

    $scope.$on('gridster-resized', function () {
      $scope.$broadcast('onWidgetResize');
    });

    $scope.$on('onTimeframeChange', (event, timeframe) => {
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
        additionalQuery: this.widget.chart.request.additionalQuery
      });

      this.reload();
    });

    let unregisterFn = $scope.$on('onQueryFilterChange', (event, query) => {
      // Associate the new query filter to the chart request
      this.widget.chart.request.additionalQuery = query.query;

      // Reload only if not the same widget which applied the latest filter
      if (this.widget.$uid !== query.source) {
        this.reload();
      }
    });

    $scope.$on('$destroy', unregisterFn);

    this.reload = () => {
      // Call the analytics service
      this.fetchData = true;

      let chart = this.widget.chart;

      // Prepare arguments
      let chartRequest = _.cloneDeep(chart.request);
      if (chartRequest.additionalQuery) {
        if (chartRequest.query) {
          if (!_.includes(chartRequest.query, this.widget.chart.request.additionalQuery)) {
            chartRequest.query += ' AND ' + this.widget.chart.request.additionalQuery;
          }
        } else {
          chartRequest.query = this.widget.chart.request.additionalQuery;
        }
        delete chartRequest.additionalQuery;
      }
      let args = [this.widget.root, chartRequest];

      if (! this.widget.root) {
        args.splice(0,1);
      }

      chart.service.function
        .apply(chart.service.caller, args)
        .then(response => {

          if (this.widget.chart.percent) {
            delete args[0].query;
            chart.service.function
              .apply(chart.service.caller, args)
              .then(responseTotal => {
                this.fetchData = false;
                _.forEach(response.data.values, (value, key) => {
                  let total = _.find(responseTotal.data.values, (v, k) => {
                    return k === key;
                  });
                  response.data.values[key] = value + '/' + _.round((value / total) * 100, 2);
                });
                this.results = response.data;
              });
          } else {
            this.fetchData = false;
            this.results = response.data;
          }
        });
    };
  }
};

export default WidgetComponent;
