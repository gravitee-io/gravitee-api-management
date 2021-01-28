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
import AnalyticsService from '../../../services/analytics.service';
import EventsService from '../../../services/events.service';
// tslint:disable-next-line:no-var-requires
require('@gravitee/ui-components/wc/gv-chart-line');

const WidgetChartLineComponent: ng.IComponentOptions = {
  template: require('./widget-chart-line.html'),
  bindings: {
    data: '<'
  },
  require: {
    parent: '^gvWidget'
  },
  controller: function($scope, $element, $rootScope, $state, AnalyticsService: AnalyticsService, EventsService: EventsService) {
    'ngInject';

    this.AnalyticsService = AnalyticsService;
    this.EventsService = EventsService;
    this.$scope = $scope;

    this.$onInit = () => {
      this.widget = this.parent.widget;
    };

    this.$onChanges = (changes) => {
      if (changes.data) {
        let data = changes.data.currentValue;
        this.series = {values: []};
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

    this.prepareData = (data) => {
      let timestamp = data.timestamp;
      this.events = data.events && data.events.content;

      data.values.forEach( (value, idx) => {
        let label = this.parent.widget.chart.labels ? this.parent.widget.chart.labels[idx] : '';

        if (value.buckets.length === 0) {
          let bucketCount = (data.timestamp.to - data.timestamp.from) / data.timestamp.interval;

          this.series.values.push({
            name: label,
            data: Array(bucketCount).fill(0),
            labelPrefix: label,
            id: label,
            visible: true
          });
        } else {
          value.buckets.forEach((bucket) => {
            if (bucket) {
              let isFieldRequest = this.parent.widget.chart.request.aggs.split('%3B')[idx].includes('field:');
              let query = this.parent.widget.chart.request.query;
              if (bucket.name === '1' || bucket.name.match('^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$')) {
                isFieldRequest = false;
              }
              if (!label || (value.metadata && value.metadata[bucket.name])) {
                label = value.metadata[bucket.name].name;
              }

              this.series.values.push({
                name: isFieldRequest ? bucket.name : label,
                data: bucket.data,
                labelPrefix: isFieldRequest ? label : '',
                id: bucket.name,
                visible: this.parent.widget.chart.selectable && query ? query.includes(bucket.name) : true
              });
            }
          });
        }
      });

      this.options = {
        labelPrefix: 'HTTP Status',
        pointStart: timestamp.from,
        pointInterval: timestamp.interval,
        stacking: this.parent.widget.chart.stacked ? 'normal' : null,
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
                }
              }
            };
          })
      };
    };

    this.onZoom = (event) => {
      $rootScope.$broadcast('timeframeZoom', {
        from: Math.floor(event.detail.from),
        to: Math.round(event.detail.to)
      });
    };

    this.onSelect = (event) => {
      let selected = event.detail.chart.series[event.detail.index];

      if (this.parent.widget.chart.selectable) {
        let query = this.parent.widget.chart.request.query;
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
          mode: (add) ? 'add' : 'remove',
        });
      }
    };
  }
};

export default WidgetChartLineComponent;
