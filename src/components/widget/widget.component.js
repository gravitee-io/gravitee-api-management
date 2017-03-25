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
var WidgetComponent = {
    template: require('./widget.html'),
    bindings: {
        widget: '<'
    },
    controller: function ($scope) {
        'ngInject';
        $scope.$on('gridster-resized', function () {
            $scope.$broadcast('onWidgetResize');
        });
        var that = this;
        $scope.$on('onTimeframeChange', function (event, timeframe) {
            // Associate the new timeframe to the chart request
            _.assignIn(that.widget.chart.request, {
                interval: timeframe.interval,
                from: timeframe.from,
                to: timeframe.to
            });
            that.reload();
        });
        $scope.$on('onQueryFilterChange', function (event, query) {
            // Associate the new query filter to the chart request
            _.assignIn(that.widget.chart.request, {
                query: query
            });
            that.reload();
        });
        this.reload = function () {
            var _this = this;
            // Call the analytics service
            this.fetchData = true;
            var chart = this.widget.chart;
            // Prepare arguments
            var args = [this.widget.root, chart.request];
            if (!this.widget.root) {
                args.splice(0, 1);
            }
            chart.service.function
                .apply(chart.service.caller, args)
                .then(function (response) {
                _this.fetchData = false;
                _this.results = response.data;
            });
        };
    }
};
exports.default = WidgetComponent;
