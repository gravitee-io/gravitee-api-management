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
class ApiMonitoringController {
  constructor (ApiService, resolvedApi, $state, $mdDialog, NotificationService, $scope) {
    'ngInject';
    this.ApiService = ApiService;
    this.$mdDialog = $mdDialog;
    this.NotificationService = NotificationService;
    this.$scope = $scope;
    this.$state = $state;
    this.api = resolvedApi.data;

    this.timeUnits = [ 'SECONDS', 'MINUTES' ];
    this.analytics = this.analytics();

    this.setTimeframe('1h');

    this.initState();
    this.updateChart();
  }

  initState() {
    if (this.api.monitoring != undefined) {
      this.$scope.monitoringEnabled = this.api.monitoring.enabled;
    } else {
      this.$scope.monitoringEnabled = false;
    }
  }

  switchEnabled() {
    if (this.api.monitoring == undefined) {
      this.api.monitoring = {}
    }
    this.api.monitoring.enabled = this.$scope.monitoringEnabled;
    this.update();
  }

  openMenu($mdOpenMenu, ev) {
    $mdOpenMenu(ev);
  }

  updateTimeframe(timeframeId) {
    this.setTimeframe(timeframeId);

    this.updateChart();
  }

  setTimeframe(timeframeId) {
    var timeframe = _.find(this.analytics.timeframes, function(timeframe) {
      return timeframe.id === timeframeId;
    });

    var now = Date.now();

    this.$scope.analytics = {
      timeframe: timeframe,
      range: {
        interval: timeframe.interval,
        from: now - timeframe.range,
        to: now
      }
    };
  }

  update() {
    var _this = this;
    this.ApiService.update(this.api).then((updatedApi) => {
      _this.api = updatedApi.data;
      _this.$scope.formApiMonitoring.$setPristine();
    });
  }

  updateChart() {
    var _this = this;

    this.ApiService.apiHealth(
      _this.api.id,
      _this.$scope.analytics.range.interval,
      _this.$scope.analytics.range.from,
      _this.$scope.analytics.range.to).then(response => {
      this.$scope.chartConfig = {
        type: 'area',
        credits: {
          enabled: false
        },
        title: {
          text: 'Health status'
        },
        subtitle: {
          text: ''
        },
        xAxis: {
          type: 'datetime',
          categories: response.data.timestamps,
          labels: {
            formatter: function () {
              return moment(this.value).format("YYYY-MM-DD HH:mm:ss");
            }
          },
          tickmarkPlacement: 'on',
          title: {
            enabled: false
          }
        },
        yAxis: {title: {text: 'Health'}},
        tooltip: {
          pointFormat: '<span style="color:{series.color}">{series.name}</span>: <b>{point.percentage:.1f}%</b><br/>',
          shared: true
        },
        plotOptions: {
          area: {
            stacking: 'percent',
            lineColor: '#ffffff',
            lineWidth: 1,
            marker: {
              lineWidth: 1,
              lineColor: '#ffffff'
            }
          }
        },
        series: []
      };

      // Push data for hits by status
      for (var property in response.data.buckets) {
        this.$scope.chartConfig.series.push({
          type: 'area',
          name: property,
          data: response.data.buckets[property]
        });
      }
    });
  }

  analytics() {
    return {
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
    };
  }
}

export default ApiMonitoringController;
