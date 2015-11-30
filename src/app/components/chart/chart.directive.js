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
          Highcharts.theme = {
            colors: ["#2b908f", "#90ee7e", "#f45b5b", "#7798BF", "#aaeeee", "#ff0066", "#eeaaee",
              "#55BF3B", "#DF5353", "#7798BF", "#aaeeee"],
            chart: {
              backgroundColor: {
                linearGradient: { x1: 0, y1: 0, x2: 1, y2: 1 },
                stops: [
                  [0, '#2a2a2b'],
                  [1, '#3e3e40']
                ]
              },
              style: {
                fontFamily: "'Unica One', sans-serif"
              },
              plotBorderColor: '#606063'
            },
            title: {
              style: {
                color: '#E0E0E3',
                textTransform: 'uppercase',
                fontSize: '20px'
              }
            },
            subtitle: {
              style: {
                color: '#E0E0E3',
                textTransform: 'uppercase'
              }
            },
            xAxis: {
              gridLineColor: '#707073',
              labels: {
                style: {
                  color: '#E0E0E3'
                }
              },
              lineColor: '#707073',
              minorGridLineColor: '#505053',
              tickColor: '#707073',
              title: {
                style: {
                  color: '#A0A0A3'

                }
              }
            },
            yAxis: {
              gridLineColor: '#707073',
              labels: {
                style: {
                  color: '#E0E0E3'
                }
              },
              lineColor: '#707073',
              minorGridLineColor: '#505053',
              tickColor: '#707073',
              tickWidth: 1,
              title: {
                style: {
                  color: '#A0A0A3'
                }
              }
            },
            tooltip: {
              backgroundColor: 'rgba(0, 0, 0, 0.85)',
              style: {
                color: '#F0F0F0'
              }
            },
            plotOptions: {
              series: {
                dataLabels: {
                  color: '#B0B0B3'
                },
                marker: {
                  lineColor: '#333'
                }
              },
              boxplot: {
                fillColor: '#505053'
              },
              candlestick: {
                lineColor: 'white'
              },
              errorbar: {
                color: 'white'
              }
            },
            legend: {
              itemStyle: {
                color: '#E0E0E3'
              },
              itemHoverStyle: {
                color: '#FFF'
              },
              itemHiddenStyle: {
                color: '#606063'
              }
            },
            credits: {
              style: {
                color: '#666'
              }
            },
            labels: {
              style: {
                color: '#707073'
              }
            },

            drilldown: {
              activeAxisLabelStyle: {
                color: '#F0F0F3'
              },
              activeDataLabelStyle: {
                color: '#F0F0F3'
              }
            },

            navigation: {
              buttonOptions: {
                symbolStroke: '#DDDDDD',
                theme: {
                  fill: '#505053'
                }
              }
            },

            // scroll charts
            rangeSelector: {
              buttonTheme: {
                fill: '#505053',
                stroke: '#000000',
                style: {
                  color: '#CCC'
                },
                states: {
                  hover: {
                    fill: '#707073',
                    stroke: '#000000',
                    style: {
                      color: 'white'
                    }
                  },
                  select: {
                    fill: '#000003',
                    stroke: '#000000',
                    style: {
                      color: 'white'
                    }
                  }
                }
              },
              inputBoxBorderColor: '#505053',
              inputStyle: {
                backgroundColor: '#333',
                color: 'silver'
              },
              labelStyle: {
                color: 'silver'
              }
            },

            navigator: {
              handles: {
                backgroundColor: '#666',
                borderColor: '#AAA'
              },
              outlineColor: '#CCC',
              maskFill: 'rgba(255,255,255,0.1)',
              series: {
                color: '#7798BF',
                lineColor: '#A6C7ED'
              },
              xAxis: {
                gridLineColor: '#505053'
              }
            },

            scrollbar: {
              barBackgroundColor: '#808083',
              barBorderColor: '#808083',
              buttonArrowColor: '#CCC',
              buttonBackgroundColor: '#606063',
              buttonBorderColor: '#606063',
              rifleColor: '#FFF',
              trackBackgroundColor: '#404043',
              trackBorderColor: '#404043'
            },

            // special colors for some of the
            legendBackgroundColor: 'rgba(0, 0, 0, 0.5)',
            background2: '#505053',
            dataLabelsColor: '#B0B0B3',
            textColor: '#C0C0C0',
            contrastTextColor: '#F0F0F3',
            maskColor: 'rgba(255,255,255,0.3)'
          };

          Highcharts.setOptions(Highcharts.theme);

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
