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
class ApiAnalyticsController {
  constructor (ApiService, resolvedApi, $q, $state, $mdDialog, NotificationService, $scope) {
    'ngInject';
    this.ApiService = ApiService;
    this.$mdDialog = $mdDialog;
    this.NotificationService = NotificationService;
    this.$scope = $scope;
    this.$state = $state;
    this.api = resolvedApi.data;
    this.$q = $q;

    this.analytics = this.analytics();

    this.setTimeframe('3d');
    this.changeReport(this.analytics.reports[0].id);
  }

  openMenu($mdOpenMenu, ev) {
    $mdOpenMenu(ev);
  };

  changeReport(reportId) {
    var report = _.find(this.analytics.reports, function(report) {
      return report.id == reportId;
    });

    this.$scope.analytics.report = report;

    this.updateChart();
  };

  updateTimeframe(timeframeId) {
    this.setTimeframe(timeframeId);

    this.updateChart();
  }

  setTimeframe(timeframeId) {
    var timeframe = _.find(this.analytics.timeframes, function(timeframe) {
      return timeframe.id == timeframeId;
    });

    var now = Date.now();

    var oldReport = (this.$scope.analytics == undefined) ? undefined : this.$scope.analytics.report;

    this.$scope.analytics = {
      timeframe: timeframe,
      report: oldReport,
      range: {
        interval: timeframe.interval,
        from: now - timeframe.range,
        to: now
      }
    };
  }

  updateChart() {
    var _this = this;

    var requests = _.map(this.$scope.analytics.report.requests, function(req) {
      return req.call(_this.ApiService, _this.api.id,
        _this.$scope.analytics.range.interval,
        _this.$scope.analytics.range.from,
        _this.$scope.analytics.range.to);
    });

    this.$q.all(requests).then(response => {
      this.$scope.chartConfig = {
        credits: {
          enabled: false
        },
        title: {
          text: ''
        },
        xAxis: {
          type: 'datetime',
          categories: response[0].data.timestamps,
          labels: {
            formatter: function() {
              return moment(this.value).format("YYYY-MM-DD HH:mm:ss");
            }
          }
        },
        yAxis: { title: { text: 'API Calls' } },
        plotOptions: {
          area: {
            marker: {
              enabled: false,
              symbol: 'circle',
              radius: 2,
              states: {
                hover: {
                  enabled: true
                }
              }
            }
          },
          column: {
            stacking: 'normal'
          }
        },
        series: []
      };

      // Push data for global hits
      this.$scope.chartConfig.series.push({
        name: this.api.name,
        type:'area',
        showInLegend: false,
        tooltip: {
          pointFormat: '{series.name}: <b>{point.y}</b> calls'
        },
        data: response[0].data.values[0].buckets[0].data,
      });

      // Push data for hits by status
      for (var i = 0; i < response[1].data.values[0].buckets.length; i++) {
        this.$scope.chartConfig.series.push({
          type: 'column',
          tooltip: {
            pointFormat: this.$scope.analytics.report.tooltip
          },
          name: response[1].data.values[0].buckets[i].name,
          data: response[1].data.values[0].buckets[i].data
        });
      }
    });
  };

  analytics() {
    return {
      reports: [
        {
          id: 'response-status',
          title: 'Response Status',
          tooltip: 'HTTP Status <b>[{series.name}]</b>: <b>{point.y}</b> calls',
          requests : [ this.ApiService.apiHits, this.ApiService.apiHitsByStatus]
        }, {
          id: 'response-time',
          title: 'Response Times',
          tooltip: 'Latency <b>[{series.name} ms]</b>: <b>{point.y}</b> calls',
          requests : [ this.ApiService.apiHits, this.ApiService.apiHitsByLatency]
        }, {
          id: 'response-payload-size',
          title: 'Payload Sizes',
          tooltip: 'Content-length <b>[{series.name}]</b>: <b>{point.y}</b> calls',
          requests : [ this.ApiService.apiHits, this.ApiService.apiHitsByPayloadSize]
        }
      ],
      timeframes: [
        {
          id: '5m',
          title: 'Last 5 minutes',
          range: 1000 * 60 * 5,
          interval: 10000,
        }, {
          id: '1h',
          title: 'Last hour',
          range: 1000 * 60 * 60,
          interval: 1000 * 60,
        }, {
          id: '24h',
          title: 'Last 24 hours',
          range: 1000 * 60 * 60 * 24,
          interval: 1000 * 60 * 60,
        }, {
          id: '3d',
          title: 'Last 3 days',
          range: 1000 * 60 * 60 * 24 * 3,
          interval: 1000 * 60 * 60 * 3,
        }, {
          id: '14d',
          title: 'Last 14 days',
          range: 1000 * 60 * 60 * 24 * 14,
          interval: 1000 * 60 * 60 * 5,
        }, {
          id: '30d',
          title: 'Last 30 days',
          range: 1000 * 60 * 60 * 24 * 30,
          interval: 10000000,
        }, {
          id: '90d',
          title: 'Last 90 days',
          range: 1000 * 60 * 60 * 24 * 90,
          interval: 10000000,
        }
      ]
    }
  }
}

export default ApiAnalyticsController;
