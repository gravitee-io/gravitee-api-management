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
  controller: function($scope, $rootScope, ChartService) {
    'ngInject';

    this.$scope = $scope;
    let that = this;

    this.$onInit = function() {
      this.widget = this.parent.widget;
    };

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
                  labelPrefix: that.parent.widget.chart.labelPrefix,
                  id: bucket.name
                });
              }
            });
          });

          let timestamp = data.timestamp;

          let plotOptions = {
            series: {
              pointStart: timestamp.from,
              pointInterval: timestamp.interval
            }
          } as any;

          if (this.parent.widget.chart.selectable) {
            plotOptions.series.events = {
              legendItemClick: function (event) {
                // If all series are visible, keep only the one selected
                let selected = event.target.chart.series[event.target.index];
                let visibles = _.filter(event.target.chart.series, { 'visible': true });

                if (visibles.length === that.result.series.length) {
                  // Do not disable selected item but disable others
                  event.preventDefault();

                  _(visibles)
                    .filter((serie: any) => { return serie.name !== event.target.name; })
                    .forEach((serie: any) => serie.hide());
                  that.updateQuery(selected, selected.visible);
                } else {
                  if (selected.visible) {
                    event.preventDefault();
                  }
                  that.updateQuery(selected, !selected.visible);
                }
              },
              hide: function(event) {
                let hidden = _.filter(event.target.chart.series, { 'visible': false });
                if (hidden.length === that.result.series.length) {
                  // Do not disable selected item but disable others
                  event.preventDefault();

                  // All series are hidden: display all !
                  _(hidden)
                    .forEach((serie: any) => serie.show());
                }
              }
            };
          }

          that.result = _.assign(that.result, {
            series: values,
            plotOptions: plotOptions
          });
        } else {
          that.result.series = [];
        }
      }
    };

    this.updateQuery = function(item, add) {
      let removeFn = function() {
          // Filter has been removed, so let's hide the serie
        if (this.visible) {
          this.hide();
        }
      };

      //console.log('filter :' + item.name + '(' + add + ')');
      that.$scope.$emit('filterItemChange', {
        widget: that.widget.$uid,
        field: that.widget.chart.request.field,
        fieldLabel: that.widget.chart.request.fieldLabel,
        key: item.userOptions.id,
        name: item.name,
        mode: (add) ? 'add' : 'remove',
        events: {
          remove: removeFn.bind(item)
        }
      });
    };
  }
};

export default WidgetChartLineComponent;
