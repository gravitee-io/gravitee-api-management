/**
 * tc-angular-chartjs - v2.0.0 - 2016-05-28
 * Copyright (c) 2016 Carl Craig <carlcraig.threeceestudios@gmail.com>
 * Dual licensed with the Apache-2.0 or MIT license.
 */
(function() {
    "use strict";
    angular.module("tc.chartjs", []).directive("tcChartjs", TcChartjs).directive("tcChartjsLine", TcChartjsLine).directive("tcChartjsBar", TcChartjsBar).directive("tcChartjsRadar", TcChartjsRadar).directive("tcChartjsPolararea", TcChartjsPolararea).directive("tcChartjsPie", TcChartjsPie).directive("tcChartjsDoughnut", TcChartjsDoughnut).directive("tcChartjsLegend", TcChartjsLegend).factory("TcChartjsFactory", TcChartjsFactory);
    function TcChartjs(TcChartjsFactory) {
        return new TcChartjsFactory();
    }
    TcChartjs.$inject = [ "TcChartjsFactory" ];
    function TcChartjsLine(TcChartjsFactory) {
        return new TcChartjsFactory("line");
    }
    TcChartjsLine.$inject = [ "TcChartjsFactory" ];
    function TcChartjsBar(TcChartjsFactory) {
        return new TcChartjsFactory("bar");
    }
    TcChartjsBar.$inject = [ "TcChartjsFactory" ];
    function TcChartjsRadar(TcChartjsFactory) {
        return new TcChartjsFactory("radar");
    }
    TcChartjsRadar.$inject = [ "TcChartjsFactory" ];
    function TcChartjsPolararea(TcChartjsFactory) {
        return new TcChartjsFactory("polararea");
    }
    TcChartjsPolararea.$inject = [ "TcChartjsFactory" ];
    function TcChartjsPie(TcChartjsFactory) {
        return new TcChartjsFactory("pie");
    }
    TcChartjsPie.$inject = [ "TcChartjsFactory" ];
    function TcChartjsDoughnut(TcChartjsFactory) {
        return new TcChartjsFactory("doughnut");
    }
    TcChartjsDoughnut.$inject = [ "TcChartjsFactory" ];
    function TcChartjsFactory() {
        return function(chartType) {
            return {
                restrict: "A",
                scope: {
                    data: "=chartData",
                    options: "=chartOptions",
                    type: "@chartType",
                    legend: "=?chartLegend",
                    chart: "=?chart",
                    click: "&chartClick"
                },
                link: link
            };
            function link($scope, $elem, $attrs) {
                var ctx = $elem[0].getContext("2d");
                var chartObj;
                var showLegend = false;
                var autoLegend = false;
                var exposeChart = false;
                var legendElem = null;
                for (var attr in $attrs) {
                    if (attr === "chartLegend") {
                        showLegend = true;
                    } else if (attr === "chart") {
                        exposeChart = true;
                    } else if (attr === "autoLegend") {
                        autoLegend = true;
                    }
                }
                $scope.$on("$destroy", function() {
                    if (chartObj && typeof chartObj.destroy === "function") {
                        chartObj.destroy();
                    }
                });
                if ($scope.click) {
                    $elem[0].onclick = function(evt) {
                        var out = {
                            chartEvent: event,
                            element: chartObj.getElementAtEvent(evt),
                            elements: chartObj.getElementsAtEvent(evt),
                            dataset: chartObj.getDatasetAtEvent(evt)
                        };
                        $scope.click({
                            event: out
                        });
                    };
                }
                $scope.$watch("data", function(value) {
                    if (value) {
                        if (chartObj && typeof chartObj.destroy === "function") {
                            chartObj.destroy();
                        }
                        var type = chartType || $scope.type;
                        if (!type) {
                            throw "Error creating chart: Chart type required.";
                        }
                        type = cleanChartName(type);
                        chartObj = new Chart(ctx, {
                            type: type,
                            data: angular.copy($scope.data),
                            options: $scope.options
                        });
                        if (showLegend) {
                            $scope.legend = chartObj.generateLegend();
                        }
                        if (autoLegend) {
                            if (legendElem) {
                                legendElem.remove();
                            }
                            angular.element($elem[0]).after(chartObj.generateLegend());
                            legendElem = angular.element($elem[0]).next();
                        }
                        if (exposeChart) {
                            $scope.chart = chartObj;
                        }
                        chartObj.resize();
                    }
                }, true);
            }
            function cleanChartName(type) {
                var typeLowerCase = type.toLowerCase();
                switch (typeLowerCase) {
                  case "polararea":
                    return "polarArea";

                  default:
                    return type;
                }
            }
        };
    }
    function TcChartjsLegend() {
        return {
            restrict: "A",
            scope: {
                legend: "=?chartLegend"
            },
            link: link
        };
        function link($scope, $elem) {
            $scope.$watch("legend", function(value) {
                if (value) {
                    $elem.html(value);
                }
            }, true);
        }
    }
})();