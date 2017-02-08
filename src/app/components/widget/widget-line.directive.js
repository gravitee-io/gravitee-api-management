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
class WidgetChartLineDirective {
  constructor() {
    this.restrict = 'E';
    this.scope = {
      data: "=data"
    };
    this.templateUrl = 'app/components/widget/widget-line.html';
  }

  controller($scope, $rootScope) {
    'ngInject';

    //from https://material.google.com/style/color.html#color-color-palette
    //shade 500 & 900
    //
    // deep purple, lime, deep orange, pink, purple,
    // light green, amber, Blue Grey, orange, teal,
    // indigo, purple, red, cyan, brown
    $scope.colorByBucket = [
      '#673ab7', '#cddc39', '#ff5722', '#e91e63', '#9c27b0',
      '#8bc34a', '#ffc107', '#607d8b', '#ff9800', '#009688',
      '#3f51b5', '#9c27b0', '#f44336', '#00bcd4', '#795548',

      '#311b92', '#827717', '#bf360c', '#880e4f', '#4a148c',
      '#33691e', '#ff6f00', '#263238', '#e65100', '#004d40',
      '#1a237e', '#4a148c', '#b71c1c', '#006064', '#3e2723'
    ];


    //from https://material.google.com/style/color.html#color-color-palette
    //shade 200 & 300
    $scope.bgColorByBucket = [
      '#b39ddb', '#e6ee9c', '#ffab91', '#f48fb1', '#ce93d8',
      '#c5e1a5', '#ffe082', '#b0bec5', '#ffcc80', '#80cbc4',
      '#9fa8da', '#ce93d8', '#ef9a9a', '#80deea', '#bcaaa4',

      '#9575cd', '#dce775', '#ff8a65', '#f06292', '#ba68c8',
      '#aed581', '#ffd54f', '#90a4ae', '#ffb74d', '#4db6ac',
      '#7986cb', '#ba68c8', '#e57373', '#4dd0e1', '#a1887f'
    ];

    $scope.$rootScope = $rootScope;
  }

  link(scope) {
    scope.result = {
      title: {text: scope.$parent.chart.title},
      xAxis: {
        type: 'datetime',
        dateTimeLabelFormats: { // don't display the dummy year
          month: '%e. %b',
          year: '%b'
        }
      },
      plotOptions: {
        areaspline: {
          stacking: scope.$parent.chart.stacked ? 'normal' : null
        }
      },
      chart: {
        events: {
          selection: function (event) {
            if (!event.resetSelection) {
              scope.$rootScope.$broadcast("timeframeZoom", {
                from: Math.floor(event.xAxis[0].min),
                to: Math.round(event.xAxis[0].max)
              });
            }
          }
        }
      }
    };

    scope.$watch('data', function(data) {
      if (data) {
        let values = [], i;

        if (data.values && data.values.length > 0) {
          _.forEach(data.values, function (value, idx) {
            _.forEach(value.buckets, function (bucket) {
              if (bucket) {
                i++;
                let lineColor = scope.colorByBucket[i % scope.colorByBucket.length];
                let bgColor = scope.bgColorByBucket[i % scope.bgColorByBucket.length];
                let label = scope.$parent.chart.labels ? scope.$parent.chart.labels[idx] : (bucket.name);
                if (value.metadata && value.metadata[bucket.name]) {
                  label = value.metadata[bucket.name].name;
                }

                values.push({
                  name: label || bucket.name, data: bucket.data, color: lineColor, fillColor: bgColor,
                  labelPrefix: scope.$parent.chart.labelPrefix
                });
              }
            });
          });

          let timestamp = data.timestamp;

          scope.result = _.assign(scope.result, {
            series: values,
            plotOptions: {
              series: {
                pointStart: timestamp.from,
                pointInterval: timestamp.interval
              }
            }
          });
        } else {
          scope.result.series = [];
        }
      }
    }, true);
  }
}

export default WidgetChartLineDirective;
