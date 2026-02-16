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
import AnalyticsService from '../../../../services/analytics.service';
import { EventService } from '../../../../services/event.service';
// eslint:disable-next-line:no-var-requires
require('@gravitee/ui-components/wc/gv-chart-line');

const WidgetChartLineComponent: ng.IComponentOptions = {
  template: require('html-loader!./widget-chart-line.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
  bindings: {
    data: '<',
    activatedRoute: '<',
  },
  require: {
    parent: '^gvWidget',
  },
  controller: [
    '$scope',
    '$element',
    '$rootScope',
    'AnalyticsService',
    'eventService',
    function ($scope, $element, $rootScope, AnalyticsService: AnalyticsService, eventService: EventService) {
      this.AnalyticsService = AnalyticsService;
      this.eventService = eventService;
      this.$scope = $scope;

      this.$onInit = () => {
        this.widget = this.parent.widget;
      };

      this.$onChanges = changes => {
        if (changes.data) {
          const data = changes.data.currentValue;
          this.series = { values: [] };
          this.gvChartLine = $element.children()[0];

          if ((data.values && data.values.length > 0) || (data.events && data.events.content && data.events.content.length > 0)) {
            this.prepareData(data);

            // Send data to gv-chart-line
            this.gvChartLine.setAttribute('series', JSON.stringify(this.series));
            this.gvChartLine.setAttribute('options', JSON.stringify(this.options));

            // Events from gv-chart-line and dashboard resizing
            this.gvChartLine.addEventListener('gv-chart-line:zoom', this.onZoom.bind(this));
            this.gvChartLine.addEventListener('gv-chart-line:select', this.onSelect.bind(this));
            $scope.$on('onWidgetResize', this.onResize.bind(this));
          } else {
            this.gvChartLine.setAttribute('options', JSON.stringify([{}]));
            this.gvChartLine.setAttribute('series', JSON.stringify([{}]));
          }
        }
      };

      this.prepareData = data => {
        const timestamp = data.timestamp;
        this.events = data.events && data.events.content;

        const orderedBucketNames = [];

        data.values.forEach((value, idx) => {
          let label = this.parent.widget.chart.labels ? this.parent.widget.chart.labels[idx] : '';

          if (value.buckets.length === 0) {
            const bucketCount = (data.timestamp.to - data.timestamp.from) / data.timestamp.interval;

            orderedBucketNames.push(label);

            this.series.values.push({
              name: label,
              data: Array(bucketCount).fill(0),
              labelPrefix: label,
              id: label,
              visible: true,
            });
          } else {
            let field = this.parent.widget.chart.request.field;
            if (this.parent.widget.chart.request.aggs && this.parent.widget.chart.request.aggs.includes('field:')) {
              field = this.parent.widget.chart.request.aggs.replace('field:', '');
            }
            const queryFilters = this.AnalyticsService.getQueryFilters(this.activatedRoute);
            value.buckets.forEach(bucket => {
              if (bucket) {
                let isFieldRequest = this.parent.widget.chart.request.aggs.split('%3B')[idx].includes('field:');
                if (
                  bucket.name === '1' ||
                  bucket.name.match('^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$')
                ) {
                  isFieldRequest = false;
                }
                if (!label || (value.metadata && value.metadata[bucket.name])) {
                  label = value.metadata[bucket.name].name;
                }

                const valueName = isFieldRequest ? bucket.name : label;
                orderedBucketNames.push(valueName);

                this.series.values.push({
                  name: valueName,
                  data: bucket.data,
                  labelPrefix: isFieldRequest ? label : '',
                  id: bucket.name,
                  visible: queryFilters && queryFilters[field] ? queryFilters[field].includes(bucket.name) : true,
                });
              }
            });
            orderedBucketNames.sort();
          }
        });

        this.series.values.forEach(value => {
          value.legendIndex = orderedBucketNames.indexOf(value.name);
        });

        const stackedValue = this.parent.widget.chart.stacked;
        let stacking: string | boolean | null = null;
        if (stackedValue != null) {
          if (stackedValue) {
            stacking = 'normal';
          } else {
            stacking = false;
          }
        }

        this.options = {
          labelPrefix: 'HTTP Status',
          pointStart: timestamp.from,
          pointInterval: timestamp.interval,
          stacking,
          plotLines: (this.events || []).map(event => {
            return {
              color: 'rgba(223, 169, 65, 0.4)',
              width: 2,
              value: event.created_at,
              label: {
                useHTML: true,
                text: `
                  <div style="
                        background-color: var(--gv-theme-font-color-dark, #262626);
                        color: white;
                        padding: 5px;
                        border-radius: 2px;
                        z-index: 99;
                        visibility: hidden;">
                    <span>Deployment #${event.properties.deployment_number}</span>
                    <br>
                    <span>${event.properties.deployment_label || ''}</span>
                  </div>`,
                rotation: 0,
                style: {
                  visibility: 'hidden',
                },
              },
            };
          }),
        };
      };

      this.onZoom = event => {
        $rootScope.$broadcast('timeframeZoom', {
          from: Math.floor(event.detail.from),
          to: Math.round(event.detail.to),
        });
      };

      this.onSelect = event => {
        const selected = event.detail.chart.series[event.detail.index];

        if (this.parent.widget.chart.selectable) {
          const query = this.parent.widget.chart.request.query;
          if (query && query.includes(selected.name)) {
            this.updateQuery(selected, false);
          } else {
            this.updateQuery(selected, true);
          }
        } else {
          this.series.values.forEach(serie => {
            if (serie.name === selected.name) {
              serie.visible = !selected.visible;
            }
          });

          this.gvChartLine.setAttribute('series', JSON.stringify(this.series));
        }
      };

      this.onResize = () => {
        this.options = {
          ...this.options,
          height: this.gvChartLine.offsetHeight,
          width: this.gvChartLine.offsetWidth,
        };

        this.gvChartLine.setAttribute('options', JSON.stringify(this.options));
      };

      this.updateQuery = (item, add) => {
        if (this.widget && item.userOptions) {
          this.$scope.$emit('filterItemChange', {
            widget: this.widget.$uid,
            field: this.widget.chart.request.aggs.split(':')[1],
            key: item.userOptions.id,
            name: item.name,
            mode: add ? 'add' : 'remove',
          });
        }
      };
    },
  ],
};

export default WidgetChartLineComponent;
