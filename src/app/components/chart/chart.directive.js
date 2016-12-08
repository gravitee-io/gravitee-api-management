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
    return {
      restrict: 'E',
      template: '<div></div>',
      scope: {
        options: '=',
        type: '@',
        stacked: '=',
        height: '@',
        width: '@'
      },
      link: function (scope, element) {
        setWindowSize();

        let lastOptions;
        scope.$watch('options', function (newOptions) {
          lastOptions = newOptions;
          displayChart(newOptions);
        }, true);

        $(window).resize(function () {
          setWindowSize();
          displayChart(lastOptions);
        });

        function setWindowSize() {
          let chartElement = angular.element(element[0]);
          chartElement.css('height', scope.height || chartElement.parent().height());
          chartElement.css('width', scope.width || chartElement.parent().width());
        }

        function displayChart(newOptions) {
          if (newOptions) {
            newOptions.title = {text: ''};
            newOptions.yAxis = _.merge(newOptions.yAxis, {title: {text: ''}});
            newOptions.chart = {type: scope.type};
            newOptions.credits = {
              enabled: false
            };
            newOptions.series = _.sortBy(newOptions.series, 'name');

            _.forEach(newOptions.series, function (serie) {
              serie.data = _.sortBy(serie.data, 'name');
            });

            if (scope.type && scope.type.startsWith('area')) {
              newOptions.tooltip = {
                formatter: function () {
                  let s = '<b>' + this.x + '</b>';
                  if (_.filter(this.points, function (point) {
                      return point.y !== 0;
                    }).length) {
                    _.forEach(this.points, function (point) {
                      if (point.y) {
                        let name = ' ' + (point.series.options.labelPrefix ? point.series.options.labelPrefix + ' ' + point.series.name : point.series.name);
                        s += '<br /><span style="color:' + point.color + '">\u25CF</span>' + name + ': <b>' + point.y + '</b>';
                      }
                    });
                  }
                  return s;
                },
                shared: true
              };
              newOptions.plotOptions = {
                series: {
                  marker: {
                    enabled: false
                  }
                }
              };
              if (scope.stacked) {
                newOptions.plotOptions.areaspline = {
                  stacking: 'normal'
                };
              }
            } else if (scope.type && scope.type === 'solidgauge') {
              newOptions = _.merge(newOptions, {
                pane: {
                  background: {
                    innerRadius: '80%',
                    outerRadius: '100%'
                  }
                },

                tooltip: {
                  enabled: false
                },

                yAxis: {
                  showFirstLabel: false,
                  showLastLabel: false,
                  min: 0,
                  max: 100,
                  stops: [
                    [0.1, '#55BF3B'], // green
                    [0.5, '#DDDF0D'], // yellow
                    [0.9, '#DF5353'] // red
                  ],
                  minorTickInterval: null,
                  tickAmount: 2
                },

                plotOptions: {
                  solidgauge: {
                    innerRadius: '80%',
                    outerRadius: '100%',
                    dataLabels: {
                      y: 30,
                      borderWidth: 0,
                      useHTML: true
                    }
                  }
                },
                series: [{
                  dataLabels: {
                    format: '<div style="text-align:center">' +
                    '<span style="font-size:25px;color:' +
                    ((Highcharts.theme && Highcharts.theme.contrastTextColor) || 'black') + '">{y}%</span><br/>' +
                    '<span style="font-size:12px;color:silver;">' + newOptions.series[0].name + '</span>' +
                    '</div>'
                  }
                }]
              });
            } else if (scope.type && scope.type === 'column') {
              if (scope.stacked) {
                newOptions.plotOptions = {
                  column: {
                    stacking: 'normal'
                  }
                };
              }
            }
            Highcharts.chart(element[0], newOptions);
          }
        }
      }
    };
  }
}

export default ChartDirective;
