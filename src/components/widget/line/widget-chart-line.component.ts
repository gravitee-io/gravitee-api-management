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
import AnalyticsService from '../../../services/analytics.service';

const WidgetChartLineComponent: ng.IComponentOptions = {
  template: require('./widget-chart-line.html'),
  bindings: {
    data: '<',
  },
  require: {
    parent: '^gvWidget',
  },
  controller: function ($scope, $rootScope, ChartService, AnalyticsService: AnalyticsService) {
    'ngInject';

    this.AnalyticsService = AnalyticsService;
    this.$scope = $scope;

    this.$onInit = () => {
      this.widget = this.parent.widget;
    };

    this.$onChanges = (changes) => {
      if (changes.data) {
        let data = changes.data.currentValue;
        let values = [],
          i;

        // Prepare chart
        this.result = {
          title: { text: this.parent.widget.chart.title },
          xAxis: {
            type: 'datetime',
            dateTimeLabelFormats: {
              // don't display the dummy year
              month: '%e. %b',
              year: '%b',
            },
          },
          chart: {
            events: {
              selection: (event) => {
                if (!event.resetSelection) {
                  $rootScope.$broadcast('timeframeZoom', {
                    from: Math.floor(event.xAxis[0].min),
                    to: Math.round(event.xAxis[0].max),
                  });
                }
              },
            },
          },
        };

        if (data.values && data.values.length > 0) {
          _.forEach(data.values, (value, idx) => {
            _.forEach(value.buckets, (bucket) => {
              if (bucket) {
                i++;
                let lineColor = ChartService.colorByBucket[i % ChartService.colorByBucket.length];
                let bgColor = ChartService.bgColorByBucket[i % ChartService.bgColorByBucket.length];
                let isFieldRequest = _.includes(this.parent.widget.chart.request.aggs.split('%3B')[idx], 'field:');
                if (
                  bucket.name === '1' ||
                  bucket.name.match('^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$')
                ) {
                  isFieldRequest = false;
                }
                let label = this.parent.widget.chart.labels ? this.parent.widget.chart.labels[idx] : '';
                if (!label || (value.metadata && value.metadata[bucket.name])) {
                  label = value.metadata[bucket.name].name;
                }

                values.push({
                  name: isFieldRequest ? bucket.name : label,
                  data: bucket.data,
                  color: lineColor,
                  fillColor: bgColor,
                  labelPrefix: isFieldRequest ? label : '',
                  id: bucket.name,
                });
              }
            });
          });

          let timestamp = data.timestamp;

          let plotOptions = {
            series: {
              pointStart: timestamp.from,
              pointInterval: timestamp.interval,
              stacking: this.parent.widget.chart.stacked ? 'normal' : null,
            },
          } as any;

          if (this.parent.widget.chart.selectable) {
            plotOptions.series.events = {
              legendItemClick: (event) => {
                // If all series are visible, keep only the one selected
                let selected = event.target.chart.series[event.target.index];
                let visibles = _.filter(event.target.chart.series, { visible: true });

                if (visibles.length === this.result.series.length) {
                  // Do not disable selected item but disable others
                  event.preventDefault();

                  _(visibles)
                    .filter((serie: any) => {
                      return serie.name !== event.target.name;
                    })
                    .forEach((serie: any) => serie.hide());
                  this.updateQuery(selected, selected.visible);
                } else {
                  this.updateQuery(selected, !selected.visible);
                }
              },
              hide: (event) => {
                let hidden = _.filter(event.target.chart.series, { visible: false });
                if (hidden.length === this.result.series.length) {
                  // Do not disable selected item but disable others
                  event.preventDefault();

                  // All series are hidden: display all !
                  _(hidden).forEach((serie: any) => serie.show());
                }
              },
            };
          }
          this.result = _.assign(this.result, {
            series: values,
            plotOptions: plotOptions,
          });
          this.result.series.forEach((serie) => {
            let widget = this.widget || this.parent.widget;
            if (widget) {
              let queryFilters = this.AnalyticsService.getQueryFilters();
              if (queryFilters) {
                let queryFilter = queryFilters[widget.chart.request.aggs.split('field:')[1]];
                serie.visible = queryFilter && queryFilter.includes(serie.id);
              }
            }
          });
          let hidden = _.filter(this.result.series, { visible: false });
          hidden.forEach((h) => {
            this.updateQuery(h, false);
          });
        } else {
          this.result.series = [];
        }
      }
    };

    this.updateQuery = (item, add) => {
      let removeFn = () => {
        // Filter has been removed, so let's hide the serie
        if (this.visible) {
          this.hide();
        }
      };

      if (this.widget && item.userOptions) {
        this.$scope.$emit('filterItemChange', {
          widget: this.widget.$uid,
          field: this.widget.chart.request.aggs.split(':')[1],
          // fieldLabel: this.widget.chart.request.fieldLabel,
          key: item.userOptions.id,
          name: item.name,
          mode: add ? 'add' : 'remove',
          events: {
            remove: removeFn.bind(item),
          },
        });
      }
    };
  },
};

export default WidgetChartLineComponent;
