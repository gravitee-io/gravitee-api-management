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
class ApiHealthCheckController {
  constructor (ApiService, resolvedApi, $state, $mdSidenav, $mdDialog, NotificationService, $scope, $rootScope) {
    'ngInject';
    this.ApiService = ApiService;
    this.$mdDialog = $mdDialog;
    this.$mdSidenav = $mdSidenav;
    this.NotificationService = NotificationService;
    this.$scope = $scope;
    this.$state = $state;
    this.$rootScope = $rootScope;
    this.api = resolvedApi.data;

    this.healthcheck = this.api.services && this.api.services['health-check'];

    this.timeUnits = [ 'SECONDS', 'MINUTES', 'HOURS' ];
    this.httpMethods = [ 'GET', 'POST', 'PUT' ];

    this.hasData = false;

    this.analytics = this.analytics();

    this.setTimeframe('1h');

    this.initState();
    this.updateChart();
  }

  initState() {
    if (this.healthcheck !== undefined) {
      this.$scope.healthCheckEnabled = this.healthcheck.enabled;
    } else {
      this.$scope.healthCheckEnabled = false;
    }

    if (this.healthcheck === undefined) {
      this.healthcheck = {};
    }
    if (this.healthcheck.request === undefined) {
      this.healthcheck.request = {};
    }
    if (this.healthcheck.expectation === undefined) {
      this.healthcheck.expectation = {};
    }
    if (this.healthcheck.request.headers === undefined) {
      this.healthcheck.request.headers = [
        {name: '', value: ''}
      ];
    }
    if (this.healthcheck.expectation.assertions === undefined) {
      this.healthcheck.expectation.assertions = [];
    }
  }

  switchEnabled() {
    if (this.healthcheck === undefined) {
      this.healthcheck = {};
    }
    this.healthcheck.enabled = this.$scope.healthCheckEnabled;
    this.update();
  }

  toggleRight() {
    this.$mdSidenav('healthcheck-config').toggle();
  }

  close() {
    this.$mdSidenav('healthcheck-config').close();
  };

  openMenu($mdOpenMenu, ev) {
    $mdOpenMenu(ev);
  }

  addHTTPHeader() {
    if (this.healthcheck.request.headers === undefined) {
      this.healthcheck.request.headers = [];
    }

    this.healthcheck.request.headers.push({name: '', value: ''});
  }

  removeHTTPHeader(idx) {
    if (this.healthcheck.request.headers !== undefined) {
      this.healthcheck.request.headers.splice(idx, 1);
    }
  }

  addAssertion() {
    if (this.healthcheck.expectation === undefined) {
      this.healthcheck.expectation = {
        assertions: ['']
      }
    } else {
      this.healthcheck.expectation.assertions.push('');
    }
  }

  removeAssertion(idx) {
    if (this.healthcheck.expectation !== undefined) {
      this.healthcheck.expectation.assertions.splice(idx, 1);
    }
  }

  updateTimeframe(timeframeId) {
    this.setTimeframe(timeframeId);

    this.updateChart();
  }

  showAssertionInformation() {
    this.$mdDialog.show({
        controller: 'DialogAssertionInformationController',
        controllerAs: 'ctrl',
        templateUrl: 'app/api/admin/healthcheck/assertion.dialog.html',
        parent: angular.element(document.body),
        clickOutsideToClose:true
      });
  }

  setTimeframe(timeframeId) {
    var timeframe = _.find(this.analytics.timeframes, function(timeframe) {
      return timeframe.id === timeframeId;
    });
    var now = moment();

    this.$scope.analytics = {
      timeframe: timeframe,
      range: {
        interval: timeframe.interval,
        from: now - timeframe.range,
        to: now,
        fromMoment: moment(now - timeframe.range).format("dddd, MMMM Do YYYY, h:mm:ss a"),
        toMoment: moment(now).format("dddd, MMMM Do YYYY, h:mm:ss a")
      }
    };
  }

  update() {
    var _this = this;
    this.api.services['health-check'] = this.healthcheck;
    this.ApiService.update(this.api).then((updatedApi) => {
      _this.api = updatedApi.data;
      _this.close();
      _this.$scope.formApiHealthCheck.$setPristine();
      _this.$rootScope.$broadcast('apiChangeSuccess');
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
          text: 'Status'
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
        yAxis: {title: {text: '% Health'}},
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

      this.hasData = !(_.isEmpty(response.data.buckets));

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
          interval: 10000
        }, {
          id: '1h',
          title: 'Last hour',
          range: 1000 * 60 * 60,
          interval: 1000 * 60
        }, {
          id: '24h',
          title: 'Last 24 hours',
          range: 1000 * 60 * 60 * 24,
          interval: 1000 * 60 * 60
        }, {
          id: '3d',
          title: 'Last 3 days',
          range: 1000 * 60 * 60 * 24 * 3,
          interval: 1000 * 60 * 60 * 3
        }, {
          id: '14d',
          title: 'Last 14 days',
          range: 1000 * 60 * 60 * 24 * 14,
          interval: 1000 * 60 * 60 * 5
        }, {
          id: '30d',
          title: 'Last 30 days',
          range: 1000 * 60 * 60 * 24 * 30,
          interval: 10000000
        }, {
          id: '90d',
          title: 'Last 90 days',
          range: 1000 * 60 * 60 * 24 * 90,
          interval: 10000000
        }
      ]
    };
  }
}

export default ApiHealthCheckController;
