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
class WidgetChartDirective {
  constructor() {
    this.restrict = 'E';
    this.scope = {
      chart: '=chart'
    };
    this.templateUrl = 'app/components/widget/widget-chart.html';
  }

  controller($scope) {
    'ngInject';

    $scope.refresh = function() {
      // Call the analytics service
      $scope.fetchData = true;

      let chart = $scope.chart;

      // Prepare arguments
      let args = [$scope.$parent.widget.root, chart.request];

      if (! $scope.$parent.widget.root) {
        args.splice(0,1);
      }

      chart.service.function
        .apply(chart.service.caller, args)
        .then(response => {
          $scope.fetchData = false;
          $scope.results = response.data;
        });
    };
  }

  link(scope) {
    // Refresh widget on each timeframe change
    scope.$on('timeframeChange', function (event, timeframe) {
      let chart = scope.chart;

      // Associate the new timeframe to the chart request
      _.assignIn(chart.request, {
        interval: timeframe.interval,
        from: timeframe.from,
        to: timeframe.to
      });

      scope.refresh();
    });

    scope.$on('queryChange', function (event, query) {
      let chart = scope.chart;

      // Associate the new query to the chart request
      _.assignIn(chart.request, {
        query: query.query
      });

      scope.refresh();
    });
  }
}

export default WidgetChartDirective;
