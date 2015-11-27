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
class ChartDirective {
  constructor() {
    'ngInject';

    let directive = {
      restrict: 'E',
      replace: true,
      template: '<div></div>',
      scope: {
        config: '='
      },
      link: function (scope, element) {
        var chart;
        var process = function () {
          Highcharts.setOptions({
            global: {
              useUTC: false
            }
          });
          
          var defaultOptions = {
            chart: { renderTo: element[0] },
          };

          var config = angular.extend(defaultOptions, scope.config);
          chart = new Highcharts.Chart(config);
        };
        process();
        scope.$watch('config.series', function () {
          process();
        });
        scope.$watch('config.loading', function (loading) {
          if (!chart) {
            return;
          }
          if (loading) {
            chart.showLoading();
          } else {
            chart.hideLoading();
          }
        });
      }
    };

    return directive;
  }
}

export default ChartDirective;
