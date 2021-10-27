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
import angular = require('angular');
import * as Highcharts from 'highcharts';
import * as Highmaps from 'highcharts/highmaps';
import * as _ from 'lodash';
import 'highcharts/modules/map';

class ChartDirective {
  constructor() {
    'ngInject';

    return {
      restrict: 'E',
      template: '<div></div>',
      scope: {
        options: '=',
        type: '@',
        zoom: '=',
        height: '=',
        width: '=',
      },
      controller: ChartController,
      link: function (scope, element, attributes, controller) {
        Highcharts.setOptions({
          time: {
            useUTC: false,
          },
        });

        const chartElement = element[0];

        if (scope.type && scope.type.startsWith('area')) {
          initSynchronizedCharts();
        }

        let lastOptions;
        scope.$watch('options', (newOptions) => {
          setTimeout(() => {
            displayChart(newOptions, chartElement);
            lastOptions = newOptions;
          });
        });

        function onWindowResized() {
          setTimeout(() => {
            onResize();
          }, 100);
        }

        angular.element(controller.$window).bind('resize', () => {
          onWindowResized();
        });

        scope.$on('onWidgetResize', () => {
          onWindowResized();
        });

        function onResize() {
          displayChart(lastOptions, chartElement);
        }

        function initSynchronizedCharts() {
          element.bind('mousemove touchmove touchstart', (e) => {
            let chart, i, event, points;

            for (i = 0; i < Highcharts.charts.length; i++) {
              chart = Highcharts.charts[i];
              if (chart && chart.pointer) {
                if (e.originalEvent) {
                  event = chart.pointer.normalize(e.originalEvent);
                } else {
                  event = chart.pointer.normalize(e);
                }
                points = _.map(chart.series, (serie: any) => {
                  return serie.searchPoint(event, true);
                });
                points = _.filter(points, (point) => {
                  return point;
                });

                (e as any).points = points;
                if (points.length && points[0] && points[0].series.area) {
                  points[0].highlight(e);
                }
              }
            }
          });
          (Highcharts as any).Pointer.prototype.reset = function () {
            let chart;
            for (let i = 0; i < Highcharts.charts.length; i++) {
              chart = Highcharts.charts[i];
              if (chart) {
                if (chart.tooltip) {
                  chart.tooltip.hide(this);
                }
                if (chart.xAxis[0]) {
                  chart.xAxis[0].hideCrosshair();
                }
              }
            }
          };
          (Highcharts as any).Point.prototype.highlight = function (event) {
            if (event.points.length) {
              this.onMouseOver();
              this.series.chart.tooltip.refresh(event.points);
              this.series.chart.xAxis[0].drawCrosshair(event, this);
            }
          };
        }

        function displayChart(newOptions, chartElement) {
          if (newOptions) {
            newOptions = _.merge(newOptions, {
              lang: {
                noData: '<code>No data to display</code>',
              },
              noData: {
                useHTML: true,
              },
            });

            if (newOptions.title) {
              newOptions.title.style = {
                fontWeight: 'bold',
                fontSize: '12px',
                fontFamily: '"Helvetica Neue",Helvetica,Arial,sans-serif',
              };
              newOptions.title.align = 'left';
            } else {
              newOptions.title = { text: '' };
            }
            newOptions.yAxis = _.merge(newOptions.yAxis, { title: { text: '' } });

            const containerElement = element.parent().parent()[0];
            const parentElement = element.parent()[0];

            newOptions.chart = _.merge(newOptions.chart, {
              type: scope.type,
              height: scope.height || parentElement.height || containerElement.height,
              width: scope.width || parentElement.clientWidth || containerElement.clientWidth,
            });
            if (scope.zoom) {
              newOptions.chart.zoomType = 'x';
            }

            newOptions.credits = {
              enabled: false,
            };

            newOptions.series = _.sortBy(newOptions.series, 'name');

            _.forEach(newOptions.series, (serie) => {
              serie.data = _.sortBy(serie.data, 'name');
            });

            if (scope.type && scope.type.startsWith('area')) {
              newOptions.tooltip = {
                formatter: function () {
                  // TODO: check this
                  // let s = '<b>' + Highcharts.dateFormat('%A, %b %d, %H:%M', new Date(this.x)) + '</b>';
                  const nbCol = Math.trunc(this.points.filter((p) => p.y).length / 10);
                  const dateFormat = newOptions.dateFormat || '%A, %b %d, %H:%M';
                  let s = '<div><b>' + Highcharts.dateFormat(dateFormat, this.x) + '</b></div>';
                  s += '<div class="' + (nbCol >= 2 ? 'gv-tooltip gv-tooltip-' + (nbCol > 5 ? 5 : nbCol) : '') + '">';
                  if (
                    _.filter(this.points, (point: any) => {
                      return point.y !== 0;
                    }).length
                  ) {
                    let i = 0;
                    _.forEach(this.points, (point) => {
                      if (point.y) {
                        const name =
                          ' ' +
                          (point.series.options.labelPrefix
                            ? point.series.options.labelPrefix + ' ' + point.series.name
                            : point.series.name);
                        if (nbCol < 2 && i++ > 0) {
                          s += '<br />';
                        }
                        s +=
                          '<span style="margin: 1px 5px;"><span style="color:' +
                          point.color +
                          '">\u25CF</span>' +
                          name +
                          ': <b>' +
                          (point.series.options.decimalFormat ? Highcharts.numberFormat(point.y, 2) : point.y) +
                          (point.series.options.labelSuffix ? point.series.options.labelSuffix : '') +
                          '</b></span>';
                      }
                    });
                  }
                  s += '</div>';
                  return s;
                },
                shared: true,
                useHTML: true,
              };
              newOptions.plotOptions = _.merge(newOptions.plotOptions, {
                series: {
                  marker: {
                    enabled: false,
                  },
                  fillOpacity: 0.1,
                },
              });

              if (scope.type && scope.type.startsWith('area')) {
                newOptions.xAxis = _.merge(newOptions.xAxis, { crosshair: true });
              }
            } else if (scope.type && scope.type === 'solidgauge') {
              newOptions = _.merge(newOptions, {
                pane: {
                  background: {
                    innerRadius: '80%',
                    outerRadius: '100%',
                  },
                },

                tooltip: {
                  enabled: false,
                },

                yAxis: {
                  showFirstLabel: false,
                  showLastLabel: false,
                  min: 0,
                  max: 100,
                  stops: [
                    [0.1, '#55BF3B'], // green
                    [0.5, '#DDDF0D'], // yellow
                    [0.9, '#DF5353'], // red
                  ],
                  minorTickInterval: null,
                  tickAmount: 2,
                },

                plotOptions: {
                  solidgauge: {
                    innerRadius: '80%',
                    outerRadius: '100%',
                    dataLabels: {
                      y: 30,
                      borderWidth: 0,
                      useHTML: true,
                    },
                  },
                },
                series: [
                  {
                    dataLabels: {
                      format:
                        '<div style="text-align:center">' +
                        '<span style="font-size:25px;color:' +
                        (((Highcharts as any).theme && (Highcharts as any).theme.contrastTextColor) || 'black') +
                        '">{y}%</span><br/>' +
                        '<span style="font-size:12px;color:silver;">' +
                        newOptions.series[0].name +
                        '</span>' +
                        '</div>',
                    },
                  },
                ],
              });
            } else if (scope.type && scope.type === 'column') {
              if (scope.stacked) {
                newOptions.plotOptions = {
                  column: {
                    stacking: 'normal',
                  },
                };
              }
            } else if (scope.type && scope.type === 'sparkline') {
              const maxValue = _.max(newOptions.series[0].data);
              newOptions = _.merge(newOptions, {
                chart: {
                  backgroundColor: null,
                  borderWidth: 0,
                  type: 'area',
                  margin: [0, 0, 2, 0],
                  // small optimalization, saves 1-2 ms each sparkline
                  skipClone: true,
                },
                title: {
                  text: '',
                },
                credits: {
                  enabled: false,
                },
                xAxis: {
                  labels: {
                    enabled: false,
                  },
                  title: {
                    text: null,
                  },
                  startOnTick: false,
                  endOnTick: false,
                  tickPositions: [],
                },
                yAxis: {
                  max: maxValue === 0 ? 1 : maxValue,
                  endOnTick: false,
                  startOnTick: false,
                  labels: {
                    enabled: false,
                  },
                  title: {
                    text: null,
                  },
                  tickPositions: [0],
                },
                legend: {
                  enabled: false,
                },
                tooltip: {
                  hideDelay: 0,
                  outside: true,
                  shared: true,
                  headerFormat: '',
                  pointFormat: '<b>{point.y}</b> hits',
                },
                plotOptions: {
                  series: {
                    animation: false,
                    lineWidth: 2,
                    shadow: false,
                    states: {
                      hover: {
                        lineWidth: 2,
                      },
                    },
                    marker: {
                      enabled: false,
                      radius: 1,
                      states: {
                        hover: {
                          radius: 2,
                        },
                      },
                    },
                    fillOpacity: 0.25,
                  },
                  column: {
                    negativeColor: '#910000',
                    borderColor: 'silver',
                  },
                },
              });
            }

            if (scope.type === 'map') {
              Highmaps.mapChart(chartElement, _.cloneDeep(newOptions));
            } else {
              Highcharts.chart(chartElement, _.cloneDeep(newOptions));
            }
          }
        }
      },
    };
  }
}

class ChartController {
  constructor(private $window: ng.IWindowService) {
    'ngInject';
  }
}

export default ChartDirective;
