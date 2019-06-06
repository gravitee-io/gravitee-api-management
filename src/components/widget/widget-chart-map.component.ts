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
import * as Highcharts from "highcharts";

const WidgetChartMapComponent: ng.IComponentOptions = {
  template: require('./widget-chart-map.html'),
  bindings: {
    data: '<'
  },
  require: {
    parent: '^gvWidget'
  },
  controller: function() {
    this.$onChanges = function(changes) {
      if (changes.data) {
        let data = _.map(changes.data.currentValue.values, (v, k) => {
          return {key: k, value: v}
        });

        this.results = {
          title: {
            text: null
          },

          mapNavigation: {
            enabled: true,
            enableMouseWheelZoom: false
          },

          legend: {
            layout: 'vertical',
            align: 'left',
            verticalAlign: 'bottom'
          },

          colorAxis: {
            min: 0,
            stops: [
              [0, '#dfe7fb'],
              [0.5, Highcharts.getOptions().colors[0]],
              [1, Highcharts.Color(Highcharts.getOptions().colors[0]).brighten(-0.5).get()]
            ]
          },

          tooltip: {
            headerFormat: '',
            pointFormat: '<b>{point.name}:</b> {point.value} hits'
          },

          series: [{
            data: data,
            mapData: Highcharts.maps['custom/world'],
            joinBy: ['hc-a2', 'key'],
            name: 'Number of API requests',
            states: {
              hover: {
                color: Highcharts.getOptions().colors[2]
              }
            },
            states: {
              hover: {
                color: '#a4edba',
                borderColor: 'gray'
              }
            },
            nullColor: '#eaecfd'
          }]
        };
      }
    };
  }
};

export default WidgetChartMapComponent;
