"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
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
var _ = require("lodash");
var WidgetChartPieComponent = {
    template: require('./widget-chart-pie.html'),
    bindings: {
        data: '<'
    },
    require: {
        parent: '^gvWidget'
    },
    controller: function () {
        this.$onChanges = function (changes) {
            if (changes.data) {
                var data = changes.data.currentValue;
                var values_1 = [];
                var total_1 = _.reduce(data.values, function (sum, val) {
                    return sum + val;
                }, 0);
                var idx_1 = 0;
                var that_1 = this;
                _.forEach(data.values, function (value) {
                    var percentage = _.round(value / total_1 * 100);
                    values_1.push({
                        name: that_1.parent.widget.chart.labels[idx_1],
                        y: percentage,
                        color: that_1.parent.widget.chart.colors[idx_1]
                    });
                    idx_1++;
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
                        pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>'
                    },
                    title: {
                        text: '<br>' + total_1 + '<br> hits',
                        align: 'center',
                        verticalAlign: 'middle',
                        y: 40
                    },
                    series: [{
                            name: 'Percent hits',
                            innerSize: '50%',
                            data: values_1
                        }]
                };
            }
        };
    }
};
exports.default = WidgetChartPieComponent;
