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

const WidgetChartLineComponent: ng.IComponentOptions = {
  template: require('./widget-chart-line.html'),
  bindings: {
    data: '<'
  },
  require: {
    parent: '^gvWidget'
  },
  controller: function($scope: ng.IScope, $rootScope, ChartService) {
    'ngInject';

    this.$scope = $scope;

    let that = this;
    this.$onChanges = function(changes) {
      if (changes.data) {
        let data = changes.data.currentValue;
        let values = [], i;

        // Prepare chart
        this.result = {
          title: {text: this.parent.widget.chart.title},
          xAxis: {
            type: 'datetime',
            dateTimeLabelFormats: { // don't display the dummy year
              month: '%e. %b',
              year: '%b'
            }
          },
          plotOptions: {
            areaspline: {
              stacking: this.parent.widget.chart.stacked ? 'normal' : null
            }
          },
          chart: {
            events: {
              selection: function (event) {
                if (!event.resetSelection) {
                  $rootScope.$broadcast("timeframeZoom", {
                    from: Math.floor(event.xAxis[0].min),
                    to: Math.round(event.xAxis[0].max)
                  });
                }
              }
            }
          }
        };

        if (data.values && data.values.length > 0) {
          _.forEach(data.values, function (value, idx) {
            _.forEach(value.buckets, function (bucket) {
              if (bucket) {
                i++;
                let lineColor = ChartService.colorByBucket[i % ChartService.colorByBucket.length];
                let bgColor = ChartService.bgColorByBucket[i % ChartService.bgColorByBucket.length];
                let label = that.parent.widget.chart.labels ? that.parent.widget.chart.labels[idx] : (bucket.name);
                if (value.metadata && value.metadata[bucket.name]) {
                  label = value.metadata[bucket.name].name;
                }

                values.push({
                  name: label || bucket.name, data: bucket.data, color: lineColor, fillColor: bgColor,
                  labelPrefix: that.parent.widget.chart.labelPrefix
                });
              }
            });
          });

          let timestamp = data.timestamp;

          that.result = _.assign(that.result, {
            series: values,
            plotOptions: {
              series: {
                pointStart: timestamp.from,
                pointInterval: timestamp.interval
              }
            }
          });
        } else {
          that.result.series = [];
        }
      }
    };
  }
};

export default WidgetChartLineComponent;
