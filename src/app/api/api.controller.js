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
class ApiController {
  constructor (ApiService, $stateParams, $mdDialog, NotificationService, $scope) {
    'ngInject';
    this.ApiService = ApiService;
    this.$mdDialog = $mdDialog;
    this.NotificationService = NotificationService;
    this.$scope = $scope;

    this.apis = [];
    if ($stateParams.apiName) {
      this.get($stateParams.apiName);
      this.listPolicies($stateParams.apiName);
    } else {
      this.list();
    }

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
  }

  get(apiName) {
    this.ApiService.get(apiName).then(response => {
      this.api = response.data;
    });
  }

  list() {
    this.ApiService.list().then(response => {
      this.apis = response.data;
    });
  }

  start(name) {
    this.ApiService.start(name).then(() => {
      this.list();
    });
  }

  stop(name) {
    this.ApiService.stop(name).then(() => {
      this.list();
    });
  }

  delete(name) {
    this.ApiService.delete(name).then(() => {
      this.list();
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

  showApiModal(api) {
    var that = this;
    this.$mdDialog.show({
      controller: DialogApiController,
      templateUrl: 'app/api/api.dialog.html',
      parent: angular.element(document.body),
      api: api
    }).then(function (api) {
      if (api) {
        that.list();
      }
    });
  }

  update(api) {
    this.ApiService.update(api).then(() => {
      this.$scope.formApi.$setPristine();
      this.NotificationService.show('Api updated with success');
    });
  }

  bgColorByIndex(index) {
    switch (index % 6) {
      case 0 :
        return '#f39c12';
      case 1 :
        return '#29b6f6';
      case 2 :
        return '#26c6da';
      case 3 :
        return '#26a69a';
      case 4 :
        return '#259b24';
      case 5 :
        return '#26a69a';
      default :
        return 'black';
    }
  }

  // documentation
}

function DialogApiController($scope, $mdDialog, ApiService, api) {
  'ngInject';

  $scope.api = api;
  $scope.editMode = api;

  $scope.hide = function () {
    $mdDialog.hide();
  };

  $scope.save = function () {
    if ($scope.editMode) {
      ApiService.update($scope.api).then(function () {
        $mdDialog.hide(api);
      }).catch(function (error) {
        $scope.error = error;
      });
    } else {
      ApiService.create($scope.api).then(function () {
        $mdDialog.hide(api);
      }).catch(function (error) {
        $scope.error = error;
      });
    }
  };
}

export default ApiController;
