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
/* global document:false */
class ApiAdminController {
  constructor (ApiService, $state, $mdDialog, NotificationService, $scope) {
    'ngInject';
    this.ApiService = ApiService;
    this.$mdDialog = $mdDialog;
    this.NotificationService = NotificationService;
    this.$scope = $scope;
    this.$state = $state;

    this.apis = [];
    this.get($state.params.apiId);

    this.selectedPolicy = null;

    $scope.chartConfig = {
      xAxis: {
        categories: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
          'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
      },
      title: {
        text: 'USD to EUR exchange rate from 2006 through 2008'
      },
      subtitle: {
        text: document.ontouchstart === undefined ?
          'Click and drag in the plot area to zoom in' :
          'Pinch the chart to zoom in'
      },
      yAxis: { title: { text: 'Temperature (Celsius)' } },
      tooltip: { valueSuffix: ' celsius' },
      legend: { align: 'center', verticalAlign: 'bottom', borderWidth: 0 },
      plotOptions: {
        area: {
          fillColor: {
            linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1},
            stops: [
              [0, Highcharts.getOptions().colors[0]],
              [1, Highcharts.Color(Highcharts.getOptions().colors[0]).setOpacity(0).get('rgba')]
            ]
          },
          marker: {
            radius: 2
          },
          lineWidth: 1,
          states: {
            hover: {
              lineWidth: 1
            }
          },
          threshold: null
        }
      },
      series: [{
        type:'area',
        data: [50.0, 6.9, 9.5, 14.5, 18.2, 21.5, 25.2, 26.5, 23.3, 18.3, 13.9, 9.6]
      }]
    };

    if ($state.current.name.endsWith('dashboard')) {
      $scope.selectedTab = 0;
    } else if ($state.current.name.endsWith('general')) {
      $scope.selectedTab = 1;
    } else if ($state.current.name.endsWith('policies')) {
      $scope.selectedTab = 2;
    } else if ($state.current.name.endsWith('documentation')) {
      $scope.selectedTab = 3;
    }

    var that = this;
    $scope.$on('$stateChangeSuccess', function (ev, to, toParams, from) {
      if (from.name.startsWith('apis.list.')) {
        that.previousState = from.name;
      }
    });
  }

  get(apiId) {
    this.ApiService.get(apiId).then(response => {
      this.api = response.data;
      this.initState();
    });
  }

  initState() {
    this.$scope.enabled = this.api.state === 'started'? true : false;
  }

  changeLifecycle(id) {
    var started = this.api.state === 'started';
    var alert = this.$mdDialog.confirm({
      title: 'Warning',
      content: 'Are you sure you want to ' + (started?'':'un') +'publish the api ' + id + '?',
      ok: 'OK',
      cancel: 'Cancel'
    });

    var that = this;

    this.$mdDialog
      .show(alert)
      .then(function () {
        if (started) {
          that.ApiService.stop(id);
        } else {
          that.ApiService.start(id);
        }
      })
      .catch(function () {
        that.initState();
      });
  }

  delete(name) {
    this.ApiService.delete(name).then(() => {
      this.backToPreviousState();
    });
  }

  listPolicies(apiName) {
    this.ApiService.listPolicies(apiName).then(response => {
      // TODO filter request, response and request/response policies
      this.policies = {
        'OnRequest': response.data,
        'OnResponse': [],
        'OnRequest/OnResponse': []
      };
    });
  }

  backToPreviousState() {
    if (!this.previousState) {
      this.previousState = 'apis.list.thumb';
    }
    this.$state.go(this.previousState);
  }
}

export default ApiAdminController;
