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
var WidgetChartLineComponent = {
    template: require('./widget-chart-line.html'),
    bindings: {
        data: '<'
    },
    require: {
        parent: '^gvWidget'
    },
    controller: function ($scope, $rootScope) {
        'ngInject';
        this.$scope = $scope;
        //from https://material.google.com/style/color.html#color-color-palette
        //shade 500 & 900
        //
        // deep purple, lime, deep orange, pink, purple,
        // light green, amber, Blue Grey, orange, teal,
        // indigo, purple, red, cyan, brown
        this.colorByBucket = [
            '#673ab7', '#cddc39', '#ff5722', '#e91e63', '#9c27b0',
            '#8bc34a', '#ffc107', '#607d8b', '#ff9800', '#009688',
            '#3f51b5', '#9c27b0', '#f44336', '#00bcd4', '#795548',
            '#311b92', '#827717', '#bf360c', '#880e4f', '#4a148c',
            '#33691e', '#ff6f00', '#263238', '#e65100', '#004d40',
            '#1a237e', '#4a148c', '#b71c1c', '#006064', '#3e2723'
        ];
        //from https://material.google.com/style/color.html#color-color-palette
        //shade 200 & 300
        this.bgColorByBucket = [
            '#b39ddb', '#e6ee9c', '#ffab91', '#f48fb1', '#ce93d8',
            '#c5e1a5', '#ffe082', '#b0bec5', '#ffcc80', '#80cbc4',
            '#9fa8da', '#ce93d8', '#ef9a9a', '#80deea', '#bcaaa4',
            '#9575cd', '#dce775', '#ff8a65', '#f06292', '#ba68c8',
            '#aed581', '#ffd54f', '#90a4ae', '#ffb74d', '#4db6ac',
            '#7986cb', '#ba68c8', '#e57373', '#4dd0e1', '#a1887f'
        ];
        var that = this;
        this.$onChanges = function (changes) {
            if (changes.data) {
                var data = changes.data.currentValue;
                var values_1 = [], i_1;
                // Prepare chart
                this.result = {
                    title: { text: this.parent.widget.chart.title },
                    xAxis: {
                        type: 'datetime',
                        dateTimeLabelFormats: {
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
                                i_1++;
                                var lineColor = that.colorByBucket[i_1 % that.colorByBucket.length];
                                var bgColor = that.bgColorByBucket[i_1 % that.bgColorByBucket.length];
                                var label = that.parent.widget.chart.labels ? that.parent.widget.chart.labels[idx] : (bucket.name);
                                if (value.metadata && value.metadata[bucket.name]) {
                                    label = value.metadata[bucket.name].name;
                                }
                                values_1.push({
                                    name: label || bucket.name, data: bucket.data, color: lineColor, fillColor: bgColor,
                                    labelPrefix: that.parent.widget.chart.labelPrefix
                                });
                            }
                        });
                    });
                    var timestamp = data.timestamp;
                    that.result = _.assign(that.result, {
                        series: values_1,
                        plotOptions: {
                            series: {
                                pointStart: timestamp.from,
                                pointInterval: timestamp.interval
                            }
                        }
                    });
                }
                else {
                    that.result.series = [];
                }
            }
        };
    }
};
exports.default = WidgetChartLineComponent;
