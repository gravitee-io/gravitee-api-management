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

const WidgetChartPieComponent: ng.IComponentOptions = {
  template: require('./widget-chart-pie.html'),
  bindings: {
    data: '<'
  },
  require: {
    parent: '^gvWidget'
  },
  controller: function() {
    this.$onChanges = function(changes) {
      if (changes.data) {
        let data = changes.data.currentValue;
        let values = [];

        let total = _.reduce(data.values, function(sum: number, val: number) {
          return sum + val;
        }, 0);

        let idx = 0;
        let that = this;
        _.forEach(data.values, function(value: number) {
          let percentage = _.round(value / total * 100, 2);
          values.push({
            name: that.parent.widget.chart.labels[idx],
            y: percentage,
            color: that.parent.widget.chart.colors[idx],
            hits: value
          });
          idx++;
        });

        this.results = {
          chart: {
            plotBackgroundColor: null,
            plotBorderWidth: 0,
            plotShadow: false
          },
          plotOptions: {
            pie: {
              dataLabels: {
                enabled: true,
                distance: -50,
                style: {
                  fontWeight: 'bold',
                  color: 'white'
                }
              },
              startAngle: -90,
              endAngle: 90,
              center: ['50%', '75%']
            }
          },
          tooltip: {
            pointFormat: '{series.name}: <b>{point.percentage:.2f}%</b>'
                       + '<br>'
                       + 'Nb hits: <b>{point.hits}</b>'
          },
          title: {
            text: '<br>' + total +'<br> hits',
            align: 'center',
            verticalAlign: 'middle',
            y: 40
          },
          series: [{
            name: 'Percent hits',
            innerSize: '50%',
            data: values
          }]
        };
      }
    };
  }
};

export default WidgetChartPieComponent;
